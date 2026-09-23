package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import net.onixary.shapeShifterCurseForge.power.MovementPowerService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityPoseMixin extends LivingEntity {

    @Unique
    private boolean ssc$wasSwimming;

    protected PlayerEntityPoseMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    /** Applies the Fabric swimming-state fallback at the same point vanilla updates it. */
    @Inject(method = "updateSwimming", at = @At("HEAD"))
    private void ssc$rememberSwimming(CallbackInfo ci) {
        ssc$wasSwimming = this.isSwimming();
    }

    @Inject(method = "updateSwimming", at = @At("TAIL"))
    private void ssc$forceSwimmingUnderwater(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.isSwimming() || MovementPowerService.shouldForceSwimming(player)
                || player.isPassenger()) return;

        // Match Fabric's EntityMixin: entering swimming requires full
        // submersion; an already swimming player may remain swimming while
        // still touching water. Do not force the sprint flag itself.
        boolean shouldSwim = ssc$wasSwimming
                ? player.isInWaterOrBubble()
                : player.isUnderWater()
                && player.level().getFluidState(player.blockPosition()).is(FluidTags.WATER);
        if (shouldSwim) player.setSwimming(true);
    }

    /** Fabric removes vanilla's upward swim impulse while this power is not sprinting. */
    @ModifyArg(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"),
            index = 0)
    private Vec3 ssc$preserveNonSprintSwimVerticalVelocity(Vec3 original) {
        Player player = (Player) (Object) this;
        if (!player.isSwimming() || player.isPassenger()
                || MovementPowerService.shouldForceSwimming(player) || player.isSprinting()) {
            return original;
        }
        Vec3 current = player.getDeltaMovement();
        return new Vec3(original.x, current.y, original.z);
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
        // Axolotl crawling is deliberately the vanilla SWIMMING pose on dry land,
        // not Forge's CROUCHING pose.  A low ceiling reaches it through vanilla
        // pose resolution; a held Shift reaches the same pose here.  The renderer
        // then replaces that single pose with the axolotl Geo crawl animation.
        if (ssc$shouldForceVanillaCrawl(player)) {
            this.setPose(Pose.SWIMMING);
            ci.cancel();
            return;
        }
        boolean isFeral = FormManager.current(player).bodyType() == FormBodyType.FERAL;
        // Fabric only replaces this method for FERAL forms.  In particular,
        // axolotl crouching remains vanilla crouching: SSC adjusts its bounds
        // and renderer without inventing a separate crawl pose.
        if (!isFeral) {
            return;
        }
        Pose pose;
        if (this.isFallFlying()) {
            pose = Pose.FALL_FLYING;
        } else if (this.isSleeping()) {
            pose = Pose.STANDING;
        } else if (this.isSwimming()) {
            pose = Pose.SWIMMING;
        } else if (this.isAutoSpinAttack()) {
            pose = Pose.SPIN_ATTACK;
        } else if (this.isShiftKeyDown() || MovementPowerService.shouldForceSneaking(player)) {
            pose = Pose.CROUCHING;
        } else {
            pose = Pose.STANDING;
        }

        Pose resolved = pose;
        if (!this.isSpectator() && !this.isPassenger() && !this.canEnterPose(pose)) {
            if (this.canEnterPose(Pose.CROUCHING)) {
                resolved = Pose.CROUCHING;
            } else {
                resolved = Pose.SWIMMING;
            }
        }
        this.setPose(resolved);
        ci.cancel();
    }

    @Unique
    private static boolean ssc$shouldForceVanillaCrawl(Player player) {
        // This is an on-ground substitute for vanilla's crawl entry.  Do not
        // turn a held Shift into crawling while jumping/falling or flying in
        // Creative; those states must retain their normal vanilla poses.
        // keep_sneaking counts as held Shift so axolotl head-collide / no-air
        // powers keep the crawl without requiring the key.
        if (!player.onGround() || player.getAbilities().flying
                || !MovementPowerService.isSneakingOrForced(player) || player.isInWaterOrBubble() || player.isPassenger()
                || player.isFallFlying() || player.isSleeping() || player.isAutoSpinAttack()) {
            return false;
        }
        final boolean[] hasCrawlingPower = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:crawling".equals(FormPowerRegistry.typeOf(power))) {
                hasCrawlingPower[0] = true;
            }
        });
        return hasCrawlingPower[0];
    }
}
