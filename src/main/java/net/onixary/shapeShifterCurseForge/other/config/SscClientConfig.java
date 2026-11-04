package net.onixary.shapeShifterCurseForge.other.config;

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

    public static final ForgeConfigSpec.IntValue ITEM_STORE_POSITION = BUILDER
            .comment("Item store HUD anchor: 1-9, from top-left to bottom-right.")
            .defineInRange("hud.item_store_position", 8, 1, 9);
    public static final ForgeConfigSpec.IntValue ITEM_STORE_OFFSET_X = BUILDER
            .defineInRange("hud.item_store_offset_x", -120, -10000, 10000);
    public static final ForgeConfigSpec.IntValue ITEM_STORE_OFFSET_Y = BUILDER
            .defineInRange("hud.item_store_offset_y", 1, -10000, 10000);

    public static final ForgeConfigSpec.BooleanValue BIG_CODEX = BUILDER
            .comment("Use the larger Fabric codex layout; requires a GUI at least 700 by 440 pixels.")
            .define("codex.big_screen", false);

    public static final ForgeConfigSpec.BooleanValue FIRST_PERSON_FORM_ARMS = BUILDER
            .define("render.first_person_form_arms", true);
    public static final ForgeConfigSpec.BooleanValue IGNORE_HIDE_ARMS = BUILDER
            .define("render.ignore_no_render_arm_power", false);
    public static final ForgeConfigSpec.IntValue BLINK_MIN_INTERVAL = BUILDER
            .defineInRange("render.blink_min_interval", 60, 1, 12000);
    public static final ForgeConfigSpec.IntValue BLINK_MAX_INTERVAL = BUILDER
            .defineInRange("render.blink_max_interval", 140, 1, 12000);
    public static final ForgeConfigSpec.IntValue BLINK_DURATION = BUILDER
            .defineInRange("render.blink_duration", 4, 1, 1200);
    public static final ForgeConfigSpec.IntValue INSTINCT_POSITION = BUILDER
            .defineInRange("hud.instinct_position", 8, 1, 9);
    public static final ForgeConfigSpec.IntValue INSTINCT_OFFSET_X = BUILDER
            .defineInRange("hud.instinct_offset_x", 100, -10000, 10000);
    public static final ForgeConfigSpec.IntValue INSTINCT_OFFSET_Y = BUILDER
            .defineInRange("hud.instinct_offset_y", -9, -10000, 10000);
    public static final ForgeConfigSpec.IntValue MANA_POSITION = BUILDER
            .defineInRange("hud.mana_position", 8, 1, 9);
    public static final ForgeConfigSpec.IntValue MANA_OFFSET_X = BUILDER
            .defineInRange("hud.mana_offset_x", 100, -10000, 10000);
    public static final ForgeConfigSpec.IntValue MANA_OFFSET_Y = BUILDER
            .defineInRange("hud.mana_offset_y", -17, -10000, 10000);

    public static final ForgeConfigSpec.BooleanValue BETTER_COMBAT_FIX = BUILDER
            .define("render.better_combat_fix", true);
    public static final ForgeConfigSpec.BooleanValue DISABLE_COLOR_UNLOCK_CHECK = BUILDER
            .define("custom.disable_color_unlock_check", false);
    public static final ForgeConfigSpec.BooleanValue FCS_USE_V1_MENU = BUILDER
            .comment("Open the professional (V1) form color menu from the codex instead of V2.")
            .define("custom.fcs_use_v1_menu", false);
    public static final ForgeConfigSpec.BooleanValue CHANGE_FPM_CONFIG = BUILDER
            .comment("Let feral forms adjust First-person Model's X offsets while that mod is loaded.")
            .define("render.enable_change_fpm_config", true);

    public static final ForgeConfigSpec.IntValue MAGIC_POSITION = BUILDER.defineInRange("hud.magic_position", 8, 1, 9);
    public static final ForgeConfigSpec.IntValue MAGIC_OFFSET_X = BUILDER.defineInRange("hud.magic_offset_x", -200, -10000, 10000);
    public static final ForgeConfigSpec.IntValue MAGIC_OFFSET_Y = BUILDER.defineInRange("hud.magic_offset_y", -35, -10000, 10000);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private SscClientConfig() {
    }
}
