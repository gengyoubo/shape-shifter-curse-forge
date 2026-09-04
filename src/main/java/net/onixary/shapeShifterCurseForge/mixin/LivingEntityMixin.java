package net.onixary.shapeShifterCurseForge.mixin;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies {@code apoli:modify_jump} to the return value of
 * {@link LivingEntity#getJumpPower()}, mirroring Apoli's
 * {@code @ModifyReturnValue} injection into Fabric's
 * {@code LivingEntity#getJumpVelocity()}.
 * <p>
 * Vanilla first calculates its normal jump power, including its own
 * jump-related modifiers. SSC then applies Apoli-compatible modifiers
 * to that final value before {@code jumpFromGround()} uses it.
 * <p>
 * Plain {@code @Inject(at = @At("RETURN"), cancellable = true)} is used
 * instead of MixinExtras {@code @ModifyReturnValue}.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    private static final float SSC_MAX_WATER_FLEXIBILITY = 0.98F;
    @Unique
    public int ssc$noJumpTick = 0;
    @Unique
    private int ssc$tripleJumpCount = 0;
    @Unique
    private int ssc$tripleTicksOnGround = 0;
    @Unique
    private float ssc$tripleActiveMultiplier = 1.0F;

    @Inject(method = "tick", at = @At("HEAD"))
    private void ssc$tickNoJump(CallbackInfo ci) {
        if (ssc$noJumpTick > 0) {
            ssc$noJumpTick--;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }
        boolean shouldReset = false;
        if (!player.isSprinting()) {
            shouldReset = true;
        }
        if (player.onGround()) {
            ssc$tripleTicksOnGround++;
            final int[] resetTicks = {10};
            FormPowerRegistry.visitActive(player, (id, power) -> {
                if ("shape-shifter-curse:triple_jump".equals(FormPowerRegistry.typeOf(power))) {
                    resetTicks[0] = FormPowerRuntime.intValue(power, "reset_ticks_on_ground", 10);
                }
            });
            if (ssc$tripleTicksOnGround > resetTicks[0]) {
                shouldReset = true;
            }
        } else {
            ssc$tripleTicksOnGround = 0;
        }
        if (shouldReset) {
            ssc$tripleJumpCount = 0;
            ssc$tripleActiveMultiplier = 1.0F;
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void ssc$onJumpTriple(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }
        final boolean[] hasTriple = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:triple_jump".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                hasTriple[0] = true;
                if (!player.onGround() || !player.isSprinting()) {
                    ssc$tripleActiveMultiplier = 1.0F;
                    return;
                }
                ssc$tripleTicksOnGround = 0;
                ssc$tripleJumpCount++;
                float first = FormPowerRuntime.floatValue(power, "first_jump_multiplier", 1.0F);
                float second = FormPowerRuntime.floatValue(power, "second_jump_multiplier", 1.5F);
                float third = FormPowerRuntime.floatValue(power, "third_jump_multiplier", 2.0F);
                switch (ssc$tripleJumpCount) {
                    case 1 -> {
                        ssc$tripleActiveMultiplier = first;
                        FormPowerRuntime.execute(player, player, power.getAsJsonObject("first_jump_action"));
                    }
                    case 2 -> {
                        ssc$tripleActiveMultiplier = second;
                        FormPowerRuntime.execute(player, player, power.getAsJsonObject("second_jump_action"));
                    }
                    case 3 -> {
                        ssc$tripleActiveMultiplier = third;
                        FormPowerRuntime.execute(player, player, power.getAsJsonObject("third_jump_action"));
                        ssc$tripleJumpCount = 0;
                    }
                    default -> {
                        ssc$tripleJumpCount = 0;
                        ssc$tripleActiveMultiplier = 1.0F;
                    }
                }
            }
        });
        if (!hasTriple[0]) {
            ssc$tripleActiveMultiplier = 1.0F;
        }
    }

    @Inject(method = "getJumpPower", at = @At("HEAD"), cancellable = true)
    private void ssc$checkNoJump(CallbackInfoReturnable<Float> cir) {
        if (ssc$noJumpTick > 0) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getJumpPower", at = @At("RETURN"), cancellable = true)
    private void ssc$modifyJumpPower(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }
        final float[] modified = {cir.getReturnValue()};

        FormPowerRegistry.visitActive(player, (id, power) -> {
            String type = FormPowerRegistry.typeOf(power);

            if ("apoli:modify_jump".equals(type)) {
                JsonObject condition = power.has("condition")
                        ? power.getAsJsonObject("condition")
                        : null;

                boolean result = FormPowerRuntime.test(player, player, condition);

                if (!result) {
                    return;
                }

                if (power.has("modifier")) {
                    modified[0] = (float) FormPowerRuntime.applyModifier(
                            modified[0],
                            power.getAsJsonObject("modifier")
                    );
                }
            }
        });
        if (ssc$tripleActiveMultiplier != 1.0F) {
            float baseJumpVelocity = 0.42F;
            float additionalVelocity = modified[0] - baseJumpVelocity;
            modified[0] = (baseJumpVelocity * ssc$tripleActiveMultiplier) + additionalVelocity;
        }

        cir.setReturnValue(modified[0]);
    }

    /**
     * Mirrors SSC Fabric's water-flexibility hook at the point where vanilla applies
     * X/Z water damping. Replacing the damping value here makes it the final value
     * after Dolphin's Grace, rather than multiplying already-completed travel again
     * during PlayerTick.END.
     */
    @ModifyArg(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0), index = 0)
    private double ssc$modifyInWaterFlexibilityX(double original) {
        return ssc$waterFlexibilityDamping(original);
    }

    @ModifyArg(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0), index = 2)
    private double ssc$modifyInWaterFlexibilityZ(double original) {
        return ssc$waterFlexibilityDamping(original);
    }

    public void ssc$setNoJumpTick(int tick) {
        ssc$noJumpTick = tick;
    }

    public int ssc$getNoJumpTick() {
        return ssc$noJumpTick;
    }

    @Unique
    private double ssc$waterFlexibilityDamping(double original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player) || !player.isInWater()) {
            return original;
        }

        final float[] flexibility = {-1.0F};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:water_flexibility".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                flexibility[0] = Math.max(0.0F, Math.min(1.0F,
                        FormPowerRuntime.floatValue(power, "water_flex", 0.5F)));
            }
        });
        if (flexibility[0] < 0.0F) {
            return original;
        }

        return 0.8D + (SSC_MAX_WATER_FLEXIBILITY - 0.8D) * flexibility[0];
    }
}
