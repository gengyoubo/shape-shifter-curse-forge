package net.onixary.shapeShifterCurseForge.client.color;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;
import net.onixary.shapeShifterCurseForge.capability.ModCapabilities;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client-side form color storage (NBT file), global slots, unlocks and clipboard codec. */
public final class FormColorData {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Shared client instance with lazy file load. */
    public static final FormColorData CLIENT = new FormColorData();
    private static boolean loaded;

    public boolean enableDefaultFormColor = true;
    public final Map<ResourceLocation, FormTextureUtils.ColorSetting> formDefaultSetting = new HashMap<>();

    public final Map<String, FormTextureUtils.ColorSetting> customSetting = new HashMap<>();
    public final Map<ResourceLocation, Map<String, FormTextureUtils.ColorSetting>> customSettingByForm = new HashMap<>();

    public static int globalSlotCount = 9;
    public static int localSlotCount = 3;

    public final Map<ResourceLocation, List<String>> formLocalNames = new HashMap<>();
    public final List<String> formGlobalNames = new ArrayList<>();
    public final Map<ResourceLocation, String> formDefaultNames = new HashMap<>();

    public final List<ResourceLocation> unlockedForms = new ArrayList<>();

    public static int v2GlobalSlotCount = 9;
    public final List<String> v2GlobalNames = new ArrayList<>();

    private FormColorData() {
    }

    public static FormColorData client() {
        if (!loaded) {
            loaded = true;
            CLIENT.loadFormConfig();
        }
        return CLIENT;
    }

