package net.onixary.shapeShifterCurseForge.client;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

public final class PowerEntityGlow {
    private PowerEntityGlow() { }
    public static JsonObject matchingPower(Entity target) {
        var player = Minecraft.getInstance().player;
        if (player == null || player == target) return null;
        final JsonObject[] matched = {null};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (matched[0] == null && "apoli:entity_glow".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))
                    && FormPowerRuntime.test(player, target, power.getAsJsonObject("bientity_condition"))) matched[0] = power;
        });
        return matched[0];
    }
}
