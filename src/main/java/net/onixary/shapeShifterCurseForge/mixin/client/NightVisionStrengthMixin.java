package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.client.NightVisionPowerLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Apoli's night vision changes rendering without granting a MobEffect. */
@Mixin(GameRenderer.class)
public class NightVisionStrengthMixin {
    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    private static void ssc$nightVisionStrength(LivingEntity entity, float partialTick,
                                                 CallbackInfoReturnable<Float> cir) {
        if (entity instanceof Player player && !entity.hasEffect(MobEffects.NIGHT_VISION)) {
            float strength = NightVisionPowerLookup.strength(player);
            if (strength >= 0.0F) cir.setReturnValue(strength);
        }
    }
}