    public CompoundTag dumpColorSetting(FormTextureUtils.ColorSetting colorSetting) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("primaryColor", colorSetting.primaryColor());
        tag.putInt("accentColor1", colorSetting.accentColor1());
        tag.putInt("accentColor2", colorSetting.accentColor2());
        tag.putInt("eyeColorA", colorSetting.eyeColorA());
        tag.putInt("eyeColorB", colorSetting.eyeColorB());
        tag.putBoolean("primaryGreyReverse", colorSetting.primaryGreyReverse());
        tag.putBoolean("accent1GreyReverse", colorSetting.accent1GreyReverse());
        tag.putBoolean("accent2GreyReverse", colorSetting.accent2GreyReverse());
        return tag;
    }

    public FormTextureUtils.ColorSetting loadColorSetting(CompoundTag tag) {
        return new FormTextureUtils.ColorSetting(tag.getInt("primaryColor"), tag.getInt("accentColor1"),
                tag.getInt("accentColor2"), tag.getInt("eyeColorA"), tag.getInt("eyeColorB"),
                tag.getBoolean("primaryGreyReverse"), tag.getBoolean("accent1GreyReverse"),
                tag.getBoolean("accent2GreyReverse"));
    }

    public CompoundTag saveCompound() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("enableDefaultFormColor", enableDefaultFormColor);
        CompoundTag formDefaultSettingNbt = new CompoundTag();
        for (Map.Entry<ResourceLocation, FormTextureUtils.ColorSetting> entry : formDefaultSetting.entrySet()) {
            formDefaultSettingNbt.put(entry.getKey().toString(), dumpColorSetting(entry.getValue()));
        }
        tag.put("formDefaultSetting", formDefaultSettingNbt);
        CompoundTag customSettingNbt = new CompoundTag();
        for (Map.Entry<String, FormTextureUtils.ColorSetting> entry : customSetting.entrySet()) {
            customSettingNbt.put(entry.getKey(), dumpColorSetting(entry.getValue()));
        }
        tag.put("customSetting", customSettingNbt);
        CompoundTag customSettingByFormNbt = new CompoundTag();
        for (Map.Entry<ResourceLocation, Map<String, FormTextureUtils.ColorSetting>> entry : customSettingByForm.entrySet()) {
            CompoundTag formNbt = new CompoundTag();
            for (Map.Entry<String, FormTextureUtils.ColorSetting> slot : entry.getValue().entrySet()) {
                formNbt.put(slot.getKey(), dumpColorSetting(slot.getValue()));
            }
            customSettingByFormNbt.put(entry.getKey().toString(), formNbt);
        }
        tag.put("customSettingByForm", customSettingByFormNbt);
        CompoundTag localNamesNbt = new CompoundTag();
        for (Map.Entry<ResourceLocation, List<String>> entry : formLocalNames.entrySet()) {
            ListTag list = new ListTag();
            for (String name : entry.getValue()) {
                list.add(StringTag.valueOf(name));
            }
            localNamesNbt.put(entry.getKey().toString(), list);
        }
        tag.put("FCS_form_local_setting_names", localNamesNbt);
        ListTag globalNames = new ListTag();
        for (String name : formGlobalNames) {
            globalNames.add(StringTag.valueOf(name));
        }
        tag.put("FCS_global_setting_names", globalNames);
        CompoundTag defaultNamesNbt = new CompoundTag();
        for (Map.Entry<ResourceLocation, String> entry : formDefaultNames.entrySet()) {
            defaultNamesNbt.putString(entry.getKey().toString(), entry.getValue());
        }
        tag.put("FCS_form_default_setting_names", defaultNamesNbt);
        ListTag v2GlobalNameTags = new ListTag();
        for (String name : v2GlobalNames) {
            v2GlobalNameTags.add(StringTag.valueOf(name));
        }
        tag.put("V2_FCS_global_setting_names", v2GlobalNameTags);
        ListTag unlocked = new ListTag();
        for (ResourceLocation form : unlockedForms) {
            unlocked.add(StringTag.valueOf(form.toString()));
        }
        tag.put("unlockedForms", unlocked);
        return tag;
    }

    public void loadCompound(CompoundTag tag) {
        formDefaultSetting.clear();
        customSetting.clear();
        customSettingByForm.clear();
        formLocalNames.clear();
        formGlobalNames.clear();
        unlockedForms.clear();
        if (tag.contains("enableDefaultFormColor")) {
            enableDefaultFormColor = tag.getBoolean("enableDefaultFormColor");
        }
        if (tag.contains("formDefaultSetting")) {
            CompoundTag section = tag.getCompound("formDefaultSetting");
            for (String form : section.getAllKeys()) {
                try {
                    formDefaultSetting.put(ResourceLocation.tryParse(form), loadColorSetting(section.getCompound(form)));
                } catch (RuntimeException exception) {
                    LOGGER.warn("Failed to load form default color setting for {}", form, exception);
                }
            }
        }
        if (tag.contains("customSetting")) {
            CompoundTag section = tag.getCompound("customSetting");
            for (String name : section.getAllKeys()) {
                try {
                    customSetting.put(name, loadColorSetting(section.getCompound(name)));
                } catch (RuntimeException exception) {
                    LOGGER.warn("Failed to load custom color setting for {}", name, exception);
                }
            }
        }
        if (tag.contains("customSettingByForm")) {
            CompoundTag section = tag.getCompound("customSettingByForm");
            for (String form : section.getAllKeys()) {
                ResourceLocation formId = ResourceLocation.tryParse(form);
                CompoundTag formNbt = section.getCompound(form);
                for (String name : formNbt.getAllKeys()) {
                    try {
                        customSettingByForm.computeIfAbsent(formId, key -> new HashMap<>())
                                .put(name, loadColorSetting(formNbt.getCompound(name)));
                    } catch (RuntimeException exception) {
                        LOGGER.warn("Failed to load custom color setting for {} on form {}", name, form, exception);
                    }
                }
            }
        }
        if (tag.contains("FCS_form_local_setting_names")) {
            CompoundTag section = tag.getCompound("FCS_form_local_setting_names");
            for (String form : section.getAllKeys()) {
                ResourceLocation formId = ResourceLocation.tryParse(form);
                List<String> names = formLocalNames.computeIfAbsent(formId, key -> new ArrayList<>());
                section.getList(form, 8).forEach(element -> names.add(element.getAsString()));
            }
        }
        if (tag.contains("FCS_global_setting_names")) {
            tag.getList("FCS_global_setting_names", 8).forEach(element -> formGlobalNames.add(element.getAsString()));
        }
        if (tag.contains("FCS_form_default_setting_names")) {
            CompoundTag section = tag.getCompound("FCS_form_default_setting_names");
            for (String form : section.getAllKeys()) {
                formDefaultNames.put(ResourceLocation.tryParse(form), section.getString(form));
            }
        }
        if (tag.contains("V2_FCS_global_setting_names")) {
            tag.getList("V2_FCS_global_setting_names", 8).forEach(element -> v2GlobalNames.add(element.getAsString()));
        }
        if (tag.contains("unlockedForms")) {
            tag.getList("unlockedForms", 8).forEach(element -> unlockedForms.add(ResourceLocation.tryParse(element.getAsString())));
        }
    }

    public Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve("shape-shifter-curse-form-color-data.nbt");
    }

    public void writeToConfig() {
        try {
            NbtIo.writeCompressed(saveCompound(), getConfigPath().toFile());
        } catch (IOException exception) {
            LOGGER.error("Failed to write form color data to config file", exception);
        }
    }

    public void loadFormConfig() {
        Path configPath = getConfigPath();
        if (Files.exists(configPath)) {
            try {
                loadCompound(NbtIo.readCompressed(configPath.toFile()));
            } catch (IOException exception) {
                LOGGER.error("Failed to load form color data from config file", exception);
            }
        }
    }

    public boolean isUnlock(ResourceLocation form) {
        return unlockedForms.contains(form);
    }

    public void unlockForm(ResourceLocation form) {
        if (form == null || unlockedForms.contains(form)) {
            return;
        }
        unlockedForms.add(form);
        writeToConfig();
    }

    public void unlockAll() {
        for (ResourceLocation form : FormRegistry.forms().keySet()) {
            if (!unlockedForms.contains(form)) {
                unlockedForms.add(form);
            }
        }
        writeToConfig();
    }

    public static byte[] colorSettingToBytes(FormTextureUtils.ColorSetting colorSetting) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(1);
            dos.writeInt(colorSetting.primaryColor());
            dos.writeInt(colorSetting.accentColor1());
            dos.writeInt(colorSetting.accentColor2());
            dos.writeInt(colorSetting.eyeColorA());
            dos.writeInt(colorSetting.eyeColorB());
            byte bools = 0;
            bools |= (byte) (colorSetting.primaryGreyReverse() ? 1 : 0);
            bools |= (byte) (colorSetting.accent1GreyReverse() ? 2 : 0);
            bools |= (byte) (colorSetting.accent2GreyReverse() ? 4 : 0);
            dos.writeByte(bools);
            dos.flush();
            return baos.toByteArray();
        } catch (Exception exception) {
            return new byte[0];
        }
    }

    @Nullable
    public static FormTextureUtils.ColorSetting colorSettingFromBytes(byte[] bytes) {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (dis.readInt() != 1) {
                return null;
            }
            int primaryColor = dis.readInt();
            int accentColor1 = dis.readInt();
            int accentColor2 = dis.readInt();
            int eyeColorA = dis.readInt();
            int eyeColorB = dis.readInt();
            byte bools = dis.readByte();
            return new FormTextureUtils.ColorSetting(primaryColor, accentColor1, accentColor2,
                    eyeColorA, eyeColorB, (bools & 1) != 0, (bools & 2) != 0, (bools & 4) != 0);
        } catch (Exception exception) {
            return null;
        }
    }

    public static byte[] formHex(String hex) {
        if (hex == null || hex.isEmpty() || hex.length() % 2 != 0) {
            return null;
        }
        int length = hex.length() / 2;
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            try {
                result[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return result;
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (byte value : bytes) {
            builder.append(String.format("%02X", value & 0xFF));
        }
        return builder.toString();
    }

    @Nullable
    public static FormTextureUtils.ColorSetting colorSettingFromString(String data) {
        try {
            if (data.startsWith("b")) {
                return colorSettingFromBytes(Base64.getDecoder().decode(data.substring(1)));
            } else if (data.startsWith("#")) {
                return colorSettingFromBytes(formHex(data.substring(1)));
            }
            return null;
        } catch (Exception exception) {
            return null;
        }
    }

    public static String colorSettingToString(FormTextureUtils.ColorSetting data, boolean useBase64) {
        if (useBase64) {
            return "b" + Base64.getEncoder().encodeToString(colorSettingToBytes(data));
        }
        return "#" + toHex(colorSettingToBytes(data));
    }

    public String v2GetNameGlobalSlot(int index) {
        if (index < v2GlobalNames.size()) {
            return v2GlobalNames.get(index);
        }
        return "";
    }

    public void v2SetNameGlobalSlot(int index, String name) {
        if (index > v2GlobalSlotCount) {
            return;
        }
        while (v2GlobalNames.size() <= index) {
            v2GlobalNames.add("");
        }
        v2GlobalNames.set(index, name);
    }

    public static Component toCopyableText(String text, String copyText) {
        MutableComponent component = Component.literal(text);
        component.setStyle(component.getStyle().withClickEvent(
                new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, copyText)));
        return component;
    }

    public static Component appendCopyableText(Component text, String copyText) {
        MutableComponent component = text.copy();
        component.setStyle(component.getStyle().withClickEvent(
                new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, copyText)));
        return component;
    }

    /** Local player's skin color from the synced capability (ABGR when requested). */
    @Nullable
    public static FormTextureUtils.ColorSetting getPlayerColorSetting(boolean abgr) {
        if (Minecraft.getInstance().player == null) {
            return null;
        }
        var data = Minecraft.getInstance().player.getCapability(ModCapabilities.PLAYER_SKIN).orElse(null);
        if (data == null) {
            return null;
        }
        FormTextureUtils.ColorSetting colorSetting = data.getFormColor();
        if (abgr) {
            return colorSetting;
        }
        return new FormTextureUtils.ColorSetting(
                FormTextureUtils.abgr2Argb(colorSetting.primaryColor()),
                FormTextureUtils.abgr2Argb(colorSetting.accentColor1()),
                FormTextureUtils.abgr2Argb(colorSetting.accentColor2()),
                FormTextureUtils.abgr2Argb(colorSetting.eyeColorA()),
                FormTextureUtils.abgr2Argb(colorSetting.eyeColorB()),
                colorSetting.primaryGreyReverse(),
                colorSetting.accent1GreyReverse(),
                colorSetting.accent2GreyReverse());
    }

    public static FormTextureUtils.ColorSetting argb2Abgr(FormTextureUtils.ColorSetting colorSetting) {
        return new FormTextureUtils.ColorSetting(
                FormTextureUtils.argb2Abgr(colorSetting.primaryColor()),
                FormTextureUtils.argb2Abgr(colorSetting.accentColor1()),
                FormTextureUtils.argb2Abgr(colorSetting.accentColor2()),
                FormTextureUtils.argb2Abgr(colorSetting.eyeColorA()),
                FormTextureUtils.argb2Abgr(colorSetting.eyeColorB()),
                colorSetting.primaryGreyReverse(),
                colorSetting.accent1GreyReverse(),
                colorSetting.accent2GreyReverse());
    }

    public static FormTextureUtils.ColorSetting abgr2Argb(FormTextureUtils.ColorSetting colorSetting) {
        return new FormTextureUtils.ColorSetting(
                FormTextureUtils.abgr2Argb(colorSetting.primaryColor()),
                FormTextureUtils.abgr2Argb(colorSetting.accentColor1()),
                FormTextureUtils.abgr2Argb(colorSetting.accentColor2()),
                FormTextureUtils.abgr2Argb(colorSetting.eyeColorA()),
                FormTextureUtils.abgr2Argb(colorSetting.eyeColorB()),
                colorSetting.primaryGreyReverse(),
                colorSetting.accent1GreyReverse(),
                colorSetting.accent2GreyReverse());
    }
}
