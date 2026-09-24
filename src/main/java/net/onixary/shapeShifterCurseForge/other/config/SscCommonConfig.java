package net.onixary.shapeShifterCurseForge.other.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/** Gameplay and diagnostics options in Forge's common config. */
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

    public static final ForgeConfigSpec.BooleanValue ENABLE_MOVEMENT_DEBUG_LOGGING = BUILDER
            .comment("Log detailed player movement, swimming, and jump diagnostics on this side.")
            .define("debug.movement_logging", false);

    public static final ForgeConfigSpec.DoubleValue TRANSFORMATIVE_BAT_SPAWN_CHANCE = spawnChance("transformative_bat_spawn_chance");
    public static final ForgeConfigSpec.DoubleValue TRANSFORMATIVE_AXOLOTL_SPAWN_CHANCE = spawnChance("transformative_axolotl_spawn_chance");
    public static final ForgeConfigSpec.DoubleValue TRANSFORMATIVE_OCELOT_SPAWN_CHANCE = spawnChance("transformative_ocelot_spawn_chance");
    public static final ForgeConfigSpec.DoubleValue TRANSFORMATIVE_WOLF_SPAWN_CHANCE = spawnChance("transformative_wolf_spawn_chance");
    public static final ForgeConfigSpec.DoubleValue TRANSFORMATIVE_SPIDER_SPAWN_CHANCE = spawnChance("transformative_spider_spawn_chance");

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private SscCommonConfig() {
    }

    private static ForgeConfigSpec.DoubleValue spawnChance(String key) {
        return BUILDER.comment("Probability of a transformative mob spawning (0 disables it; 1 guarantees it).")
                .defineInRange("transformative_mobs." + key, 0.5, 0.0, 1.0);
    }

    public static int[] cursedMoonPhases() {
        return CURSED_MOON_PHASES.get().stream().mapToInt(Number::intValue).toArray();
    }
}
