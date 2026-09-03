package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.resources.ResourceLocation;
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
        if (PLAYBACKS.remove(player.getUUID()) != null) {
            ModNetwork.sendPowerAnimation(player, PowerAnimationPacket.stop());
        }
    }

    public static void stopIfMatches(ServerPlayer player, ResourceLocation... animationIds) {
        Playback playback = PLAYBACKS.get(player.getUUID());
        if (playback == null) return;
        for (ResourceLocation animationId : animationIds) {
            if (playback.animationId().equals(animationId)) {
                stop(player);
                return;
            }
        }
    }

    /** Called once per server player tick to expire time-bound clips. */
    public static void tick(ServerPlayer player) {
        Playback playback = PLAYBACKS.get(player.getUUID());
        if (playback == null || playback.mode() != PowerAnimationPacket.Mode.TIME) return;
        if (playback.remainingTicks() <= 1) {
            stop(player);
            return;
        }
        PLAYBACKS.put(player.getUUID(), playback.withRemainingTicks(playback.remainingTicks() - 1));
    }

    /** Sends a persistent looping/timed state when a new client starts tracking the player. */
    public static void synchronizeTo(ServerPlayer player, ServerPlayer receiver) {
        Playback playback = PLAYBACKS.get(player.getUUID());
        if (playback != null) {
            ModNetwork.sendPowerAnimationTo(player, receiver, PowerAnimationPacket.start(
                    playback.animationId(), playback.mode(), playback.remainingTicks()));
        }
    }

    private static void broadcast(ServerPlayer player, Playback playback) {
        ModNetwork.sendPowerAnimation(player, PowerAnimationPacket.start(playback.animationId(),
                playback.mode(), playback.remainingTicks()));
    }

    private record Playback(ResourceLocation animationId, PowerAnimationPacket.Mode mode, int remainingTicks) {
        private Playback withRemainingTicks(int remainingTicks) {
            return new Playback(animationId, mode, remainingTicks);
        }
    }
}
