package net.onixary.shapeShifterCurseForge.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.power.AnubisMinionService;

/** Fabric's dedicated Anubis wolf minion, including level-dependent combat and owner lifetime. */
public final class AnubisWolfMinionEntity extends Wolf {
    private int minionLevel = 1;

    public AnubisWolfMinionEntity(EntityType<? extends AnubisWolfMinionEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    public void setMinionLevel(int level) {
        minionLevel = level;
        applyMinionLevel(true);
    }

    private void applyMinionLevel(boolean refillHealth) {
        if (getAttribute(Attributes.MAX_HEALTH) == null || getAttribute(Attributes.ATTACK_DAMAGE) == null) return;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(switch (minionLevel) {
            case 2 -> 16.0D;
            case 3 -> 20.0D;
            default -> 10.0D;
        });
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(switch (minionLevel) {
            case 2 -> 3.0D;
            case 3 -> 4.0D;
            default -> 2.0D;
        });
        if (refillHealth) setHealth(getMaxHealth());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.5D) {
            @Override
            protected boolean shouldPanic() {
                return AnubisWolfMinionEntity.this.isInPowderSnow
                        || AnubisWolfMinionEntity.this.isOnFire();
            }
        });
        goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
        goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));
        goalSelector.addGoal(6, new FollowOwnerWithoutTeleportGoal());
        goalSelector.addGoal(7, new BreedGoal(this, 1.0D));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractSkeleton.class, false));
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != MobEffects.REGENERATION
                && effect.getEffect() != MobEffects.POISON && super.canBeAffected(effect);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            if (target instanceof net.minecraft.world.entity.LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * minionLevel + 5, 2));
            }
            if (getOwner() != null && minionLevel >= 2) {
                getOwner().heal(minionLevel == 2 ? 1.0F : 2.0F);
            }
        }
        return hit;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            Player owner = getOwner() instanceof Player player ? player : null;
            if (owner == null || distanceToSqr(owner) > 1024.0D
                    || !AnubisMinionService.isRegistered(owner, getUUID())) {
                if (owner != null) AnubisMinionService.unregister(owner, getUUID());
                discard();
                return;
            }
            if (!hasEffect(MobEffects.WITHER)) addEffect(new MobEffectInstance(MobEffects.WITHER, -1, 0));
        }
        super.tick();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MinionLevel", minionLevel);
        tag.putFloat("MinionHealth", getHealth());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        minionLevel = tag.getInt("MinionLevel");
        applyMinionLevel(false);
        setHealth(tag.getFloat("MinionHealth"));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    @Override
    public void die(DamageSource source) {
        if (getOwner() instanceof Player owner) AnubisMinionService.unregister(owner, getUUID());
        setOwnerUUID(null);
        super.die(source);
    }

    private final class FollowOwnerWithoutTeleportGoal extends Goal {
        private Player owner;
        private int nextPathTick;
        private float oldWaterMalus;

        private FollowOwnerWithoutTeleportGoal() {
            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            owner = getOwner() instanceof Player player ? player : null;
            return owner != null && !owner.isSpectator() && !isOrderedToSit()
                    && !isPassenger() && !isLeashed() && distanceToSqr(owner) >= 100.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null && !getNavigation().isDone() && !isOrderedToSit()
                    && distanceToSqr(owner) > 4.0D;
        }

        @Override
        public void start() {
            nextPathTick = 0;
            oldWaterMalus = getPathfindingMalus(BlockPathTypes.WATER);
            setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        }

        @Override
        public void stop() {
            owner = null;
            getNavigation().stop();
            setPathfindingMalus(BlockPathTypes.WATER, oldWaterMalus);
        }

        @Override
        public void tick() {
            if (owner == null) return;
            getLookControl().setLookAt(owner, 10.0F, getMaxHeadXRot());
            if (--nextPathTick <= 0) {
                nextPathTick = adjustedTickDelay(10);
                getNavigation().moveTo(owner, 1.0D);
            }
        }
    }
}
