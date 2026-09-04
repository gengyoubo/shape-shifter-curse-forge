package net.onixary.shapeShifterCurseForge.client.codex;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.client.color.FormColorData;
import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;
import net.onixary.shapeShifterCurseForge.capability.ModCapabilities;
import net.onixary.shapeShifterCurseForge.config.SscClientConfig;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.network.UpdateSkinPacket;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

// XuHaoNan:
// 需要的功能
// RGB HSV 拉条
// 全局颜色槽位的上传和下载
// 玩家模型展示框
// 自动加载/保存 颜色数据 当开启自动同步颜色时会自动把数据写入到config中 否则仅写入到服务器中 数据用服务器端的数据 如果未进入游戏 则使用客户端的数据
// 剪切板上传下载
// 二级菜单

public class FormColorSelectMenuV2 extends Screen implements FormTextureUtils.TempFormTextureProcessor {
    private static final Component BOOL_BTN_ON = Component.translatable("text.cloth-config.boolean.value.true");
    private static final Component BOOL_BTN_OFF = Component.translatable("text.cloth-config.boolean.value.false");
    private static final Component CLICK_TO_MODIFY =
            Component.translatable("gui.shape_shifter_curse_fabric.fcsv2.click_to_modify");
    private static final Component HEX_TEXT =
            Component.translatable("gui.shape_shifter_curse_fabric.fcsv2.hex_text");
    private static final Component V2_IS_ENABLE_LAYER_LABEL =
            Component.translatable("gui.shape_shifter_curse_fabric.fcsv2.is_enable_layer");

    private static final Component EMPTY_TEXT = Component.empty();
    private static final Component GLOBAL_SLOT_TITLE =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.global_slot_title");
    private static final Component DOWNLOAD_FROM_CLIPBOARD =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.from_clipboard");
    private static final Component UPLOAD_TO_CLIPBOARD =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.to_clipboard");
    private static final MutableComponent NONE_FROM_NAME_LABEL =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.none_from_name");
    private static final Component PRIMARY_COLOR_LABEL =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.primaryColor");
    private static final Component ACCENT_COLOR_1_LABEL =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.accentColor1Color");
    private static final Component ACCENT_COLOR_2_LABEL =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.accentColor2Color");
    private static final Component EYE_COLOR_A_LABEL =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.eyeColorA");
    private static final Component EYE_COLOR_B_LABEL =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.eyeColorB");
    private static final Component PRIMARY_GREY_REVERSE_LABEL =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.primaryGreyReverse");
    private static final Component ACCENT_1_GREY_REVERSE_LABEL =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.accent1GreyReverse");
    private static final Component ACCENT_2_GREY_REVERSE_LABEL =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.accent2GreyReverse");
    private static final Component KEEP_ORIGINAL_SKIN_LABEL =
            Component.translatable("text.autoconfig.shape-shifter-curse-custom.option.keep_original_skin");
    private static final Component IS_ENABLE_FORM_COLOR_SYSTEM_LABEL =
            Component.translatable("text.autoconfig.shape-shifter-curse-custom.option.enable_form_color");
    private static final Component COLOR_CHANNEL_R =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.color_channel_r");
    private static final Component COLOR_CHANNEL_G =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.color_channel_g");
    private static final Component COLOR_CHANNEL_B =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.color_channel_b");
    private static final Component COLOR_CHANNEL_H =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.color_channel_h");
    private static final Component COLOR_CHANNEL_S =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.color_channel_s");
    private static final Component COLOR_CHANNEL_V =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.color_channel_v");
    private static final Component EXIT_SLIDER_BUTTON_LABEL =
            Component.translatable("gui.shape_shifter_curse_fabric.fcs.exit_slider_button");

    private Button formNameLabel = null;

    // Data0:
    private boolean isColorSettingDirty = true;
    private FormTextureUtils.ColorSetting colorSettingArgb = null;
    private FormTextureUtils.ColorSetting colorSettingAbgr = null;

    // Data1: 由它更新Data0，修改它后需要把isColorSettingDirty设为true和触发更新Data2函数
    private int primaryColor = 0x00FFFFFF;
    private int accentColor1Color = 0x00FFFFFF;
    private int accentColor2Color = 0x00FFFFFF;
    private int eyeColorA = 0x00FFFFFF;
    private int eyeColorB = 0x00FFFFFF;
    private boolean primaryGreyReverse = false;
    private boolean accent1GreyReverse = false;
    private boolean accent2GreyReverse = false;
    private boolean keepCustomSkin = false;
    private boolean enableFormColorSystem = true;

    // Data2: 由Data1的数据更新，修改时直接修改对应int，需要flag标记防止循环调用，更新对应Data5数据
    private boolean isUpdateConfigWidget = false;
    private final boolean useSliderTextBox = true;

    private boolean isUpdateFromTextBox = false;
    private EditBox primaryColorTextBox;
    private EditBox accentColor1TextBox;
    private EditBox accentColor2TextBox;
    private EditBox eyeColorATextBox;
    private EditBox eyeColorBTextBox;

    private EditBox sliderTextBox;
    private Button primaryGreyReverseButton;
    private Button accent1GreyReverseButton;
    private Button accent2GreyReverseButton;
    private Button keepCustomSkinButton;
    private Button enableFormColorSystemButton;

    // Data3: 修改它后需要调用刷新函数，直接修改对应的TextBox，仅当flag为否时修改
    private int sliderIndex = -1;
    private boolean isUpdateSliderFormConfig = false;
    private int sliderR;
    private int sliderG;
    private int sliderB;
    private int sliderA;
    private int sliderH;
    private int sliderS;
    private int sliderV;

    // Data4: 如果有flag则仅更新Data3，否则修改对应的Slider(不会更新Data3)
    private int isUpdateSlider = 0;
    private StringWidget panelConfigNameLabel = null;
    private EditBox sliderRTextBox;
    private EditBox sliderGTextBox;
    private EditBox sliderBTextBox;
    private EditBox sliderHTextBox;
    private EditBox sliderSTextBox;
    private EditBox sliderVTextBox;
    private Button sliderAButton;

    // Data5: 修改时将Data4的flag++，更新对应的TextBox，检查自身Flag选择是否触发更新RGB HSV
    private boolean isUpdateRgbHsv = false;
    private boolean forceUpdateFormRgbHsv = false;
    private SimpleIntSliderWidget sliderRSlider;
    private SimpleIntSliderWidget sliderGSlider;
    private SimpleIntSliderWidget sliderBSlider;
    private SimpleIntSliderWidget sliderHSlider;
    private SimpleIntSliderWidget sliderSSlider;
    private SimpleIntSliderWidget sliderVSlider;

    // 二级菜单覆盖和被覆盖需要注册在list中
    private boolean isOpenPanel02 = false;
    private final List<AbstractWidget> configPanel01 = new ArrayList<>();
    private final List<AbstractWidget> configPanel02 = new ArrayList<>();
    private boolean needTogglePanel = false;
    private boolean ntpIsOpenPanel02 = false;
    private int ntpSliderIndex = -1;

