package net.onixary.shapeShifterCurseForge.other.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Messages shown when SSC and Changed block each other's transformations. */
public final class SscChangedConfig {
    public static final String DEFAULT_MESSAGE = "info.shape-shifter-curse.transform_blocked";

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue SHOW_MESSAGES = BUILDER
            .comment("Show a chat message when a transformation is blocked by the other mod.")
            .define("blocked_message.enabled", true);

    public static final ForgeConfigSpec.IntValue MESSAGE_COOLDOWN_TICKS = BUILDER
            .comment("Minimum ticks between two messages of the same kind for one player (20 ticks = 1 second).",
                    "0 shows a message on every blocked attempt.")
            .defineInRange("blocked_message.cooldown_ticks", 40, 0, 72000);

    public static final ForgeConfigSpec.ConfigValue<String> SSC_FORM_BLOCKS_CHANGED = BUILDER
            .comment("Shown when an SSC form prevents a Changed transfur.",
                    "A translation key, or plain text used as-is. Leave empty to show nothing.")
            .define("blocked_message.ssc_form_blocks_changed", DEFAULT_MESSAGE);

    public static final ForgeConfigSpec.ConfigValue<String> CHANGED_VARIANT_BLOCKS_SSC = BUILDER
            .comment("Shown when a Changed variant prevents an SSC transformation.",
                    "A translation key, or plain text used as-is. Leave empty to show nothing.")
            .define("blocked_message.changed_variant_blocks_ssc", DEFAULT_MESSAGE);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private SscChangedConfig() {
    }
}
