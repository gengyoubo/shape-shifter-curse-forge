package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

public final class ClientPowerEffects {
    private ClientPowerEffects() {}
    public static boolean has(Player player, String type) {
        if (player == null) return false;
        final boolean[] active = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (type.equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) active[0] = true;
        });
        return active[0];
    }
}
