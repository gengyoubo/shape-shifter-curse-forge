package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.animation.PlaybackClock;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.network.PowerAnimationPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative state and replication for Fabric's power-animation layer. */
public final class PowerAnimationService {
    public static final ResourceLocation ATTACH_SIDE = ResourceLocation.fromNamespaceAndPath(
            "shape-shifter-curse", "attach_side");
    public static final ResourceLocation ATTACH_BOTTOM = ResourceLocation.fromNamespaceAndPath(
            "shape-shifter-curse", "attach_bottom");

    private static final Map<UUID, Playback> PLAYBACKS = new HashMap<>();

    private PowerAnimationService() {
    }

    public static void playWithTime(ServerPlayer player, ResourceLocation animationId, int ticks) {
        if (animationId == null || ticks <= 0) return;
        Playback playback = new Playback(animationId, PowerAnimationPacket.Mode.TIME, ticks);
        PLAYBACKS.put(player.getUUID(), playback);
        broadcast(player, playback);
    }

    public static void playWithCount(ServerPlayer player, ResourceLocation animationId, int count) {
        if (animationId == null || count <= 0) return;
        PLAYBACKS.remove(player.getUUID());
        // Fabric sends count-based clips immediately and lets each receiving client finish
        // its local repeats. They intentionally do not become persistent server state.
        ModNetwork.sendPowerAnimation(player, PowerAnimationPacket.start(animationId,
                PowerAnimationPacket.Mode.COUNT, count));
    }

    public static void playLoop(ServerPlayer player, ResourceLocation animationId) {
        if (animationId == null) return;
        Playback playback = new Playback(animationId, PowerAnimationPacket.Mode.LOOP, -1);
        PLAYBACKS.put(player.getUUID(), playback);
        broadcast(player, playback);
    }

    public static void stop(ServerPlayer player) {
        PLAYBACKS.remove(player.getUUID());
        // COUNT is deliberately client-owned, so absence of a server entry is not a no-op.
        ModNetwork.sendPowerAnimation(player, PowerAnimationPacket.stop());
    }

    public static void stopIfMatches(ServerPlayer player, ResourceLocation... animationIds) {
        Playback playback = PLAYBACKS.get(player.getUUID());
        for (ResourceLocation animationId : animationIds) {
            if (animationId == null) continue;
            if (playback != null && playback.animationId().equals(animationId)) {
                PLAYBACKS.remove(player.getUUID());
            }
            // Conditional STOP also reaches client-owned COUNT without cancelling unrelated clips.
            ModNetwork.sendPowerAnimation(player, PowerAnimationPacket.stopMatching(animationId));
        }
    }

    /** TIME expires server-side; LOOP is renewed before its client lease expires. */
    public static void tick(ServerPlayer player) {
        Playback playback = PLAYBACKS.get(player.getUUID());
        if (playback == null) return;
        if (playback.mode() == PowerAnimationPacket.Mode.TIME) {
            if (playback.remainingTicks() <= 1) {
                stop(player);
                return;
            }
            playback = playback.withRemainingTicks(playback.remainingTicks() - 1);
            PLAYBACKS.put(player.getUUID(), playback);
        }
        if (player.tickCount % PlaybackClock.HEARTBEAT_TICKS == 0) {
            ModNetwork.sendPowerAnimation(player, packet(playback).asRefresh());
        }
    }

    public static void forget(ServerPlayer player) {
        PLAYBACKS.remove(player.getUUID());
    }

    /** Sends a persistent looping/timed state when a new client starts tracking the player. */
    public static void synchronizeTo(ServerPlayer player, ServerPlayer receiver) {
        Playback playback = PLAYBACKS.get(player.getUUID());
        if (playback != null) {
            ModNetwork.sendPowerAnimationTo(player, receiver, packet(playback).asRefresh());
        }
    }

    private static void broadcast(ServerPlayer player, Playback playback) {
        ModNetwork.sendPowerAnimation(player, packet(playback));
    }

    private static PowerAnimationPacket packet(Playback playback) {
        return PowerAnimationPacket.start(playback.animationId(), playback.mode(),
                playback.mode() == PowerAnimationPacket.Mode.LOOP
                        ? PlaybackClock.LOOP_LEASE_TICKS : playback.remainingTicks());
    }

    private record Playback(ResourceLocation animationId, PowerAnimationPacket.Mode mode, int remainingTicks) {
        private Playback withRemainingTicks(int remainingTicks) {
            return new Playback(animationId, mode, remainingTicks);
        }
    }
}
