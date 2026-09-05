package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-side implementations for continuous movement and defensive form powers. */
public final class MovementPowerService {
    private static final Map<UUID, Integer> DODGE_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Boolean> DODGE_RIGHT = new HashMap<>();

    private MovementPowerService() { }

    public static void tick(Player player) {
        DODGE_COOLDOWNS.computeIfPresent(player.getUUID(), (id, ticks) -> ticks <= 1 ? null : ticks - 1);

        FormPowerRegistry.visitActive(player, (id, power) -> {
            switch (FormPowerRegistry.typeOf(power)) {
                case "shape-shifter-curse:projectile_dodge" -> dodgeProjectiles(player, power);
                case "shape-shifter-curse:powder_snow_walker" -> walkPowderSnow(player);
                case "shape-shifter-curse:slowdown_percent" -> resistWebSlowdown(player, power);
                case "shape-shifter-curse:soul_speed" -> applySoulSpeed(player, power);
                case "shape-shifter-curse:attract_by_entity" -> attractEntity(player, power);
                case "shape-shifter-curse:always_sprint_swimming" -> forceSprintSwimming(player);
                case "apoli:modify_falling" -> modifyFalling(player, power);
                default -> { }
            }
        });
    }

    private static void dodgeProjectiles(Player player, JsonObject power) {
        if (DODGE_COOLDOWNS.containsKey(player.getUUID())
                || !FormPowerRuntime.test(player, player, power.getAsJsonObject("entity_condition"))) return;
        double range = FormPowerRuntime.doubleValue(power, "range", 5.0D);
        double triggerDistance = FormPowerRuntime.doubleValue(power, "trigger_distance", 4.0D);
        for (Projectile projectile : player.level().getEntitiesOfClass(Projectile.class,
                player.getBoundingBox().inflate(range), candidate -> candidate.getOwner() != player && !candidate.isRemoved())) {
            Vec3 velocity = projectile.getDeltaMovement();
            if (velocity.lengthSqr() < 0.01D || projectile.position().distanceTo(player.position()) > triggerDistance) continue;
            Vec3 towardPlayer = player.position().subtract(projectile.position()).normalize();
            if (velocity.normalize().dot(towardPlayer) <= 0.7D) continue;
            boolean right = !DODGE_RIGHT.getOrDefault(player.getUUID(), false);
            DODGE_RIGHT.put(player.getUUID(), right);
            Vec3 horizontal = new Vec3(velocity.x, 0.0D, velocity.z).normalize();
            Vec3 dodge = right ? new Vec3(-horizontal.z, 0.0D, horizontal.x) : new Vec3(horizontal.z, 0.0D, -horizontal.x);
            player.push(dodge.x * FormPowerRuntime.doubleValue(power, "dodge_speed", 1.0D), 0.0D,
                    dodge.z * FormPowerRuntime.doubleValue(power, "dodge_speed", 1.0D));
            FormPowerRuntime.execute(player, player, power.getAsJsonObject("action"));
            DODGE_COOLDOWNS.put(player.getUUID(), Math.max(1, FormPowerRuntime.intValue(power, "cooldown", 20)));
            break;
        }
    }

    private static void walkPowderSnow(Player player) {
        if (player.getBlockStateOn().is(Blocks.POWDER_SNOW) || player.level().getBlockState(player.blockPosition()).is(Blocks.POWDER_SNOW)) {
            player.setDeltaMovement(player.getDeltaMovement().x, Math.max(player.getDeltaMovement().y, 0.0D), player.getDeltaMovement().z);
            player.resetFallDistance();
        }
    }

    private static void resistWebSlowdown(Player player, JsonObject power) {
        BlockPos pos = player.blockPosition();
        if (!player.level().getBlockState(pos).is(Blocks.COBWEB) && !player.level().getBlockState(pos.below()).is(Blocks.COBWEB)) return;
        double multiplier = FormPowerRuntime.doubleValue(power, "multiplier", 1.0D);
        if (multiplier <= 0.0D) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * 4.0D, Math.max(motion.y, -0.05D), motion.z * 4.0D);
        }
    }

    private static void applySoulSpeed(Player player, JsonObject power) {
        if (!player.getBlockStateOn().is(Blocks.SOUL_SAND) && !player.getBlockStateOn().is(Blocks.SOUL_SOIL)) return;
        double boost = 0.03D * Math.min(FormPowerRuntime.intValue(power, "level", 1),
                FormPowerRuntime.intValue(power, "max_level", 3));
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x * (1.0D + boost), motion.y, motion.z * (1.0D + boost));
    }

    private static void modifyFalling(Player player, JsonObject power) {
        if (player.onGround() || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
        double velocity = FormPowerRuntime.doubleValue(power, "velocity", 0.0D);
        Vec3 motion = player.getDeltaMovement();
        if (velocity >= 0.0D && motion.y < 0.0D) {
            player.setDeltaMovement(motion.x, Math.max(motion.y, -velocity), motion.z);
        }
    }

    /** Whether an active power requires the player to swim in deep water. */
    public static boolean shouldForceSwimming(Player player) {
        if (!player.isEyeInFluid(FluidTags.WATER) || player.isPassenger()) {
            return false;
        }

        final boolean[] force = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!force[0] && "shape-shifter-curse:always_sprint_swimming".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                force[0] = true;
            }
        });
        return force[0];
    }

    /** Applies the power before travel, so the swim-speed attribute is used immediately. */
    public static void forceSprintSwimming(Player player) {
        if (shouldForceSwimming(player)) {
            player.setSprinting(true);
            player.setSwimming(true);
        }
    }

    private static void attractEntity(Player player, JsonObject power) {
        if (!player.onGround() || player.isPassenger()) return;
        double radius = FormPowerRuntime.doubleValue(power, "attraction_radius", 8.0D);
        double stop = FormPowerRuntime.doubleValue(power, "stop_radius", 1.0D);
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity candidate : player.level().getEntities(player, player.getBoundingBox().inflate(radius),
                entity -> entity.isAlive() && !entity.isSpectator()
                        && FormPowerRuntime.test(player, entity, power.getAsJsonObject("entity_condition")))) {
            double distance = candidate.distanceToSqr(player);
            if (distance > stop * stop && distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }
        if (closest == null) return;
        Vec3 direction = new Vec3(closest.getX() - player.getX(), 0.0D, closest.getZ() - player.getZ()).normalize();
        double speed = FormPowerRuntime.doubleValue(power, "attraction_speed", 0.1D);
        if (player.getLookAngle().dot(direction) < 0.0D) speed = FormPowerRuntime.doubleValue(power, "escape_attraction_speed", 0.025D);
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(direction.x * speed, motion.y, direction.z * speed);
        FormPowerRuntime.execute(player, closest instanceof net.minecraft.world.entity.LivingEntity living ? living : player,
                power.getAsJsonObject("entity_action"));
        FormPowerRuntime.execute(player, player, power.getAsJsonObject("self_action"));
    }
}
