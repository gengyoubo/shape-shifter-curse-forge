package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Fabric-compatible values for perk powers consumed by existing game mechanics. */
public final class PerkPowerService {
    private PerkPowerService() { }

    public static int cocoonLootChance(Player player) {
        final int[] chance = {-1};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:cocoon_loot_chance".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                chance[0] = Math.max(chance[0], Mth.clamp(FormPowerRuntime.intValue(power, "chance", 40), 0, 100));
            }
        });
        return chance[0] < 0 ? 40 : chance[0];
    }

    public static boolean shouldApplyWither(Player player, int duration, int amplifier, boolean vanilla) {
        final double[] multiplier = {Double.NEGATIVE_INFINITY};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:wither_interval".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                multiplier[0] = Math.max(multiplier[0], FormPowerRuntime.doubleValue(power, "multiplier", 1.0));
            }
        });
        if (multiplier[0] == Double.NEGATIVE_INFINITY) return vanilla;
        int interval = Math.max(1, (int) Math.floor(Math.scalb(40.0 * multiplier[0], -Math.max(0, amplifier))));
        return duration % interval == 0;
    }
}
