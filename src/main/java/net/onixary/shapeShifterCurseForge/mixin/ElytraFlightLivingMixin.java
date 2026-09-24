package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.ElytraFlightPowerService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla checks the chest item every tick; retain gliding while elytra_flight is active. */
@Mixin(LivingEntity.class)
public abstract class ElytraFlightLivingMixin {
    @Unique private boolean ssc$wasPowerFlying;

    @Inject(method = "updateFallFlying", at = @At("HEAD"))
    private void ssc$rememberPowerFlight(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ssc$wasPowerFlying = entity instanceof Player player && player.isFallFlying()
                && ElytraFlightPowerService.hasFlight(player);
    }

    @Inject(method = "updateFallFlying", at = @At("TAIL"))
    private void ssc$retainPowerFlight(CallbackInfo ci) {
        if (!ssc$wasPowerFlying) return;
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player && !player.onGround() && !player.isPassenger()
                && !player.hasEffect(MobEffects.LEVITATION)) {
            player.startFallFlying();
        }
        ssc$wasPowerFlying = false;
    }
}
