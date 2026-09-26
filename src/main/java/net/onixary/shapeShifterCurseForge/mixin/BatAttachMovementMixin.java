package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.onixary.shapeShifterCurseForge.network.BatDetachRequestPacket;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.power.BatAttachService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fabric's attached-player movement and jump controls on both logical sides. */
@Mixin(value = Player.class, priority = 1100)
public abstract class BatAttachMovementMixin {
    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
    private void ssc$stopAttachedTravel(Vec3 input, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!BatAttachService.isAttached(player)) return;
        player.setDeltaMovement(Vec3.ZERO);
        ci.cancel();
    }

    @Inject(method = "getSpeed()F", at = @At("RETURN"), cancellable = true)
    private void ssc$stopAttachedSpeed(CallbackInfoReturnable<Float> cir) {
        if (BatAttachService.isAttached((Player) (Object) this)) {
            cir.setReturnValue(0.0F);
            return;
        }
        if (this instanceof net.onixary.shapeShifterCurseForge.power.LivingEntityJumpState state
                && state.ssc$getNoMoveTick() > 0) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void ssc$jumpOffAttachment(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!BatAttachService.isAttached(player)) return;
        if (player.level().isClientSide) {
            ModNetwork.CHANNEL.sendToServer(new BatDetachRequestPacket());
        } else {
            BatAttachService.detachForJump(player);
        }
        ci.cancel();
    }

    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void ssc$detachInsteadOfFlying(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (!BatAttachService.isAttached(player)) return;
        if (player.level().isClientSide) {
            ModNetwork.CHANNEL.sendToServer(new BatDetachRequestPacket());
        } else {
            BatAttachService.detachForJump(player);
        }
        player.stopFallFlying();
        player.setOnGround(true);
        cir.setReturnValue(false);
    }
}
