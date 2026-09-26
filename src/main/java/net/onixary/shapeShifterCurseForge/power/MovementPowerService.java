package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative implementations for continuous movement and defensive form powers. */
public final class MovementPowerService {
    private record DodgeKey(UUID playerId, ResourceLocation powerId) { }
    private static final Map<DodgeKey, Integer> DODGE_COOLDOWNS = new HashMap<>();
    private static final Map<DodgeKey, Boolean> DODGE_RIGHT = new HashMap<>();
    private static final Map<UUID, AttractState> ATTRACT_STATES = new HashMap<>();
    private static final Map<UUID, Boolean> SERVER_FORCED_SNEAK_STATES = new HashMap<>();
    private static final Map<UUID, Boolean> CLIENT_FORCED_SNEAK_STATES = new HashMap<>();

    private static final class AttractState {
        private ResourceLocation powerId;
        private int ticks;
        private Entity target;

        private AttractState(ResourceLocation powerId) {
            this.powerId = powerId;
        }
    }

    private MovementPowerService() { }

    public static void tick(Player player) {
        final boolean[] hasAttraction = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            switch (FormPowerRegistry.typeOf(power)) {
                case "shape-shifter-curse:projectile_dodge" -> dodgeProjectiles(player, id, power);
                case "shape-shifter-curse:attract_by_entity" -> {
                    if (FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                        hasAttraction[0] = true;
                        attractEntity(player, id, power);
                    }
                }
                default -> { }
            }
        });
        if (!hasAttraction[0]) ATTRACT_STATES.remove(player.getUUID());
    }

    private static void dodgeProjectiles(Player player, ResourceLocation powerId, JsonObject power) {
        if (player.isSpectator()
                || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))
                || !FormPowerRuntime.test(player, player, power.getAsJsonObject("entity_condition"))) return;
        DodgeKey key = new DodgeKey(player.getUUID(), powerId);
        double range = FormPowerRuntime.doubleValue(power, "range", 5.0D);
        double triggerDistance = FormPowerRuntime.doubleValue(power, "trigger_distance", 4.0D);
        var projectiles = player.level().getEntitiesOfClass(Projectile.class,
                player.getBoundingBox().inflate(range), candidate -> candidate.getOwner() != player
                        && !candidate.isRemoved() && !candidate.onGround()
                        && (candidate.xo == 0.0D || new Vec3(candidate.xo, candidate.yo, candidate.zo)
                        .distanceTo(candidate.position()) >= 0.1D));
        // Fabric advances this power's timer only after its condition and projectile query.
        DODGE_COOLDOWNS.computeIfPresent(key, (id, ticks) -> ticks <= 1 ? null : ticks - 1);
        if (DODGE_COOLDOWNS.containsKey(key)) return;
        for (Projectile projectile : projectiles) {
            Vec3 velocity = projectile.getDeltaMovement();
            if (projectile.position().distanceTo(player.position()) > triggerDistance) continue;
            Vec3 normVel = velocity.lengthSqr() < 1e-8 ? Vec3.ZERO : velocity.normalize();
            Vec3 towardPlayer = player.position().subtract(projectile.position());
            double len2 = towardPlayer.lengthSqr();
            Vec3 towardNorm = len2 < 1e-8 ? Vec3.ZERO : towardPlayer.normalize();
            if (normVel.dot(towardNorm) <= 0.7D) continue;
            boolean right = !DODGE_RIGHT.getOrDefault(key, true);
            DODGE_RIGHT.put(key, right);
            double horizLen2 = velocity.x*velocity.x + velocity.z*velocity.z;
            Vec3 horizontal = horizLen2 < 1e-8 ? Vec3.ZERO : new Vec3(velocity.x, 0.0D, velocity.z).normalize();
            Vec3 dodge = right ? new Vec3(-horizontal.z, 0.0D, horizontal.x) : new Vec3(horizontal.z, 0.0D, -horizontal.x);
            player.push(dodge.x * FormPowerRuntime.doubleValue(power, "dodge_speed", 1.0D), 0.0D,
                    dodge.z * FormPowerRuntime.doubleValue(power, "dodge_speed", 1.0D));
            markMotionForOwner(player);
            FormPowerRuntime.execute(player, player, power.getAsJsonObject("action"));
            DODGE_COOLDOWNS.put(key, Math.max(0, FormPowerRuntime.intValue(power, "cooldown", 20)));
            break;
        }
    }

    /** Apoli changes travel's gravity value before physics, on both logical sides. */
    public static double modifyFallingGravity(Player player, double gravity) {
        if (player.getDeltaMovement().y > 0.0D) return gravity;
        java.util.List<JsonObject> modifiers = new java.util.ArrayList<>();
        final boolean[] preventFallDamage = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:modify_falling".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            if (!FormPowerRuntime.booleanValue(power, "take_fall_damage", true)) {
                preventFallDamage[0] = true;
            }
            if (power.has("velocity")) {
                JsonObject velocity = new JsonObject();
                velocity.addProperty("operation", "set_total");
                velocity.add("value", power.get("velocity"));
                modifiers.add(velocity);
            }
            if (power.has("modifier") && power.get("modifier").isJsonObject()) {
                modifiers.add(power.getAsJsonObject("modifier"));
            }
            if (power.has("modifiers") && power.get("modifiers").isJsonArray()) {
                for (JsonElement modifier : power.getAsJsonArray("modifiers")) {
                    if (modifier.isJsonObject()) modifiers.add(modifier.getAsJsonObject());
                }
            }
        });
        if (preventFallDamage[0]) player.fallDistance = 0.0F;
        return FormPowerRuntime.applyModifierList(player, gravity, modifiers);
    }

    /**
     * Mirrors Fabric's PowerHolderComponent.hasPower: this checks whether the current form
     * assigns the power, without evaluating its optional runtime condition.
     */
    public static boolean hasAlwaysSprintSwimmingPower(Player player) {
        final boolean[] force = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!force[0] && "shape-shifter-curse:always_sprint_swimming".equals(FormPowerRegistry.typeOf(power))) {
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
     * {@code apoli:sneaking} checks. Fabric's helper returns false in water; Forge
     * uses the server-synced result on clients so air/pose condition timing cannot diverge.
     */
    // TODO[TEST] Newly wired into pose + apoli:sneaking; verify the axolotl no-air / head-collide
    //   forced crawl behaves like Fabric.
    public static boolean shouldForceSneaking(Player player) {
        if (Boolean.TRUE.equals(FORCE_SNEAK_GUARD.get())) return false;
        if (player.isUnderWater() || player.isInWaterOrBubble()) return false;
        if (player.level().isClientSide) {
            Boolean synced = CLIENT_FORCED_SNEAK_STATES.get(player.getUUID());
            if (synced != null) return synced;
        }
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

    /** Sends authoritative forced-sneak state only when the condition changes. */
    public static void synchronizeForcedSneakingIfChanged(ServerPlayer player) {
        boolean forced = shouldForceSneaking(player);
        Boolean previous = SERVER_FORCED_SNEAK_STATES.put(player.getUUID(), forced);
        if (previous == null || previous != forced) {
            net.onixary.shapeShifterCurseForge.network.ModNetwork.sendForcedSneakingSync(player, forced);
        }
    }

    /** Immediately refreshes forced-sneak state after a form transition or login. */
    public static void synchronizeForcedSneaking(ServerPlayer player) {
        boolean forced = shouldForceSneaking(player);
        SERVER_FORCED_SNEAK_STATES.put(player.getUUID(), forced);
        net.onixary.shapeShifterCurseForge.network.ModNetwork.sendForcedSneakingSync(player, forced);
    }

    /** Sends current state to one client that has just started tracking the player. */
    public static void synchronizeForcedSneakingTo(ServerPlayer target, ServerPlayer receiver) {
        boolean forced = shouldForceSneaking(target);
        SERVER_FORCED_SNEAK_STATES.put(target.getUUID(), forced);
        net.onixary.shapeShifterCurseForge.network.ModNetwork.sendForcedSneakingSyncTo(target, receiver, forced);
    }

    /** Applies the server's condition result on the client for pose and crawl animation decisions. */
    public static void applySyncedForcedSneaking(Player player, boolean forced) {
        if (player.level().isClientSide) {
            CLIENT_FORCED_SNEAK_STATES.put(player.getUUID(), forced);
        }
    }

    public static void clearClientForcedSneakingStates() {
        CLIENT_FORCED_SNEAK_STATES.clear();
    }

    public static void clearForcedSneakingState(Player player) {
        SERVER_FORCED_SNEAK_STATES.remove(player.getUUID());
        CLIENT_FORCED_SNEAK_STATES.remove(player.getUUID());
    }

    /** Shift-held or keep_sneaking-forced sneak, for pose and speed decisions. */
    public static boolean isSneakingOrForced(Player player) {
        return player.isShiftKeyDown() || shouldForceSneaking(player);
    }

    private static void attractEntity(Player player, ResourceLocation powerId, JsonObject power) {
        if (player.isSpectator()) return;
        AttractState state = ATTRACT_STATES.computeIfAbsent(player.getUUID(), ignored -> new AttractState(powerId));
        if (!state.powerId.equals(powerId)) {
            state.powerId = powerId;
            state.ticks = 0;
            state.target = null;
        }
        if (state.ticks++ % 5 == 0) {
            state.target = null;
            double radius = FormPowerRuntime.doubleValue(power, "attraction_radius", 8.0D);
            double stop = FormPowerRuntime.doubleValue(power, "stop_radius", 1.0D);
            double closestDistance = Double.MAX_VALUE;
            AABB searchBox = new AABB(player.position(), player.position()).inflate(radius);
            for (Entity candidate : player.level().getEntities(player, searchBox,
                    entity -> entity.isAlive() && !entity.isSpectator()
                            && FormPowerRuntime.test(player, entity, power.getAsJsonObject("entity_condition")))) {
                double distance = candidate.distanceToSqr(player);
                if (distance < closestDistance) {
                    state.target = candidate;
                    closestDistance = distance;
                }
            }
            if (state.target != null && closestDistance < stop * stop) state.target = null;
            Entity vehicle = player.getVehicle();
            if (!player.onGround() || vehicle instanceof Boat || vehicle instanceof AbstractMinecart
                    || (vehicle != null && (vehicle.getType().toShortString().contains("vehicle")
                    || vehicle.getType().toShortString().contains("mount")))) {
                state.target = null;
            }
            if (state.target != null && player.level().clip(new ClipContext(player.getEyePosition(),
                    state.target.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    player)).getType() == HitResult.Type.BLOCK) {
                state.target = null;
            }
        }
        Entity closest = state.target;
        if (closest == null || closest.level() != player.level() || !closest.isAlive() || closest.isRemoved()) {
            state.target = null;
            return;
        }
        Vec3 direction = new Vec3(closest.getX() - player.getX(), 0.0D, closest.getZ() - player.getZ()).normalize();
        double speed = FormPowerRuntime.doubleValue(power, "attraction_speed", 0.1D);
        Vec3 facing = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        if (facing.dot(direction) < 0.0D) speed = FormPowerRuntime.doubleValue(power, "escape_attraction_speed", 0.025D);
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
