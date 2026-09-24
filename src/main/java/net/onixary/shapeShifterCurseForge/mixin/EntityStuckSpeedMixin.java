package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityStuckSpeedMixin {
    @ModifyVariable(method = "makeStuckInBlock", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private Vec3 ssc$modifyStuckSpeed(Vec3 multiplier) {
        if (!((Object) this instanceof Player player)) return multiplier;
        final double[] powerMultiplier = {1.0D};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:slowdown_percent".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                powerMultiplier[0] *= FormPowerRuntime.doubleValue(power, "multiplier", 1.0D);
            }
        });
        return multiplier.scale(powerMultiplier[0]);
    }
}
