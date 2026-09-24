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
import net.onixary.shapeShifterCurseForge.util.Accessory.AccessoryUtils;
import net.onixary.shapeShifterCurseForge.power.WebPowerActions;
import net.onixary.shapeShifterCurseForge.power.WebEntanglementService;
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
            if (getOwner() instanceof net.minecraft.world.entity.player.Player player
                    && isVenomSpindleEquipped(player)) {
                applyVenomSpindleEffects(target);
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, tier - 1));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, tier - 1));
                // Visible "entangled" tier effect (its amplifier tracks remaining duration).
                target.addEffect(new MobEffectInstance(
                        net.onixary.shapeShifterCurseForge.registry.ModEffects.ENTANGLED.get(), duration, tier - 1));
                WebEntanglementService.apply(getOwner(), target, switch (tier) {
                    case 2 -> 400;
                    case 3 -> 600;
                    default -> 200;
                });
            }
        }
        hitEffects();
        discard();
    }

    private boolean isVenomSpindleEquipped(net.minecraft.world.entity.player.Player player) {
        var stack = AccessoryUtils.getEntitySlot(player, "auto", "hand", "extra_hand", 0);
        return stack != null && !stack.isEmpty() && stack.is(ModItems.VENOM_SPINDLE.get());
    }

    /** Mirrors Fabric's Venom Spindle branch: replace slow/entangle with poison and direct damage. */
    private void applyVenomSpindleEffects(LivingEntity target) {
        switch (tier) {
            case 1 -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 20, 2));
                target.hurt(damageSources().thrown(this, getOwner()), 5.0F);
            }
            case 2 -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 2));
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 2));
                target.hurt(damageSources().thrown(this, getOwner()), 6.0F);
            }
            case 3 -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 3));
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 2));
                target.hurt(damageSources().thrown(this, getOwner()), 8.0F);
            }
            default -> throw new IllegalStateException("Unexpected web bullet tier: " + tier);
        }
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
