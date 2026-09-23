package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Apoli's {@code apoli:night_vision} power has a 0..1 "strength" that scales the night-vision
 * brightness, while vanilla only toggles it on/off. Scale {@link GameRenderer#getNightVisionScale}
 * by the strongest active night-vision power strength for the local player.
 */
@Mixin(GameRenderer.class)
public class NightVisionStrengthMixin {
    @Inject(method = "getNightVisionScale", at = @At("RETURN"), cancellable = true)
    private static void ssc$nightVisionStrength(LivingEntity entity, float partialTick,
                                                 CallbackInfoReturnable<Float> cir) {
        if (!(entity instanceof Player player)) {
            return;
        }
        final float[] strength = {1.0F};
        final boolean[] found = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:night_vision".equals(FormPowerRegistry.typeOf(power)) || !power.has("strength")) {
                return;
            }
            if (!FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                return;
            }
            float value = FormPowerRuntime.floatValue(power, "strength", 1.0F);
            strength[0] = found[0] ? Math.max(strength[0], value) : value;
            found[0] = true;
        });
        if (found[0]) {
            cir.setReturnValue(cir.getReturnValueF() * Math.max(0.0F, Math.min(1.0F, strength[0])));
        }
    }
}