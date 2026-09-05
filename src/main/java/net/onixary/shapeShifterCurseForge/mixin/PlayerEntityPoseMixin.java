package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import net.onixary.shapeShifterCurseForge.power.CrawlingScaleService;
import net.onixary.shapeShifterCurseForge.power.MovementPowerService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityPoseMixin extends LivingEntity {

    protected PlayerEntityPoseMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * The Forge tick event runs after travel.  Apply the deep-water swimming
     * state at the same point vanilla updates it so the current travel call
     * sees both sprinting and the swim-speed attribute.
     */
    @Inject(method = "updateSwimming", at = @At("TAIL"))
    private void ssc$forceSwimmingUnderwater(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (MovementPowerService.shouldForceSwimming(player)) {
            player.setSprinting(true);
            player.setSwimming(true);
        }
    }

    /** Fabric scales the existing exhaustion call instead of adding a second drain. */
    @Inject(method = "causeFoodExhaustion", at = @At("HEAD"), cancellable = true)
    private void ssc$scaleSwimmingExhaustion(float exhaustion, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        final float[] modified = {exhaustion};
        final boolean[] found = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("apoli:modify_exhaustion".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                modified[0] = (float) FormPowerRuntime.applyModifier(modified[0],
                        power.getAsJsonObject("modifier"));
                found[0] = true;
            }
            if (player.isSwimming() && "shape-shifter-curse:always_sprint_swimming".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                modified[0] *= Math.max(0.0F,
                        FormPowerRuntime.floatValue(power, "hunger_multiplier", 1.0F));
                found[0] = true;
            }
        });
        if (found[0]) {
            player.getFoodData().addExhaustion(modified[0]);
            ci.cancel();
        }
    }

    @Inject(method = "hasCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    private void ssc$allowAllHarvest(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("apoli:modify_harvest".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.booleanValue(power, "allow", false)
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                cir.setReturnValue(true);
            }
        });
    }

    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void ssc$forceFeralPose(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        boolean isFeral = FormManager.current(player).bodyType() == FormBodyType.FERAL;
        boolean forceCrawling = shouldForceCrawling(player);
        // Axolotl forms are NORMAL body types, but their keep_sneaking power
        // still needs the special one-block pose. Other normal forms retain
        // vanilla pose handling unless that power is actually active.
        if (!isFeral && !forceCrawling) {
            return;
        }
        // Keep the collision-aware vanilla order, with the two axolotl state
        // overrides inserted before the fallback is resolved.  Vanilla leaves
        // the swimming flag set for a tick after entering a one-block tunnel;
        // clear that flag so the special compressed pose is not mistaken for
        // actual water swimming.
        if (forceCrawling && !player.isInWater()) {
            player.setSwimming(false);
        }
        Pose pose;
        if (this.isFallFlying()) {
            pose = Pose.FALL_FLYING;
        } else if (this.isSleeping()) {
            pose = Pose.STANDING;
        } else if (forceCrawling) {
            // Automatic one-block crawling is the same form state as holding
            // Shift. Water swimming remains a separate state below.
            pose = Pose.CROUCHING;
        } else if (this.isSwimming()) {
            pose = Pose.SWIMMING;
        } else if (this.isAutoSpinAttack()) {
            pose = Pose.SPIN_ATTACK;
        } else if (this.isShiftKeyDown()) {
            pose = Pose.CROUCHING;
        } else {
            pose = Pose.STANDING;
        }

        Pose resolved = pose;
        if (!this.isSpectator() && !this.isPassenger() && !this.canEnterPose(pose)) {
            if (forceCrawling) {
                resolved = Pose.CROUCHING;
            } else if (this.canEnterPose(Pose.CROUCHING)) {
                resolved = Pose.CROUCHING;
            } else {
                resolved = Pose.SWIMMING;
            }
        }
        this.setPose(resolved);
        if (forceCrawling) {
            // setPose does not guarantee that Forge's Size event has run in the
            // same update. Refresh immediately so the low crawl hitbox is used
            // before collision resolution tries to move the player out of the
            // one-block space.
            CrawlingScaleService.tick(player);
        }
        ci.cancel();
    }

    private static boolean shouldForceCrawling(Player player) {
        return CrawlingScaleService.isForcedCrawling(player);
    }
}
