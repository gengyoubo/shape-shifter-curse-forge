package net.onixary.shapeShifterCurseForge.perk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.client.PerkClientState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Runtime bridge for Perks that intentionally are not data-pack form powers. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class PerkPowerService {
    private static final String WATER_GUN_MARKER = "ssc_perk_water_gun";
    private static final String ACTIVE_SKILL_1 = "key.shape-shifter-curse.active_skill_1";
    private static final Map<UUID, Integer> WATER_GUN_COOLDOWNS = new HashMap<>();

    private PerkPowerService() {
    }

    /** Called after the normal active-power input path receives a rising key edge. */
    public static boolean triggerActive(ServerPlayer player, String key) {
        if (!ACTIVE_SKILL_1.equals(key) || !hasUnlocked(player, ModPerks.AXOLOTL_WATER_GUN.id())) return false;
        if (WATER_GUN_COOLDOWNS.getOrDefault(player.getUUID(), 0) > 0) return true;

        Snowball projectile = new Snowball(player.level(), player);
        projectile.getPersistentData().putBoolean(WATER_GUN_MARKER, true);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.25F, 0.35F);
        player.level().addFreshEntity(projectile);
        player.playSound(SoundEvents.PLAYER_SPLASH, 0.7F, 1.25F);
        WATER_GUN_COOLDOWNS.put(player.getUUID(), 16);
        return true;
    }

    /** Runs on both logical sides so the local owner does not sink before server correction. */
    public static void tick(Player player) {
        if (hasUnlocked(player, ModPerks.AXOLOTL_WATER_WALK.id())) maintainWaterWalk(player);
        if (!player.level().isClientSide) {
            WATER_GUN_COOLDOWNS.computeIfPresent(player.getUUID(), (id, ticks) -> ticks <= 1 ? null : ticks - 1);
        }
    }

    private static boolean hasUnlocked(Player player, net.minecraft.resources.ResourceLocation perkId) {
        if (player.level().isClientSide) return PerkClientState.hasUnlocked(perkId);
        return player instanceof ServerPlayer serverPlayer && PerkService.hasUnlocked(serverPlayer, perkId);
    }

    /** Keeps the player at a source-water surface; jumping still works because we never clamp ascent. */
    private static void maintainWaterWalk(Player player) {
        BlockPos feet = BlockPos.containing(player.getX(), player.getY() + 0.02D, player.getZ());
        FluidState fluid = player.level().getFluidState(feet);
        if (!fluid.is(FluidTags.WATER)) return;
        double surfaceY = feet.getY() + fluid.getHeight(player.level(), feet);
        // Only capture a player entering the top of a water column.  Do not pull someone who is
        // swimming deep underwater, falling from above, or deliberately jumping through it.
        if (player.getY() < surfaceY && player.getY() > surfaceY - 0.45D) {
            Vec3 movement = player.getDeltaMovement();
            player.setPos(player.getX(), surfaceY, player.getZ());
            player.setDeltaMovement(movement.x, Math.max(0.0D, movement.y), movement.z);
            player.fallDistance = 0.0F;
            if (!player.level().isClientSide) player.hurtMarked = true;
        }
    }

    @SubscribeEvent
    public static void waterGunImpact(ProjectileImpactEvent event) {
        if (!(event.getEntity() instanceof Snowball projectile) || projectile.level().isClientSide
                || !projectile.getPersistentData().getBoolean(WATER_GUN_MARKER)) return;
        event.setCanceled(true);
        if (projectile.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SPLASH, projectile.getX(), projectile.getY(), projectile.getZ(),
                    14, 0.20D, 0.20D, 0.20D, 0.05D);
        }
        HitResult hit = event.getRayTraceResult();
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target
                && projectile.getOwner() instanceof ServerPlayer owner) {
            target.hurt(owner.damageSources().thrown(projectile, owner), 3.0F);
            Vec3 push = projectile.getDeltaMovement().normalize().scale(0.75D);
            target.push(push.x, 0.15D, push.z);
            if (target instanceof ServerPlayer player) player.hurtMarked = true;
        }
        projectile.level().playSound(null, projectile.blockPosition(), SoundEvents.GENERIC_SPLASH,
                projectile.getSoundSource(), 0.8F, 1.0F);
        projectile.discard();
    }
}
