package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormPowerEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * apoli:prevent_sprinting (Ocelot 2/3, Spider 1). Apoli fails the client's "can sprint" check
 * in ClientPlayerEntity#tickMovement, so sprinting never starts. Resetting the flag once per
 * tick is not enough on Forge: LivingTickEvent fires before aiStep, which starts the sprint
 * again from the held key and moves the player at sprint speed in the same tick.
 */
@Mixin(LivingEntity.class)
public abstract class PreventSprintingMixin {
    @ModifyVariable(method = "setSprinting", at = @At("HEAD"), argsOnly = true)
    private boolean ssc$preventSprinting(boolean sprinting) {
        if (sprinting && (Object) this instanceof Player player && FormPowerEvents.preventsSprinting(player)) {
            return false;
        }
        return sprinting;
    }
}
