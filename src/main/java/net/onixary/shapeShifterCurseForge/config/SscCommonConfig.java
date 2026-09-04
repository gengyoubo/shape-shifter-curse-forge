package net.onixary.shapeShifterCurseForge.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/** Server-authoritative gameplay options. Forge writes this to the common config. */
public final class SscCommonConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CURSED_MOON_PHASES = BUILDER
            .comment("Moon phases that count as a Cursed Moon (0-7). Default Fabric values are 1 and 5.")
            .defineList("cursed_moon.phases", List.of(1, 5),
                    value -> value instanceof Integer integer && integer >= 0 && integer <= 7);

    public static final ForgeConfigSpec.BooleanValue ALLOW_SLEEP_IN_CURSED_MOON = BUILDER
            .comment("Allow players to sleep during a Cursed Moon night.")
            .define("cursed_moon.allow_sleep", false);

    public static final ForgeConfigSpec.BooleanValue ENABLE_CURSED_MOON_TRANSFORM = BUILDER
            .comment("Allow the Cursed Moon to advance a player's form and restore it at dawn.")
            .define("cursed_moon.enable_transform", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private SscCommonConfig() {
    }

    public static int[] cursedMoonPhases() {
        return CURSED_MOON_PHASES.get().stream().mapToInt(Number::intValue).toArray();
    }
}
