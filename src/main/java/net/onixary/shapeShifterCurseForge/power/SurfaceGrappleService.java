package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Continuous-key web grapple with a refundable charge and collision-safe flight. */
public final class SurfaceGrappleService {
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final String READY_AT = "SscGrappleReadyAt";
    private static final String SAVED_GRAVITY = "SscGrappleSavedNoGravity";
    private static final class State {
        ResourceLocation id; JsonObject power; boolean charging, savedGravity; Vec3 destination; int flightTicks;
    }
    private SurfaceGrappleService() {}

    public static void tick(ServerPlayer player, Map<String, Boolean> keys) {
        State state = STATES.get(player.getUUID());
        if (state != null && (!player.isAlive() || player.isPassenger() || !FormPowerRegistry.has(player, state.id))) {
            cancel(player); state = null;
        }
        if (state == null) {
            final State[] found = {null};
            FormPowerRegistry.visitActive(player, (id, power) -> {
                if (found[0] != null || !"shape-shifter-curse:surface_grapple".equals(FormPowerRegistry.typeOf(power))) return;
                String key = FormPowerRuntime.stringValue(power.getAsJsonObject("key"), "key", "");
                float cost = FormPowerRuntime.floatValue(power, "mana_cost", 10);
                if (!keys.getOrDefault(key, false) || !FormActivePowerService.hasWebMana(player)
                        || !FormActivePowerService.hasMana(player, cost)
                        || player.level().getGameTime() < player.getPersistentData().getLong(READY_AT)) return;
                FormActivePowerService.consumeMana(player, cost);
                State next = new State(); next.id = id; next.power = power; next.charging = true; found[0] = next;
            });
            if (found[0] != null) { state = found[0]; STATES.put(player.getUUID(), state); }
        }
        if (state == null) return;
        if (state.charging) {
            String key = FormPowerRuntime.stringValue(state.power.getAsJsonObject("key"), "key", "");
            Vec3 destination = destination(player, state.power);
            if (keys.getOrDefault(key, false)) {
                if (destination != null && player.tickCount % 4 == 0)
                    player.serverLevel().sendParticles(ParticleTypes.END_ROD, destination.x, destination.y, destination.z, 2, .08, .08, .08, 0);
                return;
            }
            state.charging = false;
            if (destination == null) { FormActivePowerService.gainMana(player, FormPowerRuntime.floatValue(state.power, "mana_cost", 10)); STATES.remove(player.getUUID()); return; }
            state.destination = destination;
            state.savedGravity = player.isNoGravity();
            player.getPersistentData().putBoolean(SAVED_GRAVITY, state.savedGravity);
            player.setNoGravity(true);
            state.flightTicks = (int) Math.ceil(player.position().distanceTo(destination) /
                    Math.max(.1, FormPowerRuntime.doubleValue(state.power, "speed", 1.5))) + 20;
            player.getPersistentData().putLong(READY_AT, player.level().getGameTime() + FormPowerRuntime.intValue(state.power, "cooldown", 100));
        }
        Vec3 difference = state.destination.subtract(player.position());
        if (difference.lengthSqr() < .04 || --state.flightTicks <= 0) { cancel(player); return; }
        Vec3 velocity = difference.normalize().scale(Math.min(difference.length(), FormPowerRuntime.doubleValue(state.power, "speed", 1.5)));
        if (!player.level().noCollision(player, player.getBoundingBox().move(velocity))) { cancel(player); return; }
        player.fallDistance = 0; player.setDeltaMovement(velocity); player.hurtMarked = true;
        if (player.connection != null && player.connection.connection.isConnected())
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
    }
    private static Vec3 destination(ServerPlayer player, JsonObject power) {
        Vec3 start = player.getEyePosition();
        var hit = player.level().clip(new ClipContext(start, start.add(player.getLookAngle().scale(
                FormPowerRuntime.doubleValue(power, "distance", 12))), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) return null;
        Vec3 position = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()));
        return player.level().noCollision(player, player.getBoundingBox().move(position.subtract(player.position()))) ? position : null;
    }
    public static void cancel(net.minecraft.world.entity.player.Player player) {
        State state = STATES.remove(player.getUUID());
        if (state == null) return;
        if (state.charging && FormActivePowerService.hasWebMana(player)) FormActivePowerService.gainMana(player, FormPowerRuntime.floatValue(state.power, "mana_cost", 10));
        if (state.destination != null) {
            player.setNoGravity(state.savedGravity); player.getPersistentData().remove(SAVED_GRAVITY);
            player.setDeltaMovement(Vec3.ZERO); player.hurtMarked = true;
        }
    }
    public static void recoverAfterLogin(net.minecraft.world.entity.player.Player player) {
        if (player.getPersistentData().contains(SAVED_GRAVITY)) {
            player.setNoGravity(player.getPersistentData().getBoolean(SAVED_GRAVITY));
            player.getPersistentData().remove(SAVED_GRAVITY);
        }
    }
}
