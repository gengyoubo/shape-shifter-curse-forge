package net.onixary.shapeShifterCurseForge.client.codex;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.client.color.FormColorData;
import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.network.UpdateSkinPacket;
import net.onixary.shapeShifterCurseForge.other.config.SscClientConfig;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Fabric's original "professional" form color menu (FormColorSelectMenu, V1): direct hex
 * inputs, server/client/clipboard transfer, per-form local slots, global slots and a
 * per-form default slot. Browsing to another unlocked form also previews its model.
 */
public class FormColorSelectMenu extends Screen
        implements FormTextureUtils.TempFormTextureProcessor, FormTextureUtils.TempFormModelProcessor,
        FormTextureUtils.TempCustomSkinConfigOverrider {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/gui/v1_form_color_select_menu.png");
    private static final int BG_WIDTH = 420;
    private static final int BG_HEIGHT = 227;
    private static final int BG_IMAGE_WIDTH = 420;
    private static final int BG_IMAGE_HEIGHT = 428;
    private static final int EXTRA_PART_START_X = 0;
    private static final int EXTRA_PART_START_Y = 228;
    private static final int TEXT_COLOR = 0xDDDDDD;
    public static FormColorSelectMenu instance = null;

    private static final Component EMPTY_TEXT = Component.empty();
    private static final Component BOOL_BTN_ON = Component.translatable("text.cloth-config.boolean.value.true");
    private static final Component BOOL_BTN_OFF = Component.translatable("text.cloth-config.boolean.value.false");
    private static final Component FORM_SLOT_TITLE = Component.translatable("gui.shape_shifter_curse_fabric.fcs.form_slot_title");
    private static final Component GLOBAL_SLOT_TITLE = Component.translatable("gui.shape_shifter_curse_fabric.fcs.global_slot_title");
    private static final Component FORM_DEFAULT_SLOT_TITLE = Component.translatable("gui.shape_shifter_curse_fabric.fcs.form_default_slot_title");
    private static final Component TITLE = Component.translatable("gui.shape_shifter_curse_fabric.fcs.title");
    private static final Component COLOR_CHANNEL_R = Component.translatable("gui.shape_shifter_curse_fabric.fcs.color_channel_r");
    private static final Component COLOR_CHANNEL_G = Component.translatable("gui.shape_shifter_curse_fabric.fcs.color_channel_g");
    private static final Component COLOR_CHANNEL_B = Component.translatable("gui.shape_shifter_curse_fabric.fcs.color_channel_b");
    private static final Component COLOR_CHANNEL_H = Component.translatable("gui.shape_shifter_curse_fabric.fcs.color_channel_h");
    private static final Component COLOR_CHANNEL_S = Component.translatable("gui.shape_shifter_curse_fabric.fcs.color_channel_s");
    private static final Component COLOR_CHANNEL_V = Component.translatable("gui.shape_shifter_curse_fabric.fcs.color_channel_v");
    private static final Component IS_ENABLE_LAYER_LABEL = Component.translatable("gui.shape_shifter_curse_fabric.fcs.is_enable_layer");
    private static final Component EXIT_SLIDER_BUTTON_LABEL = Component.translatable("gui.shape_shifter_curse_fabric.fcs.exit_slider_button");
    private static final MutableComponent NONE_FROM_NAME_LABEL = Component.translatable("gui.shape_shifter_curse_fabric.fcs.none_from_name");
    private static final Component DOWNLOAD_FROM_SERVER = Component.translatable("gui.shape_shifter_curse_fabric.fcs.from_server");
    private static final Component UPLOAD_TO_SERVER = Component.translatable("gui.shape_shifter_curse_fabric.fcs.to_server");
    private static final Component DOWNLOAD_FROM_CLIENT = Component.translatable("gui.shape_shifter_curse_fabric.fcs.from_client");
    private static final Component UPLOAD_TO_CLIENT = Component.translatable("gui.shape_shifter_curse_fabric.fcs.to_client");
    private static final Component DOWNLOAD_FROM_CLIPBOARD = Component.translatable("gui.shape_shifter_curse_fabric.fcs.from_clipboard");
    private static final Component UPLOAD_TO_CLIPBOARD = Component.translatable("gui.shape_shifter_curse_fabric.fcs.to_clipboard");
    private static final Component PRIMARY_COLOR_LABEL = Component.translatable("gui.shape_shifter_curse_fabric.fcs.primaryColor");
    private static final Component ACCENT_COLOR_1_LABEL = Component.translatable("gui.shape_shifter_curse_fabric.fcs.accentColor1Color");
    private static final Component ACCENT_COLOR_2_LABEL = Component.translatable("gui.shape_shifter_curse_fabric.fcs.accentColor2Color");
    private static final Component EYE_COLOR_A_LABEL = Component.translatable("gui.shape_shifter_curse_fabric.fcs.eyeColorA");
    private static final Component EYE_COLOR_B_LABEL = Component.translatable("gui.shape_shifter_curse_fabric.fcs.eyeColorB");
    private static final Component PRIMARY_GREY_REVERSE_LABEL = Component.translatable("gui.shape_shifter_curse_fabric.fcs.primaryGreyReverse");
    private static final Component ACCENT_1_GREY_REVERSE_LABEL = Component.translatable("gui.shape_shifter_curse_fabric.fcs.accent1GreyReverse");
    private static final Component ACCENT_2_GREY_REVERSE_LABEL = Component.translatable("gui.shape_shifter_curse_fabric.fcs.accent2GreyReverse");
    private static final Component KEEP_ORIGINAL_SKIN_LABEL = Component.translatable("text.autoconfig.shape-shifter-curse-custom.option.keep_original_skin");
    private static final Component IS_ENABLE_FORM_COLOR_SYSTEM_LABEL = Component.translatable("text.autoconfig.shape-shifter-curse-custom.option.enable_form_color");

    private static final Minecraft minecraftClient = Minecraft.getInstance();

    private boolean isScreenInit = false;
    private EditBox primaryColorEditBox;
    private EditBox accentColor1EditBox;
    private EditBox accentColor2EditBox;
    private EditBox eyeColorAEditBox;
    private EditBox eyeColorBEditBox;
    private Button primaryGreyReverseButton;
    private Button accent1GreyReverseButton;
    private Button accent2GreyReverseButton;
    private Button keepOriginalSkinButton;
    private Button isEnableFormColorSystemButton;
    private SimpleIntSliderWidget sliderR;
    private SimpleIntSliderWidget sliderG;
    private SimpleIntSliderWidget sliderB;
    private EditBox sliderREditBox;
    private EditBox sliderGEditBox;
    private EditBox sliderBEditBox;
    private SimpleIntSliderWidget sliderH;
    private SimpleIntSliderWidget sliderS;
    private SimpleIntSliderWidget sliderV;
    private EditBox sliderHEditBox;
    private EditBox sliderSEditBox;
    private EditBox sliderVEditBox;
    private Button formNameLabel;
    private Button isEnableLayerButton;

    private final Map<String, Map<FormTextureUtils.ColorSetting, ResourceLocation>> colorSettingCacheMap = new HashMap<>();
    private String formKey = "";
    private int formIDIndex = -1;

    private int primaryColor = 0x00FFFFFF;
    private int accentColor1Color = 0x00FFFFFF;
    private int accentColor2Color = 0x00FFFFFF;
    private int eyeColorA = 0x00FFFFFF;
    private int eyeColorB = 0x00FFFFFF;
    private boolean primaryGreyReverse = false;
    private boolean accent1GreyReverse = false;
    private boolean accent2GreyReverse = false;
    private boolean keepOriginalSkin = false;
    private boolean enableFormColorSystem = true;
    private boolean isColorSettingDirty = true;
    private FormTextureUtils.ColorSetting colorSettingArgb = null;
    private FormTextureUtils.ColorSetting colorSettingAbgr = null;

    private int tempSliderConfigIndex = -1;
    private int tempSliderR = 0;
    private int tempSliderG = 0;
    private int tempSliderB = 0;
    private int tempSliderAlpha = 0;
    private int tempSliderH = 0;
    private int tempSliderS = 0;
    private int tempSliderV = 0;
    private boolean isOpenSlider = false;
    private final List<AbstractWidget> configPanel01 = new ArrayList<>();
    private final List<AbstractWidget> configPanel02 = new ArrayList<>();
    private boolean lastLoadDataIsServerSide = false;
    private boolean isUsingTempTexture = true;
    private boolean isUsingTempModel = true;
    private boolean isUsingSkinOverrider = true;
    private @Nullable Screen parsetScreen = null;
    private boolean isUpdateUI = false;
    private int isUpdateSlider = 0;
    private boolean isUpdateHSVFromRGB = false;
    private boolean isUpdateRGBFromHSV = false;
    private int timer = 0;
    // Fabric refreshes the open menu one second after the client form changes.
    private int pendingFormChangeTicks = -1;

    private final List<Pair<FcsButtonWidget, FcsButtonWidget>> formLocalSettingButtons = new ArrayList<>();
    private final List<EditBox> formLocalSettingTextFields = new ArrayList<>();
    private Pair<FcsButtonWidget, FcsButtonWidget> formDefaultSettingButton = null;
    private EditBox formDefaultSettingTextField = null;
    private final List<Pair<FcsButtonWidget, FcsButtonWidget>> globalSettingButtons = new ArrayList<>();
    private final List<EditBox> globalSettingTextFields = new ArrayList<>();

    public FormColorSelectMenu(Component title) {
        super(title);
        this.reloadFormIDIndex();
        this.loadData();
        if (!FormTextureUtils.useTempFormTexture) {
            FormTextureUtils.useTempFormTexture = true;
            FormTextureUtils.tempFormTextureProcessor = this;
        } else {
            LogUtils.getLogger().warn("Temp Texture System is already in use, dynamic texture rendering will not work");
            this.isUsingTempTexture = false;
        }
        if (FormTextureUtils.tempFormModelProcessor == null) {
            FormTextureUtils.tempFormModelProcessor = this;
        } else {
            LogUtils.getLogger().warn("Temp Form Model System is already in use, dynamic form rendering will not work");
            this.isUsingTempModel = false;
        }
        if (FormTextureUtils.tempCustomSkinConfigOverrider == null) {
            FormTextureUtils.tempCustomSkinConfigOverrider = this;
        } else {
            LogUtils.getLogger().warn("Temp Custom Skin Config System is already in use, dynamic custom skin config will not work");
            this.isUsingSkinOverrider = false;
        }
        if (instance != null) {
            LogUtils.getLogger().error("FormColorSelectMenu is already in use, only one instance is allowed");
        }
        instance = this;
    }

    public FormColorSelectMenu(Component title, @Nullable Screen parsetScreen) {
        this(title);
        this.parsetScreen = parsetScreen;
    }

    private static List<ResourceLocation> formIds() {
        return new ArrayList<>(FormRegistry.forms().keySet());
    }

    public void scrollFormID(int offset, boolean loop) {
        int size = formIds().size();
        if (this.formIDIndex < 0) {
            this.formIDIndex = 0;
        }
        this.formIDIndex += offset;
        if (this.formIDIndex < 0) {
            this.formIDIndex = loop ? size - 1 : 0;
        } else if (this.formIDIndex >= size) {
            this.formIDIndex = loop ? 0 : size - 1;
        }
        this.onFormChange(false, false);
    }

    public void reloadFormIDName() {
        ResourceLocation form = this.getFormNoCheckUnlock();
        boolean isUnlocked = SscClientConfig.DISABLE_COLOR_UNLOCK_CHECK.get() || FormColorData.client().isUnlock(form);
        Component message = NONE_FROM_NAME_LABEL;
        if (!FormRegistry.ORIGINAL_BEFORE_ENABLE.equals(form)) {
            message = CodexData.getContentText(form, CodexData.ContentType.NAME);
        }
        if (!isUnlocked) {
            message = message.copy().setStyle(message.getStyle().withColor(TextColor.fromRgb(0xFF0000)));
        }
        this.formNameLabel.setMessage(message);
    }

    public void reloadFormIDIndex() {
        this.formIDIndex = -1;
        if (minecraftClient.player == null) {
            return;
        }
        ResourceLocation form = FormManager.current(minecraftClient.player).id();
        List<ResourceLocation> forms = formIds();
        for (int index = 0; index < forms.size(); index++) {
            if (Objects.equals(forms.get(index), form)) {
                this.formIDIndex = index;
                return;
            }
        }
    }

    public ResourceLocation getFormNoCheckUnlock() {
        List<ResourceLocation> forms = formIds();
        if (this.formIDIndex < 0 || this.formIDIndex >= forms.size()) {
            return FormRegistry.ORIGINAL_BEFORE_ENABLE;
        }
        return forms.get(this.formIDIndex);
    }

    /** The browsed form when unlocked, otherwise the player's own form. */
    public ResourceLocation getForm() {
        ResourceLocation form = this.getFormNoCheckUnlock();
        if (SscClientConfig.DISABLE_COLOR_UNLOCK_CHECK.get() || FormColorData.client().isUnlock(form)) {
            return form;
        }
        return minecraftClient.player != null
                ? FormManager.current(minecraftClient.player).id()
                : FormRegistry.ORIGINAL_BEFORE_ENABLE;
    }

    @Nullable
    private ResourceLocation getPlayerForm() {
        ResourceLocation form = this.getForm();
        return FormRegistry.ORIGINAL_BEFORE_ENABLE.equals(form) ? null : form;
    }

    @Override
    @Nullable
    public ResourceLocation getPreviewForm() {
        return this.getForm();
    }

    @Override
    public boolean keepOriginalSkin() {
        return this.keepOriginalSkin;
    }

    public void updateSlider() {
        int color = this.tempSliderAlpha << 24 | this.tempSliderR << 16 | this.tempSliderG << 8 | this.tempSliderB;
        switch (this.tempSliderConfigIndex) {
            case 0 -> this.primaryColor = color;
            case 1 -> this.accentColor1Color = color;
            case 2 -> this.accentColor2Color = color;
            case 3 -> this.eyeColorA = color;
            case 4 -> this.eyeColorB = color;
            default -> {
            }
        }
        this.isColorSettingDirty = true;
    }

    public void updateSliderHSV() {
        this.isUpdateRGBFromHSV = true;
        int[] color = FormTextureUtils.hsvToRgb(this.tempSliderH, this.tempSliderS, this.tempSliderV);
        this.tempSliderR = color[0];
        this.tempSliderG = color[1];
        this.tempSliderB = color[2];
        this.sliderR.setIntValue(this.tempSliderR);
        this.sliderG.setIntValue(this.tempSliderG);
        this.sliderB.setIntValue(this.tempSliderB);
        this.isUpdateRGBFromHSV = false;
    }

    public void updateSliderRGB() {
        this.isUpdateHSVFromRGB = true;
        int[] color = FormTextureUtils.rgbToHsv(this.tempSliderR, this.tempSliderG, this.tempSliderB);
        this.tempSliderH = color[0];
        this.tempSliderS = color[1];
        this.tempSliderV = color[2];
        this.sliderH.setIntValue(this.tempSliderH);
        this.sliderS.setIntValue(this.tempSliderS);
        this.sliderV.setIntValue(this.tempSliderV);
        this.isUpdateHSVFromRGB = false;
    }

    public void reloadSlider() {
        int sliderColor = switch (this.tempSliderConfigIndex) {
            case 0 -> this.primaryColor;
            case 1 -> this.accentColor1Color;
            case 2 -> this.accentColor2Color;
            case 3 -> this.eyeColorA;
            case 4 -> this.eyeColorB;
            default -> 0;
        };
        this.tempSliderR = sliderColor >>> 16 & 0xFF;
        this.tempSliderG = sliderColor >>> 8 & 0xFF;
        this.tempSliderB = sliderColor & 0xFF;
        this.tempSliderAlpha = sliderColor >>> 24 & 0xFF;
        this.sliderR.setIntValue(this.tempSliderR);
        this.sliderG.setIntValue(this.tempSliderG);
        this.sliderB.setIntValue(this.tempSliderB);
        this.isEnableLayerButton.setMessage(this.tempSliderAlpha != 0 ? BOOL_BTN_ON : BOOL_BTN_OFF);
        this.updateSliderRGB();
    }

    public void updatePanel() {
        this.configPanel01.forEach(element -> element.visible = !this.isOpenSlider);
        this.configPanel02.forEach(element -> element.visible = this.isOpenSlider);
        this.updateUI();
    }

    public void loadData(FormTextureUtils.ColorSetting colorSetting) {
        this.primaryColor = colorSetting.primaryColor();
        this.accentColor1Color = colorSetting.accentColor1();
        this.accentColor2Color = colorSetting.accentColor2();
        this.eyeColorA = colorSetting.eyeColorA();
        this.eyeColorB = colorSetting.eyeColorB();
        this.primaryGreyReverse = colorSetting.primaryGreyReverse();
        this.accent1GreyReverse = colorSetting.accent1GreyReverse();
        this.accent2GreyReverse = colorSetting.accent2GreyReverse();
        this.isColorSettingDirty = true;
        this.updateUI();
    }

    public void loadServerData(FormTextureUtils.ColorSetting colorSetting) {
        this.loadData(FormColorData.abgr2Argb(colorSetting));
    }

    public void loadData() {
        this.lastLoadDataIsServerSide = minecraftClient.player != null;
        this.loadData(this.lastLoadDataIsServerSide);
    }

    public void loadData(boolean serverSide) {
        if (serverSide) {
            if (minecraftClient.player != null) {
                SscApi.currentSkin(minecraftClient.player).ifPresent(skin -> {
                    this.keepOriginalSkin = skin.isKeepOriginalSkin();
                    this.enableFormColorSystem = skin.isEnableFormColor();
                    this.loadServerData(skin.getFormColor());
                });
            }
        } else {
            this.primaryColor = SscClientConfig.CUSTOM_PRIMARY_COLOR.get();
            this.accentColor1Color = SscClientConfig.CUSTOM_ACCENT_COLOR_1.get();
            this.accentColor2Color = SscClientConfig.CUSTOM_ACCENT_COLOR_2.get();
            this.eyeColorA = SscClientConfig.CUSTOM_EYE_COLOR_A.get();
            this.eyeColorB = SscClientConfig.CUSTOM_EYE_COLOR_B.get();
            this.primaryGreyReverse = SscClientConfig.CUSTOM_PRIMARY_GREY_REVERSE.get();
            this.accent1GreyReverse = SscClientConfig.CUSTOM_ACCENT_1_GREY_REVERSE.get();
            this.accent2GreyReverse = SscClientConfig.CUSTOM_ACCENT_2_GREY_REVERSE.get();
            this.keepOriginalSkin = SscClientConfig.CUSTOM_KEEP_ORIGINAL_SKIN.get();
            this.enableFormColorSystem = SscClientConfig.CUSTOM_ENABLE_FORM_COLOR.get();
            this.updateUI();
        }
        this.isColorSettingDirty = true;
    }

    public void saveDataToClient(boolean saveColorData, boolean saveExtraData) {
        if (saveColorData) {
            SscClientConfig.CUSTOM_PRIMARY_COLOR.set(this.primaryColor);
            SscClientConfig.CUSTOM_ACCENT_COLOR_1.set(this.accentColor1Color);
            SscClientConfig.CUSTOM_ACCENT_COLOR_2.set(this.accentColor2Color);
            SscClientConfig.CUSTOM_EYE_COLOR_A.set(this.eyeColorA);
            SscClientConfig.CUSTOM_EYE_COLOR_B.set(this.eyeColorB);
            SscClientConfig.CUSTOM_PRIMARY_GREY_REVERSE.set(this.primaryGreyReverse);
            SscClientConfig.CUSTOM_ACCENT_1_GREY_REVERSE.set(this.accent1GreyReverse);
            SscClientConfig.CUSTOM_ACCENT_2_GREY_REVERSE.set(this.accent2GreyReverse);
        }
        if (saveExtraData) {
            SscClientConfig.CUSTOM_KEEP_ORIGINAL_SKIN.set(this.keepOriginalSkin);
            SscClientConfig.CUSTOM_ENABLE_FORM_COLOR.set(this.enableFormColorSystem);
        }
        SscClientConfig.SPEC.save();
    }

    public FormTextureUtils.ColorSetting getColorSetting(boolean abgr) {
        if (this.isColorSettingDirty) {
            this.colorSettingArgb = new FormTextureUtils.ColorSetting(this.primaryColor, this.accentColor1Color,
                    this.accentColor2Color, this.eyeColorA, this.eyeColorB,
                    this.primaryGreyReverse, this.accent1GreyReverse, this.accent2GreyReverse);
            this.colorSettingAbgr = FormColorData.argb2Abgr(this.colorSettingArgb);
            this.isColorSettingDirty = false;
        }
        return abgr ? this.colorSettingAbgr : this.colorSettingArgb;
    }

    /** Sends the edited colors with the extra flags, like Fabric's sendUpdateCustomColor. */
    private void sendToServer() {
        if (minecraftClient.getConnection() == null) {
            return;
        }
        FormTextureUtils.ColorSetting setting = this.getColorSetting(true);
        ModNetwork.CHANNEL.sendToServer(new UpdateSkinPacket(true, this.keepOriginalSkin, this.enableFormColorSystem,
                setting.primaryColor(), setting.accentColor1(), setting.accentColor2(),
                setting.eyeColorA(), setting.eyeColorB(), setting.primaryGreyReverse(),
                setting.accent1GreyReverse(), setting.accent2GreyReverse()));
    }

    public static int colorChannel2Int(String channel) {
        return FormColorSelectMenuV2.colorChannel2Int(channel, 0, 255);
    }

    public void onConfigChanged() {
        if (!this.isScreenInit || this.isUpdateUI) {
            return;
        }
        this.primaryColor = FormColorSelectMenuV2.decodeColor(this.primaryColorEditBox.getValue());
        this.accentColor1Color = FormColorSelectMenuV2.decodeColor(this.accentColor1EditBox.getValue());
        this.accentColor2Color = FormColorSelectMenuV2.decodeColor(this.accentColor2EditBox.getValue());
        this.eyeColorA = FormColorSelectMenuV2.decodeColor(this.eyeColorAEditBox.getValue());
        this.eyeColorB = FormColorSelectMenuV2.decodeColor(this.eyeColorBEditBox.getValue());
        this.isColorSettingDirty = true;
    }

    public void updateUI() {
        if (!this.isScreenInit) {
            return;
        }
        this.isUpdateUI = true;
        this.primaryColorEditBox.setValue(FormColorSelectMenuV2.encodeColor(this.primaryColor));
        this.accentColor1EditBox.setValue(FormColorSelectMenuV2.encodeColor(this.accentColor1Color));
        this.accentColor2EditBox.setValue(FormColorSelectMenuV2.encodeColor(this.accentColor2Color));
        this.eyeColorAEditBox.setValue(FormColorSelectMenuV2.encodeColor(this.eyeColorA));
        this.eyeColorBEditBox.setValue(FormColorSelectMenuV2.encodeColor(this.eyeColorB));
        this.primaryGreyReverseButton.setMessage(this.primaryGreyReverse ? BOOL_BTN_ON : BOOL_BTN_OFF);
        this.accent1GreyReverseButton.setMessage(this.accent1GreyReverse ? BOOL_BTN_ON : BOOL_BTN_OFF);
        this.accent2GreyReverseButton.setMessage(this.accent2GreyReverse ? BOOL_BTN_ON : BOOL_BTN_OFF);
        this.keepOriginalSkinButton.setMessage(this.keepOriginalSkin ? BOOL_BTN_ON : BOOL_BTN_OFF);
        this.isEnableFormColorSystemButton.setMessage(this.enableFormColorSystem ? BOOL_BTN_ON : BOOL_BTN_OFF);
        this.reloadSlider();
        this.reloadAllSlotName();
        this.isUpdateUI = false;
    }

    public void onFormChange(boolean force, boolean reloadColorData) {
        if (!this.isScreenInit) {
            return;
        }
        if (force) {
            this.reloadFormIDIndex();
        }
        if (reloadColorData) {
            this.loadData();
        } else {
            this.updateUI();
        }
        this.reloadFormIDName();
    }

    /** Called when the local player's form changes; the open menu refreshes a second later. */
    public static void onClientFormChange() {
        if (instance != null) {
            instance.pendingFormChangeTicks = 20;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.pendingFormChangeTicks > 0 && --this.pendingFormChangeTicks == 0) {
            this.pendingFormChangeTicks = -1;
            this.onFormChange(true, true);
        }
    }

    private Button toggle(boolean initial, int x, int y, int width, Runnable onToggle) {
        return Button.builder(initial ? BOOL_BTN_ON : BOOL_BTN_OFF, button -> onToggle.run())
                .pos(x, y).size(width, 11).build();
    }

    private <T extends AbstractWidget> T panel(List<AbstractWidget> panel, T widget) {
        this.addRenderableWidget(widget);
        panel.add(widget);
        return widget;
    }

    private EditBox colorInput(int x, int y) {
        EditBox input = new EditBox(this.font, x, y, 70, 11, EMPTY_TEXT);
        input.setMaxLength(9);
        input.setResponder(text -> this.onConfigChanged());
        return this.panel(this.configPanel01, input);
    }

    @Override
    protected void init() {
        super.init();
        // init() reruns on resize; rebuild every widget list from scratch.
        this.isScreenInit = false;
        this.configPanel01.clear();
        this.configPanel02.clear();
        int bPosX = this.width / 2 - BG_WIDTH / 2;
        int bPosY = this.height / 2 - BG_HEIGHT / 2;
        this.addRenderableWidget(new StringWidget(bPosX + 20, bPosY + 146, 80, 9, FORM_SLOT_TITLE, this.font).setColor(TEXT_COLOR));
        this.addRenderableWidget(new StringWidget(bPosX + 135, bPosY + 5, 180, 9, TITLE, this.font).setColor(TEXT_COLOR));
        this.addRenderableWidget(new StringWidget(bPosX + 320, bPosY + 5, 80, 9, GLOBAL_SLOT_TITLE, this.font).setColor(TEXT_COLOR));
        this.addRenderableWidget(new StringWidget(bPosX + 320, bPosY + 182, 80, 9, FORM_DEFAULT_SLOT_TITLE, this.font).setColor(TEXT_COLOR));
        this.addRenderableWidget(Button.builder(DOWNLOAD_FROM_SERVER, button -> this.loadData(true))
                .pos(bPosX + 85, bPosY + 5).size(45, 15).build());
        this.addRenderableWidget(Button.builder(UPLOAD_TO_SERVER, button -> this.sendToServer())
                .pos(bPosX + 85, bPosY + 23).size(45, 15).build());
        this.addRenderableWidget(Button.builder(DOWNLOAD_FROM_CLIENT, button -> this.loadData(false))
                .pos(bPosX + 85, bPosY + 41).size(45, 15).build());
        this.addRenderableWidget(Button.builder(UPLOAD_TO_CLIENT, button -> this.saveDataToClient(true, true))
                .pos(bPosX + 85, bPosY + 59).size(45, 15).build());
        this.addRenderableWidget(Button.builder(DOWNLOAD_FROM_CLIPBOARD, button -> {
            FormTextureUtils.ColorSetting setting =
                    FormColorData.colorSettingFromString(minecraftClient.keyboardHandler.getClipboard());
            if (setting != null) {
                this.loadData(setting);
            }
        }).pos(bPosX + 85, bPosY + 77).size(45, 15).build());
        this.addRenderableWidget(Button.builder(UPLOAD_TO_CLIPBOARD, button -> minecraftClient.keyboardHandler.setClipboard(
                FormColorData.colorSettingToString(this.getColorSetting(false), true)))
                .pos(bPosX + 85, bPosY + 95).size(45, 15).build());
        this.formNameLabel = this.addRenderableWidget(Button.builder(NONE_FROM_NAME_LABEL, button -> {
            this.reloadFormIDIndex();
            this.onFormChange(false, false);
        }).pos(bPosX + 35, bPosY + 128).size(80, 15).build());
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.scrollFormID(-1, true))
                .pos(bPosX + 20, bPosY + 128).size(15, 15).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.scrollFormID(1, true))
                .pos(bPosX + 115, bPosY + 128).size(15, 15).build());
        this.reloadFormIDName();

        // Panel 1: hex inputs and toggles.
        this.panel(this.configPanel01, new StringWidget(bPosX + 139, bPosY + 27, 75, 11, PRIMARY_COLOR_LABEL, this.font).setColor(TEXT_COLOR));
        this.primaryColorEditBox = this.colorInput(bPosX + 241, bPosY + 27);
        this.panel(this.configPanel01, new StringWidget(bPosX + 139, bPosY + 41, 75, 11, ACCENT_COLOR_1_LABEL, this.font).setColor(TEXT_COLOR));
        this.accentColor1EditBox = this.colorInput(bPosX + 241, bPosY + 41);
        this.panel(this.configPanel01, new StringWidget(bPosX + 139, bPosY + 55, 75, 11, ACCENT_COLOR_2_LABEL, this.font).setColor(TEXT_COLOR));
        this.accentColor2EditBox = this.colorInput(bPosX + 241, bPosY + 55);
        this.panel(this.configPanel01, new StringWidget(bPosX + 139, bPosY + 69, 75, 11, EYE_COLOR_A_LABEL, this.font).setColor(TEXT_COLOR));
        this.eyeColorAEditBox = this.colorInput(bPosX + 241, bPosY + 69);
        this.panel(this.configPanel01, new StringWidget(bPosX + 139, bPosY + 83, 75, 11, EYE_COLOR_B_LABEL, this.font).setColor(TEXT_COLOR));
        this.eyeColorBEditBox = this.colorInput(bPosX + 241, bPosY + 83);
        this.panel(this.configPanel01, new StringWidget(bPosX + 139, bPosY + 97, 139, 11, PRIMARY_GREY_REVERSE_LABEL, this.font).setColor(TEXT_COLOR));
        this.primaryGreyReverseButton = this.panel(this.configPanel01, this.toggle(this.primaryGreyReverse, bPosX + 281, bPosY + 97, 30, () -> {
            this.primaryGreyReverse = !this.primaryGreyReverse;
            this.primaryGreyReverseButton.setMessage(this.primaryGreyReverse ? BOOL_BTN_ON : BOOL_BTN_OFF);
            this.isColorSettingDirty = true;
        }));
        this.panel(this.configPanel01, new StringWidget(bPosX + 139, bPosY + 111, 139, 11, ACCENT_1_GREY_REVERSE_LABEL, this.font).setColor(TEXT_COLOR));
        this.accent1GreyReverseButton = this.panel(this.configPanel01, this.toggle(this.accent1GreyReverse, bPosX + 281, bPosY + 111, 30, () -> {
            this.accent1GreyReverse = !this.accent1GreyReverse;
            this.accent1GreyReverseButton.setMessage(this.accent1GreyReverse ? BOOL_BTN_ON : BOOL_BTN_OFF);
            this.isColorSettingDirty = true;
        }));
        this.panel(this.configPanel01, new StringWidget(bPosX + 139, bPosY + 125, 139, 11, ACCENT_2_GREY_REVERSE_LABEL, this.font).setColor(TEXT_COLOR));
        this.accent2GreyReverseButton = this.panel(this.configPanel01, this.toggle(this.accent2GreyReverse, bPosX + 281, bPosY + 125, 30, () -> {
            this.accent2GreyReverse = !this.accent2GreyReverse;
            this.accent2GreyReverseButton.setMessage(this.accent2GreyReverse ? BOOL_BTN_ON : BOOL_BTN_OFF);
            this.isColorSettingDirty = true;
        }));
        this.panel(this.configPanel01, new StringWidget(bPosX + 139, bPosY + 153, 139, 11, KEEP_ORIGINAL_SKIN_LABEL, this.font).setColor(TEXT_COLOR));
        this.keepOriginalSkinButton = this.panel(this.configPanel01, this.toggle(this.keepOriginalSkin, bPosX + 281, bPosY + 153, 30, () -> {
            this.keepOriginalSkin = !this.keepOriginalSkin;
            this.keepOriginalSkinButton.setMessage(this.keepOriginalSkin ? BOOL_BTN_ON : BOOL_BTN_OFF);
        }));
        this.panel(this.configPanel01, new StringWidget(bPosX + 139, bPosY + 167, 139, 11, IS_ENABLE_FORM_COLOR_SYSTEM_LABEL, this.font).setColor(TEXT_COLOR));
        this.isEnableFormColorSystemButton = this.panel(this.configPanel01, this.toggle(this.enableFormColorSystem, bPosX + 281, bPosY + 167, 30, () -> {
            this.enableFormColorSystem = !this.enableFormColorSystem;
            this.isEnableFormColorSystemButton.setMessage(this.enableFormColorSystem ? BOOL_BTN_ON : BOOL_BTN_OFF);
        }));

        // Panel 2: RGB and HSV sliders for the selected color.
        this.panel(this.configPanel02, new StringWidget(bPosX + 139, bPosY + 27, 25, 11, COLOR_CHANNEL_R, this.font));
        this.panel(this.configPanel02, new StringWidget(bPosX + 139, bPosY + 41, 25, 11, COLOR_CHANNEL_G, this.font));
        this.panel(this.configPanel02, new StringWidget(bPosX + 139, bPosY + 55, 25, 11, COLOR_CHANNEL_B, this.font));
        this.sliderREditBox = this.rgbInput(bPosX + 177, bPosY + 27, text -> this.tempSliderR = colorChannel2Int(text), () -> this.sliderR.setIntValue(this.tempSliderR));
        this.sliderGEditBox = this.rgbInput(bPosX + 177, bPosY + 41, text -> this.tempSliderG = colorChannel2Int(text), () -> this.sliderG.setIntValue(this.tempSliderG));
        this.sliderBEditBox = this.rgbInput(bPosX + 177, bPosY + 55, text -> this.tempSliderB = colorChannel2Int(text), () -> this.sliderB.setIntValue(this.tempSliderB));
        this.sliderR = this.slider(bPosX + 211, bPosY + 27, 255, () -> this.sliderREditBox);
        this.sliderG = this.slider(bPosX + 211, bPosY + 41, 255, () -> this.sliderGEditBox);
        this.sliderB = this.slider(bPosX + 211, bPosY + 55, 255, () -> this.sliderBEditBox);
        this.panel(this.configPanel02, new StringWidget(bPosX + 139, bPosY + 69, 25, 11, COLOR_CHANNEL_H, this.font));
        this.panel(this.configPanel02, new StringWidget(bPosX + 139, bPosY + 83, 25, 11, COLOR_CHANNEL_S, this.font));
        this.panel(this.configPanel02, new StringWidget(bPosX + 139, bPosY + 97, 25, 11, COLOR_CHANNEL_V, this.font));
        this.sliderHEditBox = this.hsvInput(bPosX + 177, bPosY + 69, text -> this.tempSliderH = FormColorSelectMenuV2.colorChannel2Int(text, 0, 359), () -> this.sliderH.setIntValue(this.tempSliderH));
        this.sliderSEditBox = this.hsvInput(bPosX + 177, bPosY + 83, text -> this.tempSliderS = FormColorSelectMenuV2.colorChannel2Int(text, 0, 100), () -> this.sliderS.setIntValue(this.tempSliderS));
        this.sliderVEditBox = this.hsvInput(bPosX + 177, bPosY + 97, text -> this.tempSliderV = FormColorSelectMenuV2.colorChannel2Int(text, 0, 100), () -> this.sliderV.setIntValue(this.tempSliderV));
        this.sliderH = this.slider(bPosX + 211, bPosY + 69, 359, () -> this.sliderHEditBox);
        this.sliderS = this.slider(bPosX + 211, bPosY + 83, 100, () -> this.sliderSEditBox);
        this.sliderV = this.slider(bPosX + 211, bPosY + 97, 100, () -> this.sliderVEditBox);
        this.panel(this.configPanel02, new StringWidget(bPosX + 139, bPosY + 111, 75, 11, IS_ENABLE_LAYER_LABEL, this.font).setColor(TEXT_COLOR));
        this.isEnableLayerButton = this.panel(this.configPanel02, Button.builder(this.tempSliderAlpha != 0 ? BOOL_BTN_ON : BOOL_BTN_OFF, button -> {
            this.tempSliderAlpha = this.tempSliderAlpha == 0 ? 255 : 0;
            button.setMessage(this.tempSliderAlpha != 0 ? BOOL_BTN_ON : BOOL_BTN_OFF);
            this.updateSlider();
        }).pos(bPosX + 228, bPosY + 111).size(36, 11).build());
        this.panel(this.configPanel02, Button.builder(EXIT_SLIDER_BUTTON_LABEL, button -> {
            this.updateSlider();
            this.isOpenSlider = false;
            this.updatePanel();
        }).pos(bPosX + 281, bPosY + 111).size(30, 11).build());

        this.formLocalSettingButtons.clear();
        this.formLocalSettingTextFields.clear();
        this.globalSettingButtons.clear();
        this.globalSettingTextFields.clear();
        for (int index = 0; index < FormColorData.localSlotCount; index++) {
            this.createSaveDataButtons(0, index, bPosX + 20, bPosY + 158 + index * 18);
        }
        for (int index = 0; index < FormColorData.globalSlotCount; index++) {
            this.createSaveDataButtons(1, index, bPosX + 320, bPosY + 17 + index * 18);
        }
        this.createSaveDataButtons(2, 0, bPosX + 320, bPosY + 194);
        this.isScreenInit = true;
        this.updatePanel();
    }

    /** RGB text box: typing moves its slider; slider echoes update the color and HSV. */
    private EditBox rgbInput(int x, int y, java.util.function.Consumer<String> store, Runnable moveSlider) {
        EditBox input = new EditBox(this.font, x, y, 30, 11, EMPTY_TEXT);
        input.setMaxLength(3);
        input.setResponder(text -> {
            store.accept(text);
            if (this.isUpdateSlider == 0) {
                moveSlider.run();
            } else {
                this.updateSlider();
                if (!this.isUpdateRGBFromHSV) {
                    this.updateSliderRGB();
                }
            }
        });
        return this.panel(this.configPanel02, input);
    }

    /** HSV text box: typing moves its slider; slider echoes recompute RGB. */
    private EditBox hsvInput(int x, int y, java.util.function.Consumer<String> store, Runnable moveSlider) {
        EditBox input = new EditBox(this.font, x, y, 30, 11, EMPTY_TEXT);
        input.setMaxLength(3);
        input.setResponder(text -> {
            store.accept(text);
            if (this.isUpdateSlider == 0) {
                moveSlider.run();
            } else if (!this.isUpdateHSVFromRGB) {
                this.updateSliderHSV();
            }
        });
        return this.panel(this.configPanel02, input);
    }

    private SimpleIntSliderWidget slider(int x, int y, int max, Supplier<EditBox> echo) {
        SimpleIntSliderWidget slider = new SimpleIntSliderWidget(x, y, 100, 11, EMPTY_TEXT, 0.0D, 0, max);
        slider.onChanged = widget -> {
            this.isUpdateSlider++;
            echo.get().setValue(String.valueOf(widget.getIntValue()));
            this.isUpdateSlider--;
        };
        return this.panel(this.configPanel02, slider);
    }

    private void drawExtraPart(GuiGraphics graphics, int x, int y, int partX) {
        graphics.blit(TEXTURE, x, y, partX + EXTRA_PART_START_X, EXTRA_PART_START_Y, 184, 181,
                BG_IMAGE_WIDTH, BG_IMAGE_HEIGHT);
    }

    public void renderTextureBackground(GuiGraphics graphics) {
        int bgX = this.width / 2 - BG_WIDTH / 2;
        int bgY = this.height / 2 - BG_HEIGHT / 2;
        graphics.blit(TEXTURE, bgX, bgY, 0, 0, BG_WIDTH, BG_HEIGHT, BG_IMAGE_WIDTH, BG_IMAGE_HEIGHT);
        this.drawExtraPart(graphics, bgX + 133, bgY + 20, this.isOpenSlider ? 184 : 0);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bPosX = this.width / 2 - BG_WIDTH / 2;
        int bPosY = this.height / 2 - BG_HEIGHT / 2;
        this.renderBackground(graphics);
        this.renderTextureBackground(graphics);
        if (!this.isOpenSlider) {
            graphics.fill(bPosX + 228, bPosY + 27, bPosX + 239, bPosY + 38, this.primaryColor);
            graphics.fill(bPosX + 228, bPosY + 41, bPosX + 239, bPosY + 52, this.accentColor1Color);
            graphics.fill(bPosX + 228, bPosY + 55, bPosX + 239, bPosY + 66, this.accentColor2Color);
            graphics.fill(bPosX + 228, bPosY + 69, bPosX + 239, bPosY + 80, this.eyeColorA);
            graphics.fill(bPosX + 228, bPosY + 83, bPosX + 239, bPosY + 94, this.eyeColorB);
        } else {
            graphics.fill(bPosX + 267, bPosY + 111, bPosX + 278, bPosY + 122,
                    this.tempSliderAlpha << 24 | this.tempSliderR << 16 | this.tempSliderG << 8 | this.tempSliderB);
        }
        if (this.timer > 60) {
            this.updateSavaButtonActive();
        } else {
            this.timer++;
        }
        if (minecraftClient.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, bPosX + 50, bPosY + 100, 30,
                    (float) (bPosX + 50 - mouseX), (float) (bPosY + 100 - mouseY), minecraftClient.player);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void cleanColorSettingCache() {
        for (ResourceLocation id : this.colorSettingCacheMap.values().stream()
                .flatMap(map -> map.values().stream()).toList()) {
            FormTextureUtils.releaseTexture(id);
        }
        this.colorSettingCacheMap.clear();
    }

    @Override
    public ResourceLocation getTexture(String formKey, ResourceLocation texture,
                                       ResourceLocation mask, boolean onlyMultiply) {
        if (!this.formKey.equals(formKey)) {
            this.formKey = formKey;
            this.cleanColorSettingCache();
        }
        if (!this.enableFormColorSystem) {
            return texture;
        }
        Map<FormTextureUtils.ColorSetting, ResourceLocation> cache =
                this.colorSettingCacheMap.computeIfAbsent(formKey, key -> new HashMap<>());
        FormTextureUtils.ColorSetting setting = this.getColorSetting(true);
        ResourceLocation id = cache.get(setting);
        if (id == null) {
            com.mojang.blaze3d.platform.NativeImage image =
                    FormTextureUtils.bakeTextureImage(texture, mask, setting, onlyMultiply);
            if (image == null) {
                return texture;
            }
            id = FormTextureUtils.registerBakedTexture(image, "fcs_v1_");
            cache.put(setting, id);
        }
        return id;
    }

    @Override
    public void onClose() {
        this.cleanColorSettingCache();
        if (this.isUsingTempTexture) {
            FormTextureUtils.useTempFormTexture = false;
            FormTextureUtils.tempFormTextureProcessor = null;
            this.isUsingTempTexture = false;
        }
        if (this.isUsingTempModel) {
            FormTextureUtils.tempFormModelProcessor = null;
            this.isUsingTempModel = false;
        }
        if (this.isUsingSkinOverrider) {
            FormTextureUtils.tempCustomSkinConfigOverrider = null;
            this.isUsingSkinOverrider = false;
        }
        instance = null;
        try {
            boolean autoSync = SscClientConfig.CUSTOM_AUTO_SYNC_CONFIG.get();
            this.saveDataToClient(autoSync && SscClientConfig.CUSTOM_AUTO_SYNC_COLOR_CONFIG.get(), autoSync);
            this.sendToServer();
        } catch (Exception ignored) {
        }
        if (!this.lastLoadDataIsServerSide) {
            this.saveDataToClient(true, true);
        }
        FormColorData.client().writeToConfig();
        if (this.parsetScreen != null && this.minecraft != null) {
            this.minecraft.setScreen(this.parsetScreen);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        int bPosX = this.width / 2 - BG_WIDTH / 2;
        int bPosY = this.height / 2 - BG_HEIGHT / 2;
        if (!this.isOpenSlider && this.isScreenInit
                && mouseX > bPosX + 228 && mouseX < bPosX + 239) {
            for (int index = 0; index < 5; index++) {
                int top = bPosY + 27 + index * 14;
                if (mouseY > top && mouseY < top + 11) {
                    this.tempSliderConfigIndex = index;
                    this.isOpenSlider = true;
                    this.updatePanel();
                    return true;
                }
            }
        }
        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        // Fabric closes on the inventory key; skip that while typing, since hex values contain E.
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)
                && !(this.getFocused() instanceof EditBox editBox && editBox.canConsumeInput())) {
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isFormLocalSettingExists(int index) {
        ResourceLocation formId = this.getPlayerForm();
        return formId != null && FormColorData.client().customSettingByForm
                .getOrDefault(formId, Map.of()).containsKey(String.format("fcs_%s", index));
    }

    @Nullable
    private FormTextureUtils.ColorSetting getFormLocalSetting(int index) {
        ResourceLocation formId = this.getPlayerForm();
        return formId == null ? null : FormColorData.client().customSettingByForm
                .getOrDefault(formId, Map.of()).get(String.format("fcs_%s", index));
    }

    private void setFormLocalSetting(int index) {
        ResourceLocation formId = this.getPlayerForm();
        if (formId != null) {
            FormColorData.client().customSettingByForm.computeIfAbsent(formId, key -> new HashMap<>())
                    .put(String.format("fcs_%s", index), this.getColorSetting(false));
        }
        this.updateSavaButtonActive();
    }

    private void removeFormLocalSetting(int index) {
        ResourceLocation formId = this.getPlayerForm();
        if (formId != null) {
            FormColorData.client().customSettingByForm.computeIfAbsent(formId, key -> new HashMap<>())
                    .remove(String.format("fcs_%s", index));
        }
        this.updateSavaButtonActive();
    }

    private boolean isGlobalSettingExists(int index) {
        return FormColorData.client().customSetting.containsKey(String.format("fcs_%s", index));
    }

    @Nullable
    private FormTextureUtils.ColorSetting getGlobalSetting(int index) {
        return FormColorData.client().customSetting.get(String.format("fcs_%s", index));
    }

    private void setGlobalSetting(int index) {
        FormColorData.client().customSetting.put(String.format("fcs_%s", index), this.getColorSetting(false));
        this.updateSavaButtonActive();
    }

    private void removeGlobalSetting(int index) {
        FormColorData.client().customSetting.remove(String.format("fcs_%s", index));
        this.updateSavaButtonActive();
    }

    private boolean isFormDefaultSettingExists() {
        ResourceLocation formId = this.getPlayerForm();
        return formId != null && FormColorData.client().formDefaultSetting.containsKey(formId);
    }

    @Nullable
    private FormTextureUtils.ColorSetting getFormDefaultSetting() {
        ResourceLocation formId = this.getPlayerForm();
        return formId == null ? null : FormColorData.client().formDefaultSetting.get(formId);
    }

    private void setFormDefaultSetting() {
        ResourceLocation formId = this.getPlayerForm();
        if (formId != null) {
            FormColorData.client().formDefaultSetting.put(formId, this.getColorSetting(false));
        }
        this.updateSavaButtonActive();
    }

    private void removeFormDefaultSetting() {
        ResourceLocation formId = this.getPlayerForm();
        if (formId != null) {
            FormColorData.client().formDefaultSetting.remove(formId);
        }
        this.updateSavaButtonActive();
    }

    private static void setSlotState(Pair<FcsButtonWidget, FcsButtonWidget> buttons, boolean usable, boolean dataExist) {
        buttons.getFirst().active = usable;
        buttons.getFirst().textureX = usable && dataExist ? 15 : 0;
        buttons.getSecond().active = usable && dataExist;
    }

    private void updateSavaButtonActive() {
        if (!this.isScreenInit) {
            return;
        }
        // Form slots need a player form to belong to; global slots are always usable.
        boolean inGame = minecraftClient.player != null;
        for (int index = 0; index < this.formLocalSettingButtons.size(); index++) {
            setSlotState(this.formLocalSettingButtons.get(index), inGame, inGame && this.isFormLocalSettingExists(index));
        }
        setSlotState(this.formDefaultSettingButton, inGame, inGame && this.isFormDefaultSettingExists());
        for (int index = 0; index < this.globalSettingButtons.size(); index++) {
            setSlotState(this.globalSettingButtons.get(index), true, this.isGlobalSettingExists(index));
        }
    }

    private void saveCustomColorData(int buttonType, int index) {
        switch (buttonType) {
            case 0 -> this.setFormLocalSetting(index);
            case 1 -> this.setGlobalSetting(index);
            case 2 -> this.setFormDefaultSetting();
            default -> {
            }
        }
    }

    private void deleteSaveData(int buttonType, int index) {
        switch (buttonType) {
            case 0 -> this.removeFormLocalSetting(index);
            case 1 -> this.removeGlobalSetting(index);
            case 2 -> this.removeFormDefaultSetting();
            default -> {
            }
        }
    }

    private void loadSaveData(int buttonType, int index) {
        FormTextureUtils.ColorSetting setting = switch (buttonType) {
            case 0 -> this.getFormLocalSetting(index);
            case 1 -> this.getGlobalSetting(index);
            case 2 -> this.getFormDefaultSetting();
            default -> null;
        };
        if (setting != null) {
            this.loadData(setting);
        }
    }

    private void saveSlotName(int buttonType, int index) {
        if (this.isUpdateUI) {
            return;
        }
        FormColorData data = FormColorData.client();
        ResourceLocation formId = this.getPlayerForm();
        switch (buttonType) {
            case 0 -> {
                if (formId != null) data.setNameLocalFormSlot(formId, index, this.formLocalSettingTextFields.get(index).getValue());
            }
            case 1 -> data.setNameGlobalSlot(index, this.globalSettingTextFields.get(index).getValue());
            case 2 -> {
                if (formId != null) data.setNameDefaultSlot(formId, this.formDefaultSettingTextField.getValue());
            }
            default -> {
            }
        }
    }

    private String getSlotName(int buttonType, int index) {
        FormColorData data = FormColorData.client();
        ResourceLocation formId = this.getPlayerForm();
        return switch (buttonType) {
            case 0 -> formId == null ? "" : data.getNameLocalFormSlot(formId, index);
            case 1 -> data.getNameGlobalSlot(index);
            case 2 -> formId == null ? "" : data.getNameDefaultSlot(formId);
            default -> "";
        };
    }

    private void reloadAllSlotName() {
        for (int index = 0; index < this.globalSettingTextFields.size(); index++) {
            this.globalSettingTextFields.get(index).setValue(this.getSlotName(1, index));
        }
        if (this.getPlayerForm() != null) {
            this.formDefaultSettingTextField.setValue(this.getSlotName(2, 0));
            for (int index = 0; index < this.formLocalSettingTextFields.size(); index++) {
                this.formLocalSettingTextFields.get(index).setValue(this.getSlotName(0, index));
            }
        }
    }

    private void createSaveDataButtons(int buttonType, int index, int x, int y) {
        FcsButtonWidget updButtonWidget = new FcsButtonWidget(x, y, EMPTY_TEXT, button -> {
            if (button instanceof FcsButtonWidget fcsButtonWidget) {
                if (fcsButtonWidget.textureX == 15) {
                    this.loadSaveData(buttonType, index);
                } else if (fcsButtonWidget.textureX == 0) {
                    this.saveCustomColorData(buttonType, index);
                }
            }
        }, Supplier::get, 0);
        EditBox textFieldWidget = new EditBox(this.font, x + 15, y, 50, 15, EMPTY_TEXT);
        textFieldWidget.setResponder(text -> this.saveSlotName(buttonType, index));
        FcsButtonWidget deleteButtonWidget = new FcsButtonWidget(x + 65, y, EMPTY_TEXT, button -> {
            if (button instanceof FcsButtonWidget fcsButtonWidget && fcsButtonWidget.textureX == 30) {
                this.deleteSaveData(buttonType, index);
            }
        }, Supplier::get, 30);
        Pair<FcsButtonWidget, FcsButtonWidget> buttons = new Pair<>(updButtonWidget, deleteButtonWidget);
        switch (buttonType) {
            case 0 -> {
                this.formLocalSettingButtons.add(buttons);
                this.formLocalSettingTextFields.add(textFieldWidget);
            }
            case 1 -> {
                this.globalSettingButtons.add(buttons);
                this.globalSettingTextFields.add(textFieldWidget);
            }
            case 2 -> {
                this.formDefaultSettingButton = buttons;
                this.formDefaultSettingTextField = textFieldWidget;
            }
            default -> {
            }
        }
        this.addRenderableWidget(updButtonWidget);
        this.addRenderableWidget(textFieldWidget);
        this.addRenderableWidget(deleteButtonWidget);
    }
}
