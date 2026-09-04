package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityPoseMixin extends LivingEntity {

    protected PlayerEntityPoseMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void ssc$forceFeralPose(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        boolean isFeral = FormManager.current(player).bodyType() == FormBodyType.FERAL;
        if (!isFeral) {
            return;
        }
        // Mirror Fabric: try swimming pose first; keep vanilla's collision-aware fallback.
        if (this.canEnterPose(Pose.SWIMMING)) {
            Pose pose;
            if (this.isFallFlying()) {
                pose = Pose.FALL_FLYING;
            } else if (this.isSleeping()) {
                pose = Pose.STANDING;
            } else if (this.isSwimming()) {
                pose = Pose.STANDING;
            } else if (this.isAutoSpinAttack()) {
                pose = Pose.SPIN_ATTACK;
            } else if (this.isShiftKeyDown()) {
                pose = Pose.CROUCHING;
            } else {
                pose = Pose.STANDING;
            }

            Pose resolved;
            if (!this.isSpectator() && !this.isPassenger() && !this.canEnterPose(pose)) {
                if (this.canEnterPose(Pose.CROUCHING)) {
                    resolved = Pose.CROUCHING;
                } else {
                    resolved = Pose.STANDING;
                }
            } else {
                resolved = pose;
            }

            this.setPose(resolved);
        }
        ci.cancel();
    }
}
