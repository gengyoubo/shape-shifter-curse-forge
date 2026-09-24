package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.ElytraFlightPowerService;
import net.onixary.shapeShifterCurseForge.power.BatAttachService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allows the ordinary double-jump glide input without an equipped Elytra. */
@Mixin(Player.class)
public abstract class ElytraFlightPlayerMixin {
    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void ssc$startPowerFlight(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (BatAttachService.isAttached(player) || !ElytraFlightPowerService.hasFlight(player) || player.onGround()
                || player.isFallFlying() || player.isInWater()
                || player.hasEffect(MobEffects.LEVITATION)) return;
        player.startFallFlying();
        cir.setReturnValue(true);
    }
}
