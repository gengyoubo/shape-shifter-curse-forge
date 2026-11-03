package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative implementation of the Fabric air_jump power. */
public final class AirJumpService {
    private static final String USED_KEY = "SscAirJumpsUsed";
    private static final Map<UUID, Integer> AIR_TICKS = new HashMap<>();

    private AirJumpService() { }

    public static void clearTransient(Player player) { AIR_TICKS.remove(player.getUUID()); }

    public static void tick(Player player) {
        if (player.level().isClientSide) return;
        UUID id = player.getUUID();
        if (player.onGround()) {
            AIR_TICKS.remove(id);
            // Reset every power on landing, even if a data pack temporarily removed it.
            player.getPersistentData().remove(USED_KEY);
        } else {
            AIR_TICKS.put(id, Math.min(2, AIR_TICKS.getOrDefault(id, 0) + 1));
        }
    }

    public static boolean tryJump(ServerPlayer player) {
        if (AIR_TICKS.getOrDefault(player.getUUID(), 0) < 2 || player.onGround()
                || player.isInWater() || player.isInLava() || player.isPassenger()
                || player.isFallFlying() || player.onClimbable() || player.getAbilities().flying) return false;

        final boolean[] used = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (used[0] || !"shape-shifter-curse:air_jump".equals(FormPowerRegistry.typeOf(power))
                    || !power.has("key") || !power.get("key").isJsonObject()
                    || !"key.jump".equals(FormPowerRuntime.stringValue(power.getAsJsonObject("key"), "key", ""))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            CompoundTag counts = player.getPersistentData().getCompound(USED_KEY);
            String powerId = id.toString();
            int count = counts.getInt(powerId);
            if (count >= Math.max(1, FormPowerRuntime.intValue(power, "jumps", 1))) return;
            counts.putInt(powerId, count + 1);
            player.getPersistentData().put(USED_KEY, counts);
            Vec3 velocity = player.getDeltaMovement();
            player.setDeltaMovement(velocity.x, FormPowerRuntime.doubleValue(power, "speed", 0.6), velocity.z);
            player.hurtMarked = true;
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
            FormPowerRuntime.execute(player, player, power.getAsJsonObject("jump_action"));
            used[0] = true;
        });
        return used[0];
    }
}