    private boolean isScreenInit = false;
    private static final Minecraft minecraftClient = Minecraft.getInstance();
    private static final ResourceLocation BG_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge.RESOURCE_NAMESPACE,
            "textures/gui/v2_form_color_select_menu.png");
    private static final int BG_WIDTH = 420;
    private static final int BG_HEIGHT = 227;
    private static final int BG_IMAGE_WIDTH = 420;
    private static final int BG_IMAGE_HEIGHT = 427;
    private static final int EXTRA_PART_START_X = 0;
    private static final int EXTRA_PART_START_Y = 227;
    public static FormColorSelectMenuV2 instance;
    private boolean isLockTempTextureSystem = false;
    private @Nullable Screen parsetScreen = null;
    private final Map<String, Map<FormTextureUtils.ColorSetting, ResourceLocation>> colorSettingCacheMap = new HashMap<>();
    private String formKey = "";
    private static final String IDENTIFIER_NAMESPACE =
            net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge.RESOURCE_NAMESPACE;
    private static final String IDENTIFIER_PREFIX = "dynamic_fcs_v2_";
    private static long nowColorSettingIndex = 0;
    private int timer = 0;
    private final List<Pair<FcsButtonWidget, FcsButtonWidget>> globalSlotButton = new ArrayList<>();
    private final List<EditBox> globalSlotNameInputs = new ArrayList<>();
    private int formIDIndex = -1;

    public static String encodeColor(int color) {
        return String.format(Locale.ROOT, "#%08X", color);
    }

    public static int decodeColor(String color) {
        Integer parsed = null;
        try {
            if (color.startsWith("#")) {
                parsed = Integer.parseUnsignedInt(color.substring(1), 16);
            } else {
                parsed = Integer.parseUnsignedInt(color, 10);
            }
        } catch (Exception ignored) {
        }
        if (parsed == null) {
            return 0x00FFFFFF;
        }
        return parsed;
    }

    public static int colorChannel2Int(String channel, int min, int max) {
        try {
            int value = Integer.parseInt(channel);
            return Math.min(Math.max(value, min), max);
        } catch (Exception ignored) {
            return min;
        }
    }

    public void scrollFormID(int offset, boolean loop) {
        List<ResourceLocation> forms = new ArrayList<>(FormRegistry.forms().keySet());
        if (formIDIndex < 0) {
            formIDIndex = 0;
        }
        formIDIndex += offset;
        if (formIDIndex < 0) {
            formIDIndex = loop ? forms.size() - 1 : 0;
        } else if (formIDIndex >= forms.size()) {
            formIDIndex = loop ? 0 : forms.size() - 1;
        }
        this.reloadFormIDName();
    }

    public void reloadFormIDName() {
        ResourceLocation form = this.getFormNoCheckUnlock();
        boolean isUnlocked = FormColorData.client().isUnlock(form);
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
        if (minecraftClient.player != null) {
            boolean isFind = false;
            ResourceLocation form = FormManager.current(minecraftClient.player).id();
            if (form != null) {
                int index = 0;
                for (ResourceLocation formId : FormRegistry.forms().keySet()) {
                    if (Objects.equals(formId, form)) {
                        formIDIndex = index;
                        isFind = true;
                        break;
                    }
                    index++;
                }
            }
            if (!isFind) {
                formIDIndex = -1;
            }
            return;
        }
        formIDIndex = -1;
    }

    public ResourceLocation getFormNoCheckUnlock() {
        List<ResourceLocation> forms = new ArrayList<>(FormRegistry.forms().keySet());
        if (this.formIDIndex < 0 || this.formIDIndex >= forms.size()) {
            return FormRegistry.ORIGINAL_BEFORE_ENABLE;
        }
        return forms.get(this.formIDIndex);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void createSaveDataButtons(int index, int x, int y) {
        // X,Y,80,15
        // X+0,Y+0,15,15 upload/download Button
        FcsButtonWidget updButtonWidget = new FcsButtonWidget(x, y, EMPTY_TEXT, (button -> {
            if (button instanceof FcsButtonWidget fcsButtonWidget) {
                if (fcsButtonWidget.textureX == 15) {
                    FormTextureUtils.ColorSetting colorSetting = this.getGlobalSetting(index);
                    if (colorSetting != null) {
                        this.loadData(colorSetting);
                    }
                } else if (fcsButtonWidget.textureX == 0) {
                    this.setGlobalSetting(index);
                }
            }
        }), (textSupplier) -> (MutableComponent) textSupplier.get(), 0);

        // X+15,Y+0,40,15 slot name input
        EditBox textFieldWidget = new EditBox(this.font, x + 15, y, 40, 15, EMPTY_TEXT);
        textFieldWidget.setResponder((text) -> this.onGlobalSlotNameChanged(index));
        // X+55,Y+0,15,15 delete Button
        FcsButtonWidget deleteButtonWidget = new FcsButtonWidget(x + 55, y, EMPTY_TEXT, (button -> {
            if (button instanceof FcsButtonWidget fcsButtonWidget) {
                if (fcsButtonWidget.textureX == 30) {
                    this.removeGlobalSetting(index);
                }
            }
        }), (textSupplier) -> (MutableComponent) textSupplier.get(), 30);
        globalSlotButton.add(new Pair<>(updButtonWidget, deleteButtonWidget));
        globalSlotNameInputs.add(textFieldWidget);
        textFieldWidget.setValue(this.getGlobalSlotName(index));
        this.addRenderableWidget(updButtonWidget);
        this.addRenderableWidget(textFieldWidget);
        this.addRenderableWidget(deleteButtonWidget);
    }

    private void updateColorSetting() {
        colorSettingArgb = new FormTextureUtils.ColorSetting(
                primaryColor,
                accentColor1Color,
                accentColor2Color,
                eyeColorA,
                eyeColorB,
                primaryGreyReverse,
                accent1GreyReverse,
                accent2GreyReverse
        );
        // NOTE: eye channels intentionally follow Fabric's order here (B then A).
        colorSettingAbgr = new FormTextureUtils.ColorSetting(
                FormTextureUtils.argb2Abgr(primaryColor),
                FormTextureUtils.argb2Abgr(accentColor1Color),
                FormTextureUtils.argb2Abgr(accentColor2Color),
                FormTextureUtils.argb2Abgr(eyeColorB),
                FormTextureUtils.argb2Abgr(eyeColorA),
                primaryGreyReverse,
                accent1GreyReverse,
                accent2GreyReverse
        );
        this.isColorSettingDirty = false;
    }

    public FormTextureUtils.ColorSetting getColorSetting(boolean isAbgr) {
        if (this.isColorSettingDirty) {
            updateColorSetting();
        }
        return isAbgr ? colorSettingAbgr : colorSettingArgb;
    }

    private void onData1Changed() {
        if (!this.isScreenInit) {
            return;
        }
        this.isUpdateConfigWidget = true;
        if (!isUpdateFromTextBox) {
            if (!this.useSliderTextBox) {
                this.primaryColorTextBox.setValue(encodeColor(this.primaryColor));
                this.accentColor1TextBox.setValue(encodeColor(this.accentColor1Color));
                this.accentColor2TextBox.setValue(encodeColor(this.accentColor2Color));
                this.eyeColorATextBox.setValue(encodeColor(this.eyeColorA));
                this.eyeColorBTextBox.setValue(encodeColor(this.eyeColorB));
            } else {
                int color = 0x00FFFFFF;
                switch (this.sliderIndex) {
                    case 0 -> color = this.primaryColor;
                    case 1 -> color = this.accentColor1Color;
                    case 2 -> color = this.accentColor2Color;
                    case 3 -> color = this.eyeColorA;
                    case 4 -> color = this.eyeColorB;
                    default -> {
                    }
                }
                this.sliderTextBox.setValue(encodeColor(color));
            }
        }
        this.primaryGreyReverseButton.setMessage(this.primaryGreyReverse ? BOOL_BTN_ON : BOOL_BTN_OFF);
        this.accent1GreyReverseButton.setMessage(this.accent1GreyReverse ? BOOL_BTN_ON : BOOL_BTN_OFF);
        this.accent2GreyReverseButton.setMessage(this.accent2GreyReverse ? BOOL_BTN_ON : BOOL_BTN_OFF);
        this.keepCustomSkinButton.setMessage(this.keepCustomSkin ? BOOL_BTN_ON : BOOL_BTN_OFF);
        this.enableFormColorSystemButton.setMessage(this.enableFormColorSystem ? BOOL_BTN_ON : BOOL_BTN_OFF);
        this.isUpdateConfigWidget = false;
        int color = 0x00FFFFFF;
        switch (this.sliderIndex) {
            case 0 -> color = this.primaryColor;
            case 1 -> color = this.accentColor1Color;
            case 2 -> color = this.accentColor2Color;
            case 3 -> color = this.eyeColorA;
            case 4 -> color = this.eyeColorB;
            default -> {
            }
        }
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        this.isUpdateSliderFormConfig = true;
        this.sliderRSlider.setIntValue(red);
        this.sliderGSlider.setIntValue(green);
        this.sliderBSlider.setIntValue(blue);
        this.sliderA = alpha;
        this.sliderAButton.setMessage(this.sliderA != 0 ? BOOL_BTN_ON : BOOL_BTN_OFF);
        this.isUpdateSliderFormConfig = false;
        this.updateHSVFromRGB();
    }

    private void onData2ChangedOrClicked(int textBoxIndex) {
        if (this.isUpdateConfigWidget) {
            return;
        }
        if (!this.useSliderTextBox) {
            switch (textBoxIndex) {
                case 0 -> this.primaryColor = decodeColor(this.primaryColorTextBox.getValue());
                case 1 -> this.accentColor1Color = decodeColor(this.accentColor1TextBox.getValue());
                case 2 -> this.accentColor2Color = decodeColor(this.accentColor2TextBox.getValue());
                case 3 -> this.eyeColorA = decodeColor(this.eyeColorATextBox.getValue());
                case 4 -> this.eyeColorB = decodeColor(this.eyeColorBTextBox.getValue());
                case 5 -> this.primaryGreyReverse = !this.primaryGreyReverse;
                case 6 -> this.accent1GreyReverse = !this.accent1GreyReverse;
                case 7 -> this.accent2GreyReverse = !this.accent2GreyReverse;
                case 8 -> this.keepCustomSkin = !this.keepCustomSkin;
                case 9 -> this.enableFormColorSystem = !this.enableFormColorSystem;
                default -> {
                }
            }
        } else {
            switch (textBoxIndex) {
                case 0, 1, 2, 3, 4 -> {
                    switch (this.sliderIndex) {
                        case 0 -> this.primaryColor = decodeColor(this.sliderTextBox.getValue());
                        case 1 -> this.accentColor1Color = decodeColor(this.sliderTextBox.getValue());
                        case 2 -> this.accentColor2Color = decodeColor(this.sliderTextBox.getValue());
                        case 3 -> this.eyeColorA = decodeColor(this.sliderTextBox.getValue());
                        case 4 -> this.eyeColorB = decodeColor(this.sliderTextBox.getValue());
                        default -> {
                        }
                    }
                }
                case 5 -> this.primaryGreyReverse = !this.primaryGreyReverse;
                case 6 -> this.accent1GreyReverse = !this.accent1GreyReverse;
                case 7 -> this.accent2GreyReverse = !this.accent2GreyReverse;
                case 8 -> this.keepCustomSkin = !this.keepCustomSkin;
                case 9 -> this.enableFormColorSystem = !this.enableFormColorSystem;
                default -> {
                }
            }
        }
        this.isColorSettingDirty = true;
        isUpdateFromTextBox = true;
        this.onData1Changed();
        isUpdateFromTextBox = false;
    }

    private void onData3Changed() {
        if (this.isUpdateSliderFormConfig) {
            return;
        }
        if (this.isUpdateRgbHsv && !forceUpdateFormRgbHsv) {
            return;
        }
        int color = sliderA << 24 | sliderR << 16 | sliderG << 8 | sliderB;
        switch (this.sliderIndex) {
            case 0 -> this.primaryColor = color;
            case 1 -> this.accentColor1Color = color;
            case 2 -> this.accentColor2Color = color;
            case 3 -> this.eyeColorA = color;
            case 4 -> this.eyeColorB = color;
            default -> {
            }
        }
        this.isColorSettingDirty = true;
        this.onData1Changed();
    }

    private void onData4ChangedOrClicked(int textBoxIndex) {
        if (this.isUpdateSlider == 0) {
            switch (textBoxIndex) {
                case 0 -> this.sliderRSlider.setIntValue(colorChannel2Int(this.sliderRTextBox.getValue(), 0, 255));
                case 1 -> this.sliderGSlider.setIntValue(colorChannel2Int(this.sliderGTextBox.getValue(), 0, 255));
                case 2 -> this.sliderBSlider.setIntValue(colorChannel2Int(this.sliderBTextBox.getValue(), 0, 255));
                case 3 -> this.sliderHSlider.setIntValue(colorChannel2Int(this.sliderHTextBox.getValue(), 0, 359));
                case 4 -> this.sliderSSlider.setIntValue(colorChannel2Int(this.sliderSTextBox.getValue(), 0, 100));
                case 5 -> this.sliderVSlider.setIntValue(colorChannel2Int(this.sliderVTextBox.getValue(), 0, 100));
                case 6 -> this.sliderA = this.sliderA == 0 ? 255 : 0;
                default -> {
                }
            }
        } else {
            switch (textBoxIndex) {
                case 0 -> this.sliderR = colorChannel2Int(this.sliderRTextBox.getValue(), 0, 255);
                case 1 -> this.sliderG = colorChannel2Int(this.sliderGTextBox.getValue(), 0, 255);
                case 2 -> this.sliderB = colorChannel2Int(this.sliderBTextBox.getValue(), 0, 255);
                case 3 -> this.sliderH = colorChannel2Int(this.sliderHTextBox.getValue(), 0, 359);
                case 4 -> this.sliderS = colorChannel2Int(this.sliderSTextBox.getValue(), 0, 100);
                case 5 -> this.sliderV = colorChannel2Int(this.sliderVTextBox.getValue(), 0, 100);
                case 6 -> this.sliderA = this.sliderA == 0 ? 255 : 0;
                default -> {
                }
            }
        }
        this.onData3Changed();
        this.sliderAButton.setMessage(this.sliderA != 0 ? BOOL_BTN_ON : BOOL_BTN_OFF);
    }

    private void updateHSVFromRGB() {
        if (this.isUpdateRgbHsv || this.isUpdateSlider > 0) {
            return;
        }
        this.isUpdateRgbHsv = true;
        int[] hsv = FormTextureUtils.rgbToHsv(sliderR, sliderG, sliderB);
        this.sliderHSlider.setIntValue(hsv[0]);
        this.sliderSSlider.setIntValue(hsv[1]);
        this.sliderVSlider.setIntValue(hsv[2]);
        this.forceUpdateFormRgbHsv = true;
        this.onData3Changed();
        this.forceUpdateFormRgbHsv = false;
        this.isUpdateRgbHsv = false;
    }

    private void updateRGBFromHSV() {
        if (this.isUpdateRgbHsv || this.isUpdateSlider > 0) {
            return;
        }
        this.isUpdateRgbHsv = true;
        int[] rgb = FormTextureUtils.hsvToRgb(sliderH, sliderS, sliderV);
        this.sliderRSlider.setIntValue(rgb[0]);
        this.sliderGSlider.setIntValue(rgb[1]);
        this.sliderBSlider.setIntValue(rgb[2]);
        this.forceUpdateFormRgbHsv = true;
        this.onData3Changed();
        this.forceUpdateFormRgbHsv = false;
        this.isUpdateRgbHsv = false;
    }

    private void onData5Changed(int sliderIndex) {
        this.isUpdateSlider++;
        switch (sliderIndex) {
            case 0 -> this.sliderRTextBox.setValue(String.valueOf(this.sliderRSlider.getIntValue()));
            case 1 -> this.sliderGTextBox.setValue(String.valueOf(this.sliderGSlider.getIntValue()));
            case 2 -> this.sliderBTextBox.setValue(String.valueOf(this.sliderBSlider.getIntValue()));
            case 3 -> this.sliderHTextBox.setValue(String.valueOf(this.sliderHSlider.getIntValue()));
            case 4 -> this.sliderSTextBox.setValue(String.valueOf(this.sliderSSlider.getIntValue()));
            case 5 -> this.sliderVTextBox.setValue(String.valueOf(this.sliderVSlider.getIntValue()));
            default -> {
            }
        }
        this.isUpdateSlider--;
        if (this.isUpdateSliderFormConfig) {
            return;
        }
        switch (sliderIndex) {
            case 0, 1, 2 -> this.updateHSVFromRGB();
            case 3, 4, 5 -> this.updateRGBFromHSV();
            default -> {
            }
        }
    }

    public void updatePanel() {
        if (isOpenPanel02) {
            configPanel01.forEach(element -> element.visible = false);
            configPanel02.forEach(element -> element.visible = true);
        } else {
            configPanel01.forEach(element -> element.visible = true);
            configPanel02.forEach(element -> element.visible = false);
        }
        this.onData1Changed();
    }

    public void openPanel(int index) {
        this.needTogglePanel = true;
        this.ntpIsOpenPanel02 = true;
        this.ntpSliderIndex = index;
    }

    public void closePanel() {
        this.needTogglePanel = true;
        this.ntpIsOpenPanel02 = false;
        this.ntpSliderIndex = -1;
    }

    public void realOpenClosePanel() {
        if (!this.needTogglePanel) {
            return;
        }
        this.isOpenPanel02 = this.ntpIsOpenPanel02;
        this.sliderIndex = this.ntpSliderIndex;
        if (!isOpenPanel02) {
            this.onData3Changed();
        }
        this.needTogglePanel = false;
        this.ntpIsOpenPanel02 = false;
        this.ntpSliderIndex = -1;
        this.updatePanel();
    }

    private String getGlobalSlotName(int index) {
        return FormColorData.client().v2GetNameGlobalSlot(index);
    }

    private void onGlobalSlotNameChanged(int slotIndex) {
        FormColorData.client().v2SetNameGlobalSlot(slotIndex, this.globalSlotNameInputs.get(slotIndex).getValue());
    }

    private boolean isGlobalSettingExists(int index) {
        String id = String.format("fcs_v2_%s", index);
        return FormColorData.client().customSetting.containsKey(id);
    }

    @Nullable
    private FormTextureUtils.ColorSetting getGlobalSetting(int index) {
        String id = String.format("fcs_v2_%s", index);
        return FormColorData.client().customSetting.get(id);
    }

    private void setGlobalSetting(int index) {
        String id = String.format("fcs_v2_%s", index);
        FormTextureUtils.ColorSetting colorSettingRgba = this.getColorSetting(false);
        FormColorData.client().customSetting.put(id, colorSettingRgba);
        this.updateSavaButtonActive();
    }

    private void removeGlobalSetting(int index) {
        String id = String.format("fcs_v2_%s", index);
        FormColorData.client().customSetting.remove(id);
        this.updateSavaButtonActive();
    }

    private void updateSavaButtonActive() {
        if (!this.isScreenInit) {
            return;
        }
        for (int index = 0; index < this.globalSlotNameInputs.size(); index++) {
            boolean dataExist = this.isGlobalSettingExists(index);
            Pair<FcsButtonWidget, FcsButtonWidget> buttonWidget = globalSlotButton.get(index);
            FcsButtonWidget deleteButtonWidget = buttonWidget.getSecond();
            deleteButtonWidget.active = dataExist;
            FcsButtonWidget updButtonWidget = buttonWidget.getFirst();
            updButtonWidget.active = true;
            updButtonWidget.textureX = dataExist ? 15 : 0;
        }
    }

    public FormColorSelectMenuV2(Component title, @Nullable Screen parsetScreen) {
        this(title);
        this.parsetScreen = parsetScreen;
    }

    public void loadData(FormTextureUtils.ColorSetting colorSetting) {
        primaryColor = colorSetting.primaryColor();
        accentColor1Color = colorSetting.accentColor1();
        accentColor2Color = colorSetting.accentColor2();
        eyeColorA = colorSetting.eyeColorA();
        eyeColorB = colorSetting.eyeColorB();
        primaryGreyReverse = colorSetting.primaryGreyReverse();
        accent1GreyReverse = colorSetting.accent1GreyReverse();
        accent2GreyReverse = colorSetting.accent2GreyReverse();
        isColorSettingDirty = true;
        this.onData1Changed();
    }

    public void loadServerData(FormTextureUtils.ColorSetting colorSetting) {
        primaryColor = FormTextureUtils.abgr2Argb(colorSetting.primaryColor());
        accentColor1Color = FormTextureUtils.abgr2Argb(colorSetting.accentColor1());
        accentColor2Color = FormTextureUtils.abgr2Argb(colorSetting.accentColor2());
        eyeColorA = FormTextureUtils.abgr2Argb(colorSetting.eyeColorA());
        eyeColorB = FormTextureUtils.abgr2Argb(colorSetting.eyeColorB());
        primaryGreyReverse = colorSetting.primaryGreyReverse();
        accent1GreyReverse = colorSetting.accent1GreyReverse();
        accent2GreyReverse = colorSetting.accent2GreyReverse();
        isColorSettingDirty = true;
        this.onData1Changed();
    }

    public void loadData() {
        if (minecraftClient.player != null) {
            var skin = minecraftClient.player.getCapability(ModCapabilities.PLAYER_SKIN).orElse(null);
            if (skin != null) {
                FormTextureUtils.ColorSetting colorSetting = skin.getFormColor();
                this.keepCustomSkin = skin.isKeepOriginalSkin();
                this.enableFormColorSystem = skin.isEnableFormColor();
                this.loadServerData(colorSetting);
            } else {
                this.onData1Changed();
            }
        } else {
            primaryColor = SscClientConfig.CUSTOM_PRIMARY_COLOR.get();
            accentColor1Color = SscClientConfig.CUSTOM_ACCENT_COLOR_1.get();
            accentColor2Color = SscClientConfig.CUSTOM_ACCENT_COLOR_2.get();
            eyeColorA = SscClientConfig.CUSTOM_EYE_COLOR_A.get();
            eyeColorB = SscClientConfig.CUSTOM_EYE_COLOR_B.get();
            primaryGreyReverse = SscClientConfig.CUSTOM_PRIMARY_GREY_REVERSE.get();
            accent1GreyReverse = SscClientConfig.CUSTOM_ACCENT_1_GREY_REVERSE.get();
            accent2GreyReverse = SscClientConfig.CUSTOM_ACCENT_2_GREY_REVERSE.get();
            this.keepCustomSkin = SscClientConfig.CUSTOM_KEEP_ORIGINAL_SKIN.get();
            this.enableFormColorSystem = SscClientConfig.CUSTOM_ENABLE_FORM_COLOR.get();
            this.onData1Changed();
        }
        isColorSettingDirty = true;
    }

    public FormColorSelectMenuV2(Component title) {
        super(title);
        this.reloadFormIDIndex();
        loadData();
        if (!FormTextureUtils.useTempFormTexture) {
            FormTextureUtils.useTempFormTexture = true;
            FormTextureUtils.tempFormTextureProcessor = this;
            isLockTempTextureSystem = true;
        } else {
            com.mojang.logging.LogUtils.getLogger().warn("Temp Texture System is already in use, dynamic texture rendering will not work");
        }
        if (instance != null) {
            com.mojang.logging.LogUtils.getLogger().error("FormColorSelectMenu is already in use, only one instance is allowed");
        }
        instance = this;
    }

    @Override
    protected void init() {
        super.init();
        int bPosX = this.width / 2 - BG_WIDTH / 2;
        int bPosY = this.height / 2 - BG_HEIGHT / 2;
        int textColor = 0xDDDDDD;
        this.addRenderableWidget(new StringWidget(bPosX + 330, bPosY + 20, 70, 11,
                GLOBAL_SLOT_TITLE, this.font).setColor(textColor));
        this.addRenderableWidget(Button.builder(DOWNLOAD_FROM_CLIPBOARD, button -> {
            String clipboardData = minecraftClient.keyboardHandler.getClipboard();
            FormTextureUtils.ColorSetting setting = FormColorData.colorSettingFromString(clipboardData);
            if (setting != null) {
                this.loadData(setting);
            }
        }).pos(bPosX + 20, bPosY + 196).size(68, 11).build());
        this.addRenderableWidget(Button.builder(UPLOAD_TO_CLIPBOARD, button -> {
            String clipboardData = FormColorData.colorSettingToString(this.getColorSetting(false), true);
            if (clipboardData == null) {
                return;
            }
            minecraftClient.keyboardHandler.setClipboard(clipboardData);
        }).pos(bPosX + 94, bPosY + 196).size(68, 11).build());
        Button formScrollButton = Button.builder(NONE_FROM_NAME_LABEL, button -> {
            this.reloadFormIDIndex();
            this.reloadFormIDName();
        }).pos(bPosX + 48, bPosY + 20).size(86, 11).build();
        this.addRenderableWidget(formScrollButton);
        this.formNameLabel = formScrollButton;
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            this.scrollFormID(-1, true);
        }).pos(bPosX + 31, bPosY + 20).size(11, 11).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            this.scrollFormID(1, true);
        }).pos(bPosX + 140, bPosY + 20).size(11, 11).build());
        this.reloadFormIDName();
        StringWidget primaryColorLabel = new StringWidget(bPosX + 192, bPosY + 39, 68, 11,
                PRIMARY_COLOR_LABEL, this.font).setColor(textColor);
        this.addRenderableWidget(primaryColorLabel);
        this.configPanel01.add(primaryColorLabel);
        ButtonWidgetOKey primaryColorButton = new ButtonWidgetOKey(bPosX + 270, bPosY + 39, 45, 11,
                CLICK_TO_MODIFY, button -> this.openPanel(0), ButtonWidgetOKey.DEFAULT_NARRATION_SUPPLIER);
        primaryColorButton.canClick = ButtonWidgetOKey.LEFT_CLICK;
        this.addRenderableWidget(primaryColorButton);
        this.configPanel01.add(primaryColorButton);
        StringWidget accentColor1Label = new StringWidget(bPosX + 192, bPosY + 54, 68, 11,
                ACCENT_COLOR_1_LABEL, this.font).setColor(textColor);
        this.addRenderableWidget(accentColor1Label);
        this.configPanel01.add(accentColor1Label);
        ButtonWidgetOKey accentColor1Button = new ButtonWidgetOKey(bPosX + 270, bPosY + 54, 45, 11,
                CLICK_TO_MODIFY, button -> this.openPanel(1), ButtonWidgetOKey.DEFAULT_NARRATION_SUPPLIER);
        accentColor1Button.canClick = ButtonWidgetOKey.LEFT_CLICK;
        this.addRenderableWidget(accentColor1Button);
        this.configPanel01.add(accentColor1Button);
        StringWidget accentColor2Label = new StringWidget(bPosX + 192, bPosY + 69, 68, 11,
                ACCENT_COLOR_2_LABEL, this.font).setColor(textColor);
        this.addRenderableWidget(accentColor2Label);
        this.configPanel01.add(accentColor2Label);
        ButtonWidgetOKey accentColor2Button = new ButtonWidgetOKey(bPosX + 270, bPosY + 69, 45, 11,
                CLICK_TO_MODIFY, button -> this.openPanel(2), ButtonWidgetOKey.DEFAULT_NARRATION_SUPPLIER);
        accentColor2Button.canClick = ButtonWidgetOKey.LEFT_CLICK;
        this.addRenderableWidget(accentColor2Button);
        this.configPanel01.add(accentColor2Button);
        StringWidget eyeColorALabel = new StringWidget(bPosX + 192, bPosY + 84, 68, 11,
                EYE_COLOR_A_LABEL, this.font).setColor(textColor);
        this.addRenderableWidget(eyeColorALabel);
        this.configPanel01.add(eyeColorALabel);
        ButtonWidgetOKey eyeColorAButton = new ButtonWidgetOKey(bPosX + 270, bPosY + 84, 45, 11,
                CLICK_TO_MODIFY, button -> this.openPanel(3), ButtonWidgetOKey.DEFAULT_NARRATION_SUPPLIER);
        eyeColorAButton.canClick = ButtonWidgetOKey.LEFT_CLICK;
        this.addRenderableWidget(eyeColorAButton);
        this.configPanel01.add(eyeColorAButton);
        StringWidget eyeColorBLabel = new StringWidget(bPosX + 192, bPosY + 99, 68, 11,
                EYE_COLOR_B_LABEL, this.font).setColor(textColor);
        this.addRenderableWidget(eyeColorBLabel);
        this.configPanel01.add(eyeColorBLabel);
        ButtonWidgetOKey eyeColorBButton = new ButtonWidgetOKey(bPosX + 270, bPosY + 99, 45, 11,
                CLICK_TO_MODIFY, button -> this.openPanel(4), ButtonWidgetOKey.DEFAULT_NARRATION_SUPPLIER);
        eyeColorBButton.canClick = ButtonWidgetOKey.LEFT_CLICK;
        this.addRenderableWidget(eyeColorBButton);
        this.configPanel01.add(eyeColorBButton);
        StringWidget primaryGreyReverseLabel = new StringWidget(bPosX + 177, bPosY + 114, 101, 12,
                PRIMARY_GREY_REVERSE_LABEL, this.font).setColor(textColor);
        this.addRenderableWidget(primaryGreyReverseLabel);
        this.configPanel01.add(primaryGreyReverseLabel);
        Button primaryGreyReverseButton = Button.builder(this.primaryGreyReverse ? BOOL_BTN_ON : BOOL_BTN_OFF,
                button -> this.onData2ChangedOrClicked(5)).pos(bPosX + 288, bPosY + 114).size(27, 12).build();
        this.addRenderableWidget(primaryGreyReverseButton);
        this.primaryGreyReverseButton = primaryGreyReverseButton;
        this.configPanel01.add(primaryGreyReverseButton);
        StringWidget accent1GreyReverseLabel = new StringWidget(bPosX + 177, bPosY + 130, 101, 12,
                ACCENT_1_GREY_REVERSE_LABEL, this.font).setColor(textColor);
        this.addRenderableWidget(accent1GreyReverseLabel);
        this.configPanel01.add(accent1GreyReverseLabel);
        Button accent1GreyReverseButton = Button.builder(this.accent1GreyReverse ? BOOL_BTN_ON : BOOL_BTN_OFF,
                button -> this.onData2ChangedOrClicked(6)).pos(bPosX + 288, bPosY + 130).size(27, 12).build();
        this.addRenderableWidget(accent1GreyReverseButton);
        this.accent1GreyReverseButton = accent1GreyReverseButton;
        this.configPanel01.add(accent1GreyReverseButton);
        StringWidget accent2GreyReverseLabel = new StringWidget(bPosX + 177, bPosY + 146, 101, 12,
                ACCENT_2_GREY_REVERSE_LABEL, this.font).setColor(textColor);
        this.addRenderableWidget(accent2GreyReverseLabel);
        this.configPanel01.add(accent2GreyReverseLabel);
        Button accent2GreyReverseButton = Button.builder(this.accent2GreyReverse ? BOOL_BTN_ON : BOOL_BTN_OFF,
                button -> this.onData2ChangedOrClicked(7)).pos(bPosX + 288, bPosY + 146).size(27, 12).build();
        this.addRenderableWidget(accent2GreyReverseButton);
        this.accent2GreyReverseButton = accent2GreyReverseButton;
        this.configPanel01.add(accent2GreyReverseButton);
        StringWidget keepOriginalSkinLabel = new StringWidget(bPosX + 177, bPosY + 176, 101, 11,
                KEEP_ORIGINAL_SKIN_LABEL, this.font).setColor(textColor);
        this.addRenderableWidget(keepOriginalSkinLabel);
        this.configPanel01.add(keepOriginalSkinLabel);
        Button keepOriginalSkinButton = Button.builder(this.keepCustomSkin ? BOOL_BTN_ON : BOOL_BTN_OFF,
                button -> this.onData2ChangedOrClicked(8)).pos(bPosX + 288, bPosY + 176).size(27, 11).build();
        this.addRenderableWidget(keepOriginalSkinButton);
        this.keepCustomSkinButton = keepOriginalSkinButton;
        this.configPanel01.add(keepOriginalSkinButton);
        StringWidget enableFormColorLabel = new StringWidget(bPosX + 177, bPosY + 191, 101, 11,
                IS_ENABLE_FORM_COLOR_SYSTEM_LABEL, this.font).setColor(textColor);
        this.addRenderableWidget(enableFormColorLabel);
        this.configPanel01.add(enableFormColorLabel);
        Button enableFormColorButton = Button.builder(this.enableFormColorSystem ? BOOL_BTN_ON : BOOL_BTN_OFF,
                button -> this.onData2ChangedOrClicked(9)).pos(bPosX + 288, bPosY + 191).size(27, 11).build();
        this.addRenderableWidget(enableFormColorButton);
        this.enableFormColorSystemButton = enableFormColorButton;
        this.configPanel01.add(enableFormColorButton);
        StringWidget configLabel = new StringWidget(bPosX + 177, bPosY + 68, 41, 11,
                HEX_TEXT, this.font).setColor(textColor);
        this.addRenderableWidget(configLabel);
        this.panelConfigNameLabel = configLabel;
        this.configPanel02.add(configLabel);
        EditBox configInput = new EditBox(this.font, bPosX + 222, bPosY + 68, 93, 11, EMPTY_TEXT);
        configInput.setMaxLength(9);
        configInput.setResponder(text -> this.onData2ChangedOrClicked(0));
        this.addRenderableWidget(configInput);
        this.sliderTextBox = configInput;
        this.configPanel02.add(configInput);
        StringWidget rLabel = new StringWidget(bPosX + 177, bPosY + 83, 11, 11,
                COLOR_CHANNEL_R, this.font);
        this.addRenderableWidget(rLabel);
        this.configPanel02.add(rLabel);
        StringWidget gLabel = new StringWidget(bPosX + 177, bPosY + 98, 11, 11,
                COLOR_CHANNEL_G, this.font);
        this.addRenderableWidget(gLabel);
        this.configPanel02.add(gLabel);
        StringWidget bLabel = new StringWidget(bPosX + 177, bPosY + 113, 11, 11,
                COLOR_CHANNEL_B, this.font);
        this.addRenderableWidget(bLabel);
        this.configPanel02.add(bLabel);
        StringWidget hLabel = new StringWidget(bPosX + 177, bPosY + 128, 11, 11,
                COLOR_CHANNEL_H, this.font);
        this.addRenderableWidget(hLabel);
        this.configPanel02.add(hLabel);
        StringWidget sLabel = new StringWidget(bPosX + 177, bPosY + 143, 11, 11,
                COLOR_CHANNEL_S, this.font);
        this.addRenderableWidget(sLabel);
        this.configPanel02.add(sLabel);
        StringWidget vLabel = new StringWidget(bPosX + 177, bPosY + 158, 11, 11,
                COLOR_CHANNEL_V, this.font);
        this.addRenderableWidget(vLabel);
        this.configPanel02.add(vLabel);
        EditBox rInput = new EditBox(this.font, bPosX + 192, bPosY + 83, 26, 11, EMPTY_TEXT);
        rInput.setMaxLength(3);
        rInput.setResponder(text -> this.onData4ChangedOrClicked(0));
        this.addRenderableWidget(rInput);
        this.sliderRTextBox = rInput;
        this.configPanel02.add(rInput);
        EditBox gInput = new EditBox(this.font, bPosX + 192, bPosY + 98, 26, 11, EMPTY_TEXT);
        gInput.setMaxLength(3);
        gInput.setResponder(text -> this.onData4ChangedOrClicked(1));
        this.addRenderableWidget(gInput);
        this.sliderGTextBox = gInput;
        this.configPanel02.add(gInput);
        EditBox bInput = new EditBox(this.font, bPosX + 192, bPosY + 113, 26, 11, EMPTY_TEXT);
        bInput.setMaxLength(3);
        bInput.setResponder(text -> this.onData4ChangedOrClicked(2));
        this.addRenderableWidget(bInput);
        this.sliderBTextBox = bInput;
        this.configPanel02.add(bInput);
        EditBox hInput = new EditBox(this.font, bPosX + 192, bPosY + 128, 26, 11, EMPTY_TEXT);
        hInput.setMaxLength(3);
        hInput.setResponder(text -> this.onData4ChangedOrClicked(3));
        this.addRenderableWidget(hInput);
        this.sliderHTextBox = hInput;
        this.configPanel02.add(hInput);
        EditBox sInput = new EditBox(this.font, bPosX + 192, bPosY + 143, 26, 11, EMPTY_TEXT);
        sInput.setMaxLength(3);
        sInput.setResponder(text -> this.onData4ChangedOrClicked(4));
        this.addRenderableWidget(sInput);
        this.sliderSTextBox = sInput;
        this.configPanel02.add(sInput);
        EditBox vInput = new EditBox(this.font, bPosX + 192, bPosY + 158, 26, 11, EMPTY_TEXT);
        vInput.setMaxLength(3);
        vInput.setResponder(text -> this.onData4ChangedOrClicked(5));
        this.addRenderableWidget(vInput);
        this.sliderVTextBox = vInput;
        this.configPanel02.add(vInput);
        SimpleIntSliderWidget rSlider = new SimpleIntSliderWidget(bPosX + 222, bPosY + 85, 93, 7,
                EMPTY_TEXT, 0.0D, 0, 255);
        rSlider.onChanged = value -> this.onData5Changed(0);
        this.addRenderableWidget(rSlider);
        this.sliderRSlider = rSlider;
        this.configPanel02.add(rSlider);
        SimpleIntSliderWidget gSlider = new SimpleIntSliderWidget(bPosX + 222, bPosY + 100, 93, 7,
                EMPTY_TEXT, 0.0D, 0, 255);
        gSlider.onChanged = value -> this.onData5Changed(1);
        this.addRenderableWidget(gSlider);
        this.sliderGSlider = gSlider;
        this.configPanel02.add(gSlider);
        SimpleIntSliderWidget bSlider = new SimpleIntSliderWidget(bPosX + 222, bPosY + 115, 93, 7,
                EMPTY_TEXT, 0.0D, 0, 255);
        bSlider.onChanged = value -> this.onData5Changed(2);
        this.addRenderableWidget(bSlider);
        this.sliderBSlider = bSlider;
        this.configPanel02.add(bSlider);
        SimpleIntSliderWidget hSlider = new SimpleIntSliderWidget(bPosX + 222, bPosY + 130, 93, 7,
                EMPTY_TEXT, 0.0D, 0, 359);
        hSlider.onChanged = value -> this.onData5Changed(3);
        this.addRenderableWidget(hSlider);
        this.sliderHSlider = hSlider;
        this.configPanel02.add(hSlider);
        SimpleIntSliderWidget sSlider = new SimpleIntSliderWidget(bPosX + 222, bPosY + 145, 93, 7,
                EMPTY_TEXT, 0.0D, 0, 100);
        sSlider.onChanged = value -> this.onData5Changed(4);
        this.addRenderableWidget(sSlider);
        this.sliderSSlider = sSlider;
        this.configPanel02.add(sSlider);
        SimpleIntSliderWidget vSlider = new SimpleIntSliderWidget(bPosX + 222, bPosY + 160, 93, 7,
                EMPTY_TEXT, 0.0D, 0, 100);
        vSlider.onChanged = value -> this.onData5Changed(5);
        this.addRenderableWidget(vSlider);
        this.sliderVSlider = vSlider;
        this.configPanel02.add(vSlider);
        StringWidget isEnableLayerLabel = new StringWidget(bPosX + 177, bPosY + 174, 101, 12,
                V2_IS_ENABLE_LAYER_LABEL, this.font).setColor(textColor);
        this.addRenderableWidget(isEnableLayerLabel);
        this.configPanel02.add(isEnableLayerLabel);
        Button isEnableLayerButton = Button.builder(this.sliderA != 0 ? BOOL_BTN_ON : BOOL_BTN_OFF,
                button -> this.onData4ChangedOrClicked(6)).pos(bPosX + 288, bPosY + 174).size(27, 12).build();
        this.addRenderableWidget(isEnableLayerButton);
        this.sliderAButton = isEnableLayerButton;
        this.configPanel02.add(isEnableLayerButton);
        Button exitSliderButton = Button.builder(EXIT_SLIDER_BUTTON_LABEL,
                button -> this.closePanel()).pos(bPosX + 255, bPosY + 190).size(60, 12).build();
        this.addRenderableWidget(exitSliderButton);
        this.configPanel02.add(exitSliderButton);

        this.globalSlotButton.clear();
        this.globalSlotNameInputs.clear();
        for (int index = 0; index < FormColorData.v2GlobalSlotCount; index++) {
            this.createSaveDataButtons(index, bPosX + 330, bPosY + 37 + index * 19);
        }

        this.isScreenInit = true;
        this.updatePanel();
    }

    private void drawExtraPart(GuiGraphics graphics, int x, int y, int partX, int partY, int width, int height) {
        int realX = partX + EXTRA_PART_START_X;
        int realY = partY + EXTRA_PART_START_Y;
        graphics.blit(BG_TEXTURE, x, y, realX, realY, width, height, BG_IMAGE_WIDTH, BG_IMAGE_HEIGHT);
    }

    public void renderTextureBackground(GuiGraphics graphics) {
        int bgX = this.width / 2 - BG_WIDTH / 2;
        int bgY = this.height / 2 - BG_HEIGHT / 2;
        graphics.blit(BG_TEXTURE, bgX, bgY, 0, 0, BG_WIDTH, BG_HEIGHT, BG_IMAGE_WIDTH, BG_IMAGE_HEIGHT);
        if (!isOpenPanel02) {
            this.drawExtraPart(graphics, bgX + 172, bgY + 34, 0, 0, 148, 173);
        } else {
            this.drawExtraPart(graphics, bgX + 172, bgY + 34, 149, 0, 148, 173);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bPosX = this.width / 2 - BG_WIDTH / 2;
        int bPosY = this.height / 2 - BG_HEIGHT / 2;
        this.renderBackground(graphics);
        this.renderTextureBackground(graphics);

        if (!isOpenPanel02) {
            graphics.fill(bPosX + 177, bPosY + 39, bPosX + 188, bPosY + 50, this.primaryColor);
            graphics.fill(bPosX + 177, bPosY + 54, bPosX + 188, bPosY + 65, this.accentColor1Color);
            graphics.fill(bPosX + 177, bPosY + 69, bPosX + 188, bPosY + 80, this.accentColor2Color);
            graphics.fill(bPosX + 177, bPosY + 84, bPosX + 188, bPosY + 95, this.eyeColorA);
            graphics.fill(bPosX + 177, bPosY + 99, bPosX + 188, bPosY + 110, this.eyeColorB);
        } else {
            graphics.fill(bPosX + 177, bPosY + 39, bPosX + 201, bPosY + 64,
                    (this.sliderA << 24) | (this.sliderR << 16) | (this.sliderG << 8) | this.sliderB);
        }
        if (timer > 60) {
            this.updateSavaButtonActive();
        } else {
            timer++;
        }
        if (minecraftClient.player != null) {
            int viewportX = bPosX + 20;
            int viewportY = bPosY + 34;
            graphics.enableScissor(viewportX, viewportY, viewportX + 142, viewportY + 157);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, bPosX + 91, bPosY + 226, 90,
                    (float) (bPosX + 91 - mouseX), (float) (bPosY + 96 - mouseY), minecraftClient.player);
            graphics.disableScissor();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public void saveDataToClient(boolean saveColorData, boolean saveExtraData) {
        if (saveColorData) {
            SscClientConfig.CUSTOM_PRIMARY_COLOR.set(primaryColor);
            SscClientConfig.CUSTOM_ACCENT_COLOR_1.set(accentColor1Color);
            SscClientConfig.CUSTOM_ACCENT_COLOR_2.set(accentColor2Color);
            SscClientConfig.CUSTOM_EYE_COLOR_A.set(eyeColorA);
            SscClientConfig.CUSTOM_EYE_COLOR_B.set(eyeColorB);
            SscClientConfig.CUSTOM_PRIMARY_GREY_REVERSE.set(primaryGreyReverse);
            SscClientConfig.CUSTOM_ACCENT_1_GREY_REVERSE.set(accent1GreyReverse);
            SscClientConfig.CUSTOM_ACCENT_2_GREY_REVERSE.set(accent2GreyReverse);
        }
        if (saveExtraData) {
            SscClientConfig.CUSTOM_KEEP_ORIGINAL_SKIN.set(keepCustomSkin);
            SscClientConfig.CUSTOM_ENABLE_FORM_COLOR.set(enableFormColorSystem);
        }
        SscClientConfig.SPEC.save();
    }

    @Override
    public void onClose() {
        this.cleanColorSettingCache();
        if (this.isLockTempTextureSystem) {
            FormTextureUtils.useTempFormTexture = false;
            FormTextureUtils.tempFormTextureProcessor = null;
            isLockTempTextureSystem = false;
        }
        instance = null;
        try {
            FormTextureUtils.ColorSetting setting = this.getColorSetting(true);
            ModNetwork.CHANNEL.sendToServer(new UpdateSkinPacket(true, this.keepCustomSkin,
                    this.enableFormColorSystem, setting.primaryColor(), setting.accentColor1(),
                    setting.accentColor2(), setting.eyeColorA(), setting.eyeColorB(),
                    setting.primaryGreyReverse(), setting.accent1GreyReverse(), setting.accent2GreyReverse()));
        } catch (Exception ignored) {
        }
        this.saveDataToClient(true, true);
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
        if (!this.isOpenPanel02 && this.isScreenInit) {
            if (mouseX > bPosX + 177 && mouseX < bPosX + 188 && mouseY > bPosY + 39 && mouseY < bPosY + 50) {
                this.openPanel(0);
                result = true;
            } else if (mouseX > bPosX + 177 && mouseX < bPosX + 188 && mouseY > bPosY + 54 && mouseY < bPosY + 65) {
                this.openPanel(1);
                result = true;
            } else if (mouseX > bPosX + 177 && mouseX < bPosX + 188 && mouseY > bPosY + 69 && mouseY < bPosY + 80) {
                this.openPanel(2);
                result = true;
            } else if (mouseX > bPosX + 177 && mouseX < bPosX + 188 && mouseY > bPosY + 84 && mouseY < bPosY + 95) {
                this.openPanel(3);
                result = true;
            } else if (mouseX > bPosX + 177 && mouseX < bPosX + 188 && mouseY > bPosY + 99 && mouseY < bPosY + 110) {
                this.openPanel(4);
                result = true;
            }
        }
        if (needTogglePanel) {
            this.realOpenClosePanel();
        }
        return result;
    }

    private void cleanColorSettingCache() {
        for (ResourceLocation id : colorSettingCacheMap.values().stream()
                .flatMap(map -> map.values().stream()).toList()) {
            FormTextureUtils.releaseTexture(id);
        }
        colorSettingCacheMap.clear();
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
                colorSettingCacheMap.computeIfAbsent(formKey, key -> new HashMap<>());
        FormTextureUtils.ColorSetting setting = this.getColorSetting(true);
        ResourceLocation id = cache.get(setting);
        if (id == null) {
            com.mojang.blaze3d.platform.NativeImage image =
                    FormTextureUtils.bakeTextureImage(texture, mask, setting, onlyMultiply);
            if (image == null) {
                return texture;
            }
            id = FormTextureUtils.registerBakedTexture(image, "fcs_v2_");
            cache.put(setting, id);
        }
        return id;
    }
}
