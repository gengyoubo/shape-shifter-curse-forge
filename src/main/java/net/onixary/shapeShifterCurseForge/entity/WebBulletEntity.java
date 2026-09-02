package net.onixary.shapeShifterCurseForge.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.onixary.shapeShifterCurseForge.power.WebPowerActions;
import net.onixary.shapeShifterCurseForge.registry.ModEntities;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

public final class WebBulletEntity extends ThrowableItemProjectile {
    private int tier = 1;
    private boolean buildTop = true;

    public WebBulletEntity(EntityType<? extends WebBulletEntity> type, Level level) {
        super(type, level);
    }

    public WebBulletEntity(Level level, LivingEntity owner, int tier, boolean buildTop) {
        super(ModEntities.WEB_BULLET.get(), owner, level);
        this.tier = Math.max(1, Math.min(3, tier));
        this.buildTop = buildTop;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.WEB_PROJECTILE.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(tier >= 3 ? ParticleTypes.CLOUD : ParticleTypes.ASH,
                    getX(), getY(), getZ(), tier, 0.05D, 0.05D, 0.05D, 0.01D);
            if (isInWaterOrBubble()) {
                discard();
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (hitResult.getEntity() instanceof LivingEntity target) {
            int duration = 40 + tier * 40;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, tier - 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, tier - 1));
        }
        hitEffects();
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (level() instanceof ServerLevel serverLevel) {
            WebPowerActions.buildLadder(serverLevel, hitResult.getBlockPos(), hitResult.getDirection(), tier, buildTop);
        }
        hitEffects();
        discard();
    }

    private void hitEffects() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, getX(), getY(), getZ(), 20,
                    0.3D, 0.3D, 0.3D, 0.05D);
            playSound(SoundEvents.WET_GRASS_BREAK, 1.0F, 0.9F + random.nextFloat() * 0.2F);
        }
    }
}
