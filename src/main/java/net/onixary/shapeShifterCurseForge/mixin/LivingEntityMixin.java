package net.onixary.shapeShifterCurseForge.mixin;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.other.config.SscCommonConfig;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import net.onixary.shapeShifterCurseForge.power.FormPowerEvents;
import net.onixary.shapeShifterCurseForge.power.ClimbingExService;
import net.onixary.shapeShifterCurseForge.power.LivingEntityJumpState;
import net.onixary.shapeShifterCurseForge.power.MovementPowerService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
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
@SuppressWarnings("JavadocReference")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements LivingEntityJumpState {
    @Shadow protected float xxa;
    @Shadow protected float zza;
    @Shadow protected abstract void hurtCurrentlyUsedShield(float amount);
    @Shadow protected abstract Vec3 getFluidFallingAdjustedMovement(double gravity, boolean falling, Vec3 velocity);
    @Shadow private Optional<BlockPos> lastClimbablePos;
    @Unique private int ssc$lastClimbDebugTick = Integer.MIN_VALUE;
    @Unique private boolean ssc$lastClimbDebugResult;

    /** Apoli's entity_group power changes the value used by vanilla enchantments. */
    @Inject(method = "getMobType", at = @At("HEAD"), cancellable = true)
    private void ssc$applyEntityGroup(CallbackInfoReturnable<net.minecraft.world.entity.MobType> cir) {
        if (!((Object) this instanceof Player player)) return;
        final net.minecraft.world.entity.MobType[] group = {null};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (group[0] != null || !"apoli:entity_group".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            group[0] = switch (FormPowerRuntime.stringValue(power, "group", "")) {
                case "undead" -> net.minecraft.world.entity.MobType.UNDEAD;
                case "arthropod" -> net.minecraft.world.entity.MobType.ARTHROPOD;
                case "aquatic" -> net.minecraft.world.entity.MobType.WATER;
                case "illager" -> net.minecraft.world.entity.MobType.ILLAGER;
                default -> null;
            };
        });
        if (group[0] != null) cir.setReturnValue(group[0]);
    }

    @ModifyVariable(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"),
            ordinal = 0, require = 1)
    private double ssc$modifyFallingGravity(double gravity) {
        LivingEntity self = (LivingEntity) (Object) this;
        return self instanceof Player player
                ? MovementPowerService.modifyFallingGravity(player, gravity) : gravity;
    }

    @Unique
    private static final float SSC_MAX_WATER_FLEXIBILITY = 0.98F;

    /** Fabric subtracts falling protection inside calculateFallDamage, preserving the real fall distance. */
    @ModifyVariable(method = "calculateFallDamage(FF)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float ssc$protectedFallDistance(float distance) {
        if (!((Object) this instanceof Player player)) return distance;
        final float[] strongest = {0.0F};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:falling_protection".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                strongest[0] = Math.max(strongest[0], FormPowerRuntime.floatValue(power, "fall_distance", 0.0F));
            }
        });
        return Math.max(0.0F, distance - strongest[0]);
    }
    @Unique
    public int ssc$noJumpTick = 0;
    @Unique
    private int ssc$tripleJumpCount = 0;
    @Unique
    private int ssc$tripleTicksOnGround = 0;
    @Unique
    private float ssc$tripleActiveMultiplier = 1.0F;
    @Unique
    private boolean ssc$jumpStartedOnBlock;
    @Unique
    private boolean ssc$virtualShieldBlocked;

    @Inject(method = "hurt", at = @At("HEAD"))
    private void ssc$resetVirtualShieldBlock(DamageSource source, float amount,
                                             CallbackInfoReturnable<Boolean> cir) {
        ssc$virtualShieldBlocked = false;
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void ssc$runSuccessfulHitActions(DamageSource source, float amount,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            FormPowerEvents.onSuccessfulHit((LivingEntity) (Object) this, source, amount);
        }
    }

    @Inject(method = "isDamageSourceBlocked", at = @At("HEAD"), cancellable = true)
    private void ssc$blockWithVirtualShield(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && FormPowerEvents.blocksWithVirtualShield(player, source)) {
            ssc$virtualShieldBlocked = true;
            cir.setReturnValue(true);
        }
    }

    @Redirect(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurtCurrentlyUsedShield(F)V"), require = 1)
    private void ssc$preserveRealShieldDurability(LivingEntity instance, float amount) {
        if (!ssc$virtualShieldBlocked) this.hurtCurrentlyUsedShield(amount);
        ssc$virtualShieldBlocked = false;
    }

    /** Forge replaces vanilla's baseTick air update with ForgeHooks.onLivingBreathe. */
    @ModifyArg(method = "baseTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/common/ForgeHooks;onLivingBreathe(Lnet/minecraft/world/entity/LivingEntity;II)V",
                    remap = false), index = 2, require = 1)
    private int ssc$suppressDryLandAirRefill(int refill) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player
                && FormPowerEvents.hasCustomWaterBreathing(player)
                && FormPowerEvents.isDryLandForCustomWaterBreathing(player)) {
            return 0;
        }
        return refill;
    }

    @Inject(method = "canBreatheUnderwater", at = @At("HEAD"), cancellable = true)
    private void ssc$canBreatheUnderwaterWithCustomPower(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && FormPowerEvents.hasCustomWaterBreathing(player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V",
                    shift = At.Shift.BEFORE))
    private void ssc$debugBeforeSwimInput(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player) || !ssc$shouldDebugAxolotlSwim(player)) return;
        ShapeShifterCurseForge.LOGGER.info(
                "[SSC-TRAVEL-DEBUG] stage=before-moveRelative side={} tick={} input=({}, {}, {}) xxa={} zza={} velocity={}",
                player.level().isClientSide ? "client" : "server", player.tickCount,
                input.x, input.y, input.z, xxa, zza, player.getDeltaMovement());
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V",
                    shift = At.Shift.AFTER))
    private void ssc$debugAfterSwimInput(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player) || !ssc$shouldDebugAxolotlSwim(player)) return;
        ShapeShifterCurseForge.LOGGER.info(
                "[SSC-TRAVEL-DEBUG] stage=after-moveRelative side={} tick={} input=({}, {}, {}) xxa={} zza={} velocity={}",
                player.level().isClientSide ? "client" : "server", player.tickCount,
                input.x, input.y, input.z, xxa, zza, player.getDeltaMovement());
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void ssc$tickNoJump(CallbackInfo ci) {
        if (ssc$noJumpTick > 0) {
            ssc$noJumpTick--;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }
        boolean shouldReset = !player.isSprinting();
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
        ssc$jumpStartedOnBlock = player.onGround();
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

    /** Edit the block friction before vanilla derives both acceleration and damping from it. */
    @Redirect(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getFriction(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)F",
                    remap = false),
            require = 1)
    private float ssc$modifyFriction(net.minecraft.world.level.block.state.BlockState state,
                                     net.minecraft.world.level.LevelReader level,
                                     BlockPos pos, net.minecraft.world.entity.Entity entity) {
        float friction = state.getFriction(level, pos, entity);
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return friction;
        final float[] modified = {friction};
        final boolean[] applied = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("apoli:modify_slipperiness".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))
                    && FormPowerRuntime.matchesBlockState(player.level(), pos, power.getAsJsonObject("block_condition"))) {
                modified[0] = (float) FormPowerRuntime.applyModifier(modified[0], power.getAsJsonObject("modifier"));
                applied[0] = true;
                return;
            }
            if (!"shape-shifter-curse:conditioned_modify_slipperiness".equals(FormPowerRegistry.typeOf(power))) return;
            if (!FormPowerRuntime.matchesBlockState(player.level(), pos,
                    power.getAsJsonObject("block_condition"))) return;
            if (!FormPowerRuntime.test(player, player, power.getAsJsonObject("entity_condition"))) return;
            com.google.gson.JsonElement modEl = power.get("modifier");
            if (modEl != null && modEl.isJsonPrimitive()) {
                // Fabric: original 0.6 + 0.35 = 0.95 (addition)
                float delta = modEl.getAsFloat();
                modified[0] = friction + delta;
                applied[0] = true;
            } else if (modEl != null && modEl.isJsonObject()) {
                modified[0] = (float) FormPowerRuntime.applyModifier(modified[0], modEl.getAsJsonObject());
                applied[0] = true;
            }
        });
        return applied[0] ? modified[0] : friction;
    }

    @ModifyArg(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"),
            index = 0)
    private float ssc$modifyAirSpeed(float speed) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return speed;
        final float[] modified = {speed};
        if (player.onGround() || player.isInWater()) return modified[0];
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("apoli:modify_air_speed".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                modified[0] = (float) FormPowerRuntime.applyModifier(modified[0], power.getAsJsonObject("modifier"));
            }
        });
        return modified[0];
    }

    /** Fabric's LikeWaterPower suppresses the gravity adjustment only at terminal water drift. */
    @Redirect(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getFluidFallingAdjustedMovement(DZLnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0), require = 1)
    private Vec3 ssc$likeWaterTravel(LivingEntity entity, double gravity, boolean falling, Vec3 velocity) {
        Vec3 adjusted = this.getFluidFallingAdjustedMovement(gravity, falling, velocity);
        if (!((Object) this instanceof Player player)) return adjusted;
        final boolean[] likeWater = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:like_water".equals(id.toString())
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                likeWater[0] = true;
            }
        });
        if (likeWater[0] && Math.abs(velocity.y - gravity / 16.0D) < 0.025D) {
            return new Vec3(adjusted.x, 0.0D, adjusted.z);
        }
        return adjusted;
    }

    /** Fabric's BreathingUnderWaterPower changes the vanilla water-air drain to a 1% chance. */
    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void ssc$modifyWaterAirDrain(int air, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }
        final boolean[] active = {false};
        final boolean[] holdBreath = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:breathing_under_water".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                active[0] = true;
            } else if ("shape-shifter-curse:hold_breath".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                holdBreath[0] = true;
            }
        });
        if (active[0]) {
            cir.setReturnValue(player.getRandom().nextInt(101) == 0 ? air - 1 : air);
        } else if (holdBreath[0]) {
            cir.setReturnValue(player.getRandom().nextInt(4) > 0 ? air : air - 1);
        }
    }

    @Inject(method = "canBreatheUnderwater", at = @At("HEAD"), cancellable = true)
    private void ssc$customWaterBreather(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && FormPowerEvents.hasCustomWaterBreathing(player)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Apoli's {@code apoli:climbing} power (Fabric {@code ClimbingPower}): the entity is
     * treated as being on a ladder while the power's {@code condition} or
     * {@code hold_condition} matches. This is how the spider form climbs cobwebs.
     */
    // TODO[TEST] Newly added mapping of Apoli's apoli:climbing power onto LivingEntity#onClimbable;
    //   verify the spider form can climb cobwebs and that the condition/hold_condition window matches
    //   Fabric's ClimbingPower.
    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void ssc$powerClimbing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player) || player.isSpectator()) {
            return;
        }
        final boolean[] climbing = {false};
        boolean vanilla = cir.getReturnValue();
        if (!vanilla) {
            FormPowerRegistry.visitActive(player, (id, power) -> {
                if (climbing[0]) return;
                if ("shape-shifter-curse:climbing_ex".equals(FormPowerRegistry.typeOf(power))) {
                    climbing[0] = ClimbingExService.isActive(player, id, power);
                    return;
                }
                if (!"apoli:climbing".equals(FormPowerRegistry.typeOf(power))) return;
                JsonObject start = power.getAsJsonObject("condition");
                if (FormPowerRuntime.test(player, player, start)) {
                    climbing[0] = true;
                }
            });
        }
        if (climbing[0]) {
            this.lastClimbablePos = Optional.of(player.blockPosition());
            cir.setReturnValue(true);
        }
        boolean result = vanilla || climbing[0];
        if (SscCommonConfig.ENABLE_MOVEMENT_DEBUG_LOGGING.get()
                && FormManager.current(player).id().getPath().startsWith("ocelot_")
                && (ssc$lastClimbDebugTick != player.tickCount || ssc$lastClimbDebugResult != result)) {
            ssc$lastClimbDebugTick = player.tickCount;
            ssc$lastClimbDebugResult = result;
            ShapeShifterCurseForge.LOGGER.info(
                    "[SSC-CLIMB-DEBUG] stage=onClimbable side={} tick={} vanilla={} power={} result={} onGround={} horizontalCollision={} velocity={}",
                    player.level().isClientSide ? "client" : "server", player.tickCount, vanilla,
                    climbing[0], result, player.onGround(), player.horizontalCollision, player.getDeltaMovement());
        }
    }

    @Inject(method = "isSuppressingSlidingDownLadder", at = @At("RETURN"), cancellable = true)
    private void ssc$climbingExHolding(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return;
        final boolean[] hasClimbingPower = {false};
        final boolean[] holding = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            String type = FormPowerRegistry.typeOf(power);
            if ("shape-shifter-curse:climbing_ex".equals(type)) {
                if (!ClimbingExService.isActive(player, id, power)) return;
                hasClimbingPower[0] = true;
                holding[0] |= ClimbingExService.canHold(player, power);
            } else if ("apoli:climbing".equals(type)
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                hasClimbingPower[0] = true;
                JsonObject holdCondition = power.getAsJsonObject("hold_condition");
                holding[0] |= FormPowerRuntime.booleanValue(power, "allow_holding", true)
                        && (holdCondition == null ? player.isShiftKeyDown()
                        : FormPowerRuntime.test(player, player, holdCondition));
            }
        });
        if (hasClimbingPower[0]) cir.setReturnValue(holding[0]);
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
        double modified = ssc$waterFlexibilityDamping(original);
        ssc$logWaterFlexibilityDamping("x", original, modified);
        return modified;
    }

    @ModifyArg(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0), index = 2)
    private double ssc$modifyInWaterFlexibilityZ(double original) {
        double modified = ssc$waterFlexibilityDamping(original);
        ssc$logWaterFlexibilityDamping("z", original, modified);
        return modified;
    }

    /**
     * A surface launch is issued after the current travel call. If the player
     * is still barely touching water on the next tick, vanilla would otherwise
     * apply its normal vertical water damping to the freshly-added launch.
     */
    @ModifyArg(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 0), index = 1)
    private double ssc$preserveSurfaceLaunchY(double original) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && FormActivePowerService.consumeWaterLaunchGrace(player)) {
            return 1.0D;
        }
        return original;
    }

    @Unique
    public void ssc$setNoJumpTick(int tick) {
        ssc$noJumpTick = tick;
    }

    @Unique
    public int ssc$getNoJumpTick() {
        return ssc$noJumpTick;
    }

    @Override
    public boolean ssc$wasJumpStartedOnBlock() {
        return ssc$jumpStartedOnBlock;
    }

    @Override
    public void ssc$clearJumpStartedOnBlock() {
        ssc$jumpStartedOnBlock = false;
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

    @Unique
    private void ssc$logWaterFlexibilityDamping(String axis, double vanillaFactor, double appliedFactor) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player) || !ssc$shouldDebugAxolotlSwim(player)) return;
        ShapeShifterCurseForge.LOGGER.info(
                "[SSC-TRAVEL-DEBUG] stage=water-flexibility axis={} side={} tick={} vanillaFactor={} appliedFactor={} velocity={}",
                axis, player.level().isClientSide ? "client" : "server", player.tickCount,
                vanillaFactor, appliedFactor, player.getDeltaMovement());
    }

    @Unique
    private static boolean ssc$shouldDebugAxolotlSwim(Player player) {
        return SscCommonConfig.ENABLE_MOVEMENT_DEBUG_LOGGING.get()
                && MovementPowerService.hasAlwaysSprintSwimmingPower(player)
                && (player.isInWater() || player.isSwimming());
    }
}
