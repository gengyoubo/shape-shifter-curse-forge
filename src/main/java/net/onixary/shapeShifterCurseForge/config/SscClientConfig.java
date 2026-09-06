package net.onixary.shapeShifterCurseForge.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only presentation options. Forge writes this to config/shape-shifter-curse-client.toml. */
public final class SscClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
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
