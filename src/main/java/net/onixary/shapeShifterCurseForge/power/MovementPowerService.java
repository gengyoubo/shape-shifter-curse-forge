package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative implementations for continuous movement and defensive form powers. */
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
            Vec3 normVel = velocity.lengthSqr() < 1e-8 ? Vec3.ZERO : velocity.normalize();
            Vec3 towardPlayer = player.position().subtract(projectile.position());
            double len2 = towardPlayer.lengthSqr();
            Vec3 towardNorm = len2 < 1e-8 ? Vec3.ZERO : towardPlayer.normalize();
            if (normVel.dot(towardNorm) <= 0.7D) continue;
            boolean right = !DODGE_RIGHT.getOrDefault(player.getUUID(), false);
            DODGE_RIGHT.put(player.getUUID(), right);
            double horizLen2 = velocity.x*velocity.x + velocity.z*velocity.z;
            Vec3 horizontal = horizLen2 < 1e-8 ? new Vec3(1,0,0) : new Vec3(velocity.x, 0.0D, velocity.z).normalize();
            Vec3 dodge = right ? new Vec3(-horizontal.z, 0.0D, horizontal.x) : new Vec3(horizontal.z, 0.0D, -horizontal.x);
            player.push(dodge.x * FormPowerRuntime.doubleValue(power, "dodge_speed", 1.0D), 0.0D,
                    dodge.z * FormPowerRuntime.doubleValue(power, "dodge_speed", 1.0D));
            markMotionForOwner(player);
            FormPowerRuntime.execute(player, player, power.getAsJsonObject("action"));
            DODGE_COOLDOWNS.put(player.getUUID(), Math.max(1, FormPowerRuntime.intValue(power, "cooldown", 20)));
            break;
        }
    }

    private static void walkPowderSnow(Player player) {
        if (player.getBlockStateOn().is(Blocks.POWDER_SNOW) || player.level().getBlockState(player.blockPosition()).is(Blocks.POWDER_SNOW)) {
            Vec3 motion = player.getDeltaMovement();
            setMotionAndSync(player, motion.x, Math.max(motion.y, 0.0D), motion.z);
            player.resetFallDistance();
        }
    }

    private static void resistWebSlowdown(Player player, JsonObject power) {
        BlockPos pos = player.blockPosition();
        if (!player.level().getBlockState(pos).is(Blocks.COBWEB) && !player.level().getBlockState(pos.below()).is(Blocks.COBWEB)) return;
        double multiplier = FormPowerRuntime.doubleValue(power, "multiplier", 1.0D);
        if (multiplier <= 0.0D) {
            // vanilla cobweb multiplies by 0.25; resist should restore to ~1.0, not compound each tick
            Vec3 motion = player.getDeltaMovement();
            // already slowed 0.25, so *4 restores; but only if currently slowed, avoid exponential blowup by clamping
            double restoredX = Math.abs(motion.x * 4.0D) > 0.5D ? motion.x : motion.x * 4.0D;
            double restoredZ = Math.abs(motion.z * 4.0D) > 0.5D ? motion.z : motion.z * 4.0D;
            setMotionAndSync(player, restoredX, Math.max(motion.y, -0.05D), restoredZ);
            // alternative: set to original input velocity if needed; keep single restoration per tick without compounding
        }
    }

    private static void applySoulSpeed(Player player, JsonObject power) {
        if (!player.getBlockStateOn().is(Blocks.SOUL_SAND) && !player.getBlockStateOn().is(Blocks.SOUL_SOIL)) return;
        double boost = 0.03D * Math.min(FormPowerRuntime.intValue(power, "level", 1),
                FormPowerRuntime.intValue(power, "max_level", 3));
        // soul speed should be additive to base speed, not multiplicative compounding each tick
        // use horizontal speed clamp to prevent exponential acceleration
        Vec3 motion = player.getDeltaMovement();
        double targetX = motion.x + Math.signum(motion.x) * boost * 0.5D;
        double targetZ = motion.z + Math.signum(motion.z) * boost * 0.5D;
        // cap max horizontal speed to 0.35 (approx sprint) + boost
        double cap = 0.35D + boost;
        double speed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (speed > cap) {
            double scale = cap / speed;
            targetX = motion.x * scale;
            targetZ = motion.z * scale;
        }
        setMotionAndSync(player, targetX, motion.y, targetZ);
    }

    private static void modifyFalling(Player player, JsonObject power) {
        if (player.onGround() || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
        double velocity = FormPowerRuntime.doubleValue(power, "velocity", 0.0D);
        Vec3 motion = player.getDeltaMovement();
        if (velocity >= 0.0D && motion.y < 0.0D) {
            setMotionAndSync(player, motion.x, Math.max(motion.y, -velocity), motion.z);
        }
    }

    /** Whether an active power provides Fabric's always-sprint-swimming behavior. */
    public static boolean shouldForceSwimming(Player player) {
        final boolean[] force = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!force[0] && "shape-shifter-curse:always_sprint_swimming".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                force[0] = true;
            }
        });
        return force[0];
    }

    private static final ThreadLocal<Boolean> FORCE_SNEAK_GUARD = ThreadLocal.withInitial(() -> false);

    /**
     * Forge port of Fabric's KeepSneakingPower.shouldForceSneak: while any active
     * {@code shape-shifter-curse:keep_sneaking} power has its condition met (and the
     * player is not in water), the player is treated as holding sneak for pose and
     * {@code apoli:sneaking} checks. Fabric explicitly returns false in water.
     */
    public static boolean shouldForceSneaking(Player player) {
        if (Boolean.TRUE.equals(FORCE_SNEAK_GUARD.get())) return false;
        if (player.isInWaterOrBubble() || player.isPassenger()) return false;
        final boolean[] force = {false};
        FORCE_SNEAK_GUARD.set(true);
        try {
            FormPowerRegistry.visitActive(player, (id, power) -> {
                if (!force[0] && "shape-shifter-curse:keep_sneaking".equals(FormPowerRegistry.typeOf(power))
                        && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                    force[0] = true;
                }
            });
        } finally {
            FORCE_SNEAK_GUARD.set(false);
        }
        return force[0];
    }

    /** Shift-held or keep_sneaking-forced sneak, for pose and speed decisions. */
    public static boolean isSneakingOrForced(Player player) {
        return player.isShiftKeyDown() || shouldForceSneaking(player);
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
        setMotionAndSync(player, direction.x * speed, motion.y, direction.z * speed);
        FormPowerRuntime.execute(player, closest instanceof net.minecraft.world.entity.LivingEntity living ? living : player,
                power.getAsJsonObject("entity_action"));
        FormPowerRuntime.execute(player, player, power.getAsJsonObject("self_action"));
    }

    /**
     * Forge's normal tracker sends {@code hasImpulse} motion updates only to
     * watchers. {@code hurtMarked} additionally sends the motion packet to the
     * owning ServerPlayer, matching Fabric's velocityModified flag.
     */
    private static void markMotionForOwner(Player player) {
        player.hurtMarked = true;
    }

    private static void setMotionAndSync(Player player, double x, double y, double z) {
        Vec3 before = player.getDeltaMovement();
        if (Double.compare(before.x, x) == 0 && Double.compare(before.y, y) == 0
                && Double.compare(before.z, z) == 0) {
            return;
        }
        player.setDeltaMovement(x, y, z);
        markMotionForOwner(player);
    }
}
