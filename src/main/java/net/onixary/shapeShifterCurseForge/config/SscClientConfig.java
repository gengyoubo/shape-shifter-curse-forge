package net.onixary.shapeShifterCurseForge.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only presentation options. Forge writes this to config/shape-shifter-curse-client.toml. */
public final class SscClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
/*
预计要做的动画形态一共有三种
覆盖型 / Overlay。它不推翻旧 PAL 动画，而是在旧动画基础上额外叠一层 Geo 动画。
重做型 / Replacement。这个就是真正把旧 PAL 动画替换掉，重新做一套新的主体动作。
添加型 / Additional。原版 SSC 原本根本没有对应动画，但你给某个动作、技能或者特殊状态新增动画。
 */
    public static final ForgeConfigSpec.BooleanValue PREFER_NEW_ANIMATIONS = BUILDER
            .comment("Use player_animation/new/<animation>.json when present.",
                    "If the new file or its animation key is missing, SSC falls back to the old animation.")
            .define("animations.prefer_new_animations", true);

    // Local form color preferences (mirrors Fabric PlayerCustomConfig). The server
    // capability stays authoritative in game; these seed the color menu offline.
    public static final ForgeConfigSpec.BooleanValue CUSTOM_AUTO_SYNC_CONFIG = BUILDER
            .comment("Push local color preferences to the server automatically.")
            .define("custom.auto_sync_config", true);
    public static final ForgeConfigSpec.BooleanValue CUSTOM_ENABLE_SERVER_MODIFY_FCD = BUILDER
            .comment("Accept server-driven color slot save/load/delete/config commands.")
            .define("custom.enable_server_modify_fcd", true);
    public static final ForgeConfigSpec.BooleanValue CUSTOM_ENABLE_FORM_DEFAULT_COLOR_SYSTEM = BUILDER
            .comment("Auto-apply per-form default colors on form change when available.")
            .define("custom.enable_form_default_color_system", true);
    public static final ForgeConfigSpec.BooleanValue CUSTOM_AUTO_SYNC_COLOR_CONFIG = BUILDER
            .comment("Include colors when auto-syncing preferences.")
            .define("custom.auto_sync_color_config", true);
    public static final ForgeConfigSpec.BooleanValue CUSTOM_KEEP_ORIGINAL_SKIN = BUILDER
            .define("custom.keep_original_skin", false);
    public static final ForgeConfigSpec.BooleanValue CUSTOM_ENABLE_FORM_COLOR = BUILDER
            .define("custom.enable_form_color", false);
    public static final ForgeConfigSpec.IntValue CUSTOM_PRIMARY_COLOR = BUILDER
            .defineInRange("custom.primary_color", 0xFFFFFFFF, Integer.MIN_VALUE, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue CUSTOM_ACCENT_COLOR_1 = BUILDER
            .defineInRange("custom.accent_color_1", 0xFFFFFFFF, Integer.MIN_VALUE, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue CUSTOM_ACCENT_COLOR_2 = BUILDER
            .defineInRange("custom.accent_color_2", 0xFFFFFFFF, Integer.MIN_VALUE, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue CUSTOM_EYE_COLOR_A = BUILDER
            .defineInRange("custom.eye_color_a", 0xFF000000, Integer.MIN_VALUE, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue CUSTOM_EYE_COLOR_B = BUILDER
            .defineInRange("custom.eye_color_b", 0xFF000000, Integer.MIN_VALUE, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.BooleanValue CUSTOM_PRIMARY_GREY_REVERSE = BUILDER
            .define("custom.primary_grey_reverse", false);
    public static final ForgeConfigSpec.BooleanValue CUSTOM_ACCENT_1_GREY_REVERSE = BUILDER
            .define("custom.accent_1_grey_reverse", false);
    public static final ForgeConfigSpec.BooleanValue CUSTOM_ACCENT_2_GREY_REVERSE = BUILDER
            .define("custom.accent_2_grey_reverse", false);
    public static final ForgeConfigSpec.BooleanValue CUSTOM_ENABLE_FORM_RANDOM_SOUND = BUILDER
            .define("custom.enable_form_random_sound", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private SscClientConfig() {
    }
}
