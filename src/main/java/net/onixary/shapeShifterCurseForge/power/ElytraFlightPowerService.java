package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.world.entity.player.Player;

/** Shared lookup for the data-defined Apoli elytra_flight power. */
public final class ElytraFlightPowerService {
    private ElytraFlightPowerService() { }

    public static boolean hasFlight(Player player) {
        final boolean[] found = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!found[0] && "apoli:elytra_flight".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                found[0] = true;
            }
        });
        return found[0];
    }

    public static boolean rendersVirtualElytra(Player player) {
        final boolean[] renders = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!renders[0] && "apoli:elytra_flight".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.booleanValue(power, "render_elytra", false)
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                renders[0] = true;
            }
        });
        return renders[0];
    }
}
