package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityPoseMixin extends LivingEntity {

    protected PlayerEntityPoseMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    /** Fabric scales the existing exhaustion call instead of adding a second drain. */
    @Inject(method = "causeFoodExhaustion", at = @At("HEAD"), cancellable = true)
    private void ssc$scaleSwimmingExhaustion(float exhaustion, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!player.isSwimming()) {
            return;
        }
        final float[] multiplier = {1.0F};
        final boolean[] found = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!found[0] && "shape-shifter-curse:always_sprint_swimming".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                multiplier[0] = Math.max(0.0F,
                        FormPowerRuntime.floatValue(power, "hunger_multiplier", 1.0F));
                found[0] = true;
            }
        });
        if (found[0]) {
            player.getFoodData().addExhaustion(exhaustion * multiplier[0]);
            ci.cancel();
        }
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
