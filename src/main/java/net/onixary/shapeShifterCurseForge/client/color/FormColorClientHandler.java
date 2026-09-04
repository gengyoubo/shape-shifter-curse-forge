package net.onixary.shapeShifterCurseForge.client.color;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;
import net.onixary.shapeShifterCurseForge.config.SscClientConfig;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.network.ModifyFcdPacket;
import net.onixary.shapeShifterCurseForge.network.UpdateSkinPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Client-side apply for server-driven color slot commands. Slot maps store ARGB. */
public final class FormColorClientHandler {
    private FormColorClientHandler() {
    }

    public static void applyModifyFcd(ModifyFcdPacket packet) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        String command = packet.commandType();
        if (!"list".equals(command) && !SscClientConfig.CUSTOM_ENABLE_SERVER_MODIFY_FCD.get()) {
            return;
        }
        switch (command) {
            case "save" -> saveSlot(packet.arg1(), packet.arg2(), packet.formId());
            case "load" -> loadSlot(packet.arg1(), packet.arg2(), packet.formId());
            case "delete" -> deleteSlot(packet.arg1(), packet.arg2(), packet.formId());
            case "config" -> setEnableDefault(Boolean.parseBoolean(packet.arg1()));
            case "list" -> listSlots(packet.arg1(), packet.formId());
            default -> {
            }
        }
    }

    private static void saveSlot(String scope, String slot, String formId) {
        FormTextureUtils.ColorSetting current = FormColorData.getPlayerColorSetting(false);
        if (current == null || slot.isEmpty()) {
            return;
        }
        FormColorData data = FormColorData.client();
        switch (scope) {
            case "form" -> {
                ResourceLocation form = ResourceLocation.tryParse(formId);
                if (form != null) {
                    data.customSettingByForm.computeIfAbsent(form, key -> new java.util.HashMap<>()).put(slot, current);
                }
            }
            case "global" -> data.customSetting.put(slot, current);
            case "form_default" -> {
                ResourceLocation form = ResourceLocation.tryParse(formId);
                if (form != null) {
                    data.formDefaultSetting.put(form, current);
                }
            }
            default -> {
                return;
            }
        }
        data.writeToConfig();
    }

    private static void loadSlot(String scope, String slot, String formId) {
        FormColorData data = FormColorData.client();
        FormTextureUtils.ColorSetting stored = null;
        switch (scope) {
            case "form" -> {
                ResourceLocation form = ResourceLocation.tryParse(formId);
                Map<String, FormTextureUtils.ColorSetting> byForm =
                        form == null ? null : data.customSettingByForm.get(form);
                if (byForm != null) {
                    stored = byForm.get(slot);
                }
            }
            case "global" -> stored = data.customSetting.get(slot);
            case "form_default" -> {
                ResourceLocation form = ResourceLocation.tryParse(formId);
                if (form != null) {
                    stored = data.formDefaultSetting.get(form);
                }
            }
            default -> {
                return;
            }
        }
        if (stored == null) {
            return;
        }
        FormTextureUtils.ColorSetting abgr = FormColorData.argb2Abgr(stored);
        ModNetwork.CHANNEL.sendToServer(new UpdateSkinPacket(false, false, false,
                abgr.primaryColor(), abgr.accentColor1(), abgr.accentColor2(),
                abgr.eyeColorA(), abgr.eyeColorB(), abgr.primaryGreyReverse(),
                abgr.accent1GreyReverse(), abgr.accent2GreyReverse()));
    }

    private static void deleteSlot(String scope, String slot, String formId) {
        FormColorData data = FormColorData.client();
        switch (scope) {
            case "form" -> {
                ResourceLocation form = ResourceLocation.tryParse(formId);
                Map<String, FormTextureUtils.ColorSetting> byForm =
                        form == null ? null : data.customSettingByForm.get(form);
                if (byForm != null) {
                    byForm.remove(slot);
                }
            }
            case "global" -> data.customSetting.remove(slot);
            case "form_default" -> {
                ResourceLocation form = ResourceLocation.tryParse(formId);
                if (form != null) {
                    data.formDefaultSetting.remove(form);
                }
            }
            default -> {
                return;
            }
        }
        data.writeToConfig();
    }

    private static void setEnableDefault(boolean value) {
        FormColorData data = FormColorData.client();
        data.enableDefaultFormColor = value;
        data.writeToConfig();
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("enableDefaultFormColor = " + value));
        }
    }

    private static void listSlots(String scope, String formId) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        FormColorData data = FormColorData.client();
        List<String> names = new ArrayList<>();
        switch (scope) {
            case "form" -> {
                ResourceLocation form = ResourceLocation.tryParse(formId);
                Map<String, FormTextureUtils.ColorSetting> byForm =
                        form == null ? null : data.customSettingByForm.get(form);
                if (byForm != null) {
                    names.addAll(byForm.keySet());
                }
            }
            case "global" -> names.addAll(data.customSetting.keySet());
            case "form_default" -> {
                for (ResourceLocation form : data.formDefaultSetting.keySet()) {
                    names.add(form.toString());
                }
            }
            default -> {
                return;
            }
        }
        Minecraft.getInstance().player.sendSystemMessage(
                Component.literal("slots[" + scope + "]: " + String.join(", ", names)));
    }
}
