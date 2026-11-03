package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server state for the snow fox's directed dive. */
public final class FrostDiveService {
    private static final Map<UUID, Dive> DIVES = new HashMap<>();
    private static final java.util.Set<UUID> CLIENT_DIVES = new java.util.HashSet<>();
    private FrostDiveService() { }
    private record Dive(ResourceLocation id, JsonObject power, net.minecraft.world.level.Level level,
                        double startY, Vec3 previous) { }

    public static boolean isDiving(Player player) {
        return player.level().isClientSide ? CLIENT_DIVES.contains(player.getUUID()) : DIVES.containsKey(player.getUUID());
    }
    public static void setClientDiving(Player player, boolean diving) {
        if (diving) CLIENT_DIVES.add(player.getUUID()); else CLIENT_DIVES.remove(player.getUUID());
        if (!diving) player.resetFallDistance();
    }
    public static void clearClient() { CLIENT_DIVES.clear(); }
    public static void clear(Player player) {
        if (DIVES.remove(player.getUUID()) != null) {
            player.resetFallDistance();
            if (player instanceof ServerPlayer serverPlayer) sendState(serverPlayer, false);
        }
    }

    public static boolean airborne(Player player) {
        return !player.onGround() && !player.isInWater() && !player.isInLava() && !player.isPassenger()
                && !player.isFallFlying() && !player.onClimbable() && !player.getAbilities().flying;
    }

    public static boolean tryStart(ServerPlayer player, String key) {
        if (isDiving(player) || !airborne(player)) return false;
        boolean[] started = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (started[0] || !"shape-shifter-curse:frost_dive".equals(FormPowerRegistry.typeOf(power))
                    || !power.has("key") || !power.get("key").isJsonObject()
                    || !key.equals(FormPowerRuntime.stringValue(power.getAsJsonObject("key"), "key", ""))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))
                    || !FormActivePowerService.usePowerCooldown(player, id, FormPowerRuntime.intValue(power, "cooldown", 160))) return;
            DIVES.put(player.getUUID(), new Dive(id, power, player.level(), player.getY(), player.position()));
            sendState(player, true);
            FormPowerRuntime.execute(player, player, power.getAsJsonObject("start_action"));
            player.resetFallDistance();
            applyVelocity(player, power);
            started[0] = true;
        });
        return started[0];
    }

    public static void tick(ServerPlayer player) {
        Dive dive = DIVES.get(player.getUUID());
        if (dive == null) return;
        player.resetFallDistance();
        if (!player.isAlive() || player.level() != dive.level() || !FormPowerRegistry.has(player, dive.id())
                || dive.previous().distanceToSqr(player.position()) > 64) { clear(player); return; }
        if (player.onGround()) {
            boolean impact = dive.startY() - player.getY() > FormPowerRuntime.doubleValue(dive.power(), "minimum_height", 4);
            clear(player);
            if (impact) FormPowerRuntime.execute(player, player, dive.power().getAsJsonObject("landing_action"));
            return;
        }
        if (!airborne(player)) { clear(player); return; }
        DIVES.put(player.getUUID(), new Dive(dive.id(), dive.power(), dive.level(), dive.startY(), player.position()));
        applyVelocity(player, dive.power());
    }

    private static void applyVelocity(ServerPlayer player, JsonObject power) {
        player.setDeltaMovement(0, -FormPowerRuntime.doubleValue(power, "speed", 0.8), 0);
        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static void sendState(ServerPlayer player, boolean diving) {
        if (player.connection != null && player.connection.connection.isConnected()) {
            net.onixary.shapeShifterCurseForge.network.ModNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new net.onixary.shapeShifterCurseForge.network.FrostDiveStatePacket(diving));
        }
    }
}
