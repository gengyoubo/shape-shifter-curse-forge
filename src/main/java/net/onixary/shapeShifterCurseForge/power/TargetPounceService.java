package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-side fixed-direction pounce; resolve the hit only after physical contact. */
public final class TargetPounceService {
    private static final Map<UUID, State> ACTIVE = new HashMap<>();
    private record State(ResourceLocation ownerPower, LivingEntity target, Vec3 direction, int remaining, JsonObject definition) {}
    private TargetPounceService() {}

    public static void start(Player player, LivingEntity target, JsonObject action) {
        if (player.level().isClientSide || !target.isAlive()) return;
        String id = FormPowerRuntime.stringValue(action, "power", "");
        int split = id.lastIndexOf('_');
        if (split < 0) return;
        ResourceLocation parentId = ResourceLocation.tryParse(id.substring(0, split));
        var parent = parentId == null ? null : FormPowerRegistry.get(parentId);
        if (parent == null || !FormPowerRegistry.has(player, parentId)) return;
        var definition = parent.data().getAsJsonObject(id.substring(split + 1));
        if (definition == null || !"shape-shifter-curse:target_pounce".equals(FormPowerRegistry.typeOf(definition))) return;
        Vec3 direction = target.getBoundingBox().getCenter().subtract(player.getBoundingBox().getCenter()).normalize();
        if (direction.lengthSqr() < 1.0E-8) return;
        ACTIVE.put(player.getUUID(), new State(parentId, target, direction,
                FormPowerRuntime.intValue(definition, "duration", 20), definition));
    }

    public static void tick(Player player) {
        if (player.level().isClientSide) return;
        State state = ACTIVE.get(player.getUUID());
        if (state == null) return;
        LivingEntity target = state.target;
        if (!player.isAlive() || !target.isAlive() || target.level() != player.level()
                || state.remaining <= 0 || !FormPowerRegistry.has(player, state.ownerPower)) {
            stop(player); return;
        }
        if (player.getBoundingBox().inflate(.3).intersects(target.getBoundingBox())) {
            stop(player);
            FormPowerRuntime.executeBiEntity(player, target, state.definition.getAsJsonObject("bientity_action"));
            return;
        }
        double distance = player.getBoundingBox().getCenter().distanceTo(target.getBoundingBox().getCenter());
        Vec3 velocity = state.direction.scale(Math.min(FormPowerRuntime.doubleValue(state.definition, "speed", 1.5), Math.max(.1, distance - .3)));
        if (!player.level().noCollision(player, player.getBoundingBox().move(velocity))) { stop(player); return; }
        player.fallDistance = 0;
        setVelocity(player, velocity);
        ACTIVE.put(player.getUUID(), new State(state.ownerPower, target, state.direction, state.remaining - 1, state.definition));
    }
    public static void stop(Player player) {
        if (ACTIVE.remove(player.getUUID()) != null) setVelocity(player, Vec3.ZERO);
    }
    private static void setVelocity(Player player, Vec3 velocity) {
        player.setDeltaMovement(velocity); player.hurtMarked = true;
        if (player instanceof ServerPlayer server && server.connection != null && server.connection.connection.isConnected())
            server.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(server));
    }
}
