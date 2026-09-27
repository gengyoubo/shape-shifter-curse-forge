package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.onixary.shapeShifterCurseForge.animation.PlaybackClock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.client.render.BedrockAnimationPlayer;
import net.onixary.shapeShifterCurseForge.client.render.FormAnimationSystem;
import net.onixary.shapeShifterCurseForge.network.PowerAnimationPacket;

import java.util.HashMap;
import java.util.Map;

/** Client playback clock for server-synchronised high-priority power animations. */
public final class PowerAnimationClientHandler {
    private static final Map<Integer, Playback> PLAYBACKS = new HashMap<>();

    private static ClientLevel playbackLevel;

    private PowerAnimationClientHandler() {
    }

    public static void apply(PowerAnimationPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        ensureLevel(level);
        if (level == null) return;
        if (packet.mode() == PowerAnimationPacket.Mode.STOP) {
            Playback current = PLAYBACKS.get(packet.entityId());
            if (packet.animationId().isEmpty() || current != null
                    && current.animationId().toString().equals(packet.animationId())) {
                PLAYBACKS.remove(packet.entityId());
            }
            return;
        }
        ResourceLocation animationId = ResourceLocation.tryParse(packet.animationId());
        if (animationId == null) return;
        double now = level.getGameTime();
        Playback previous = PLAYBACKS.get(packet.entityId());
        int duration = packet.mode() == PowerAnimationPacket.Mode.COUNT ? -1 : packet.durationOrCount();
        if (packet.refresh() && previous != null && previous.animationId().equals(animationId)
                && previous.mode() == packet.mode() && !previous.clock().expired(now)) {
            previous.clock().renew(now, duration);
            return;
        }
        PLAYBACKS.put(packet.entityId(), new Playback(animationId, packet.mode(), packet.durationOrCount(),
                new PlaybackClock(now, duration)));
    }

    public static void ensureLevel(ClientLevel level) {
        if (playbackLevel != level) {
            PLAYBACKS.clear();
            playbackLevel = level;
        }
    }

    public static void remove(int entityId) {
        PLAYBACKS.remove(entityId);
    }

    public static ActiveAnimation active(Player player, float partialTick) {
        ensureLevel(Minecraft.getInstance().level);
        Playback playback = PLAYBACKS.get(player.getId());
        if (playback == null) return null;
        FormAnimationSystem.Selection selection = FormAnimationSystem.powerSelection(player, playback.animationId());
        if (selection == null) {
            PLAYBACKS.remove(player.getId());
            return null;
        }
        double now = player.level().getGameTime() + partialTick;
        if (playback.clock().expired(now)) {
            PLAYBACKS.remove(player.getId());
            return null;
        }
        float time = playback.clock().seconds(now, selection.speed());
        if (playback.mode() == PowerAnimationPacket.Mode.COUNT) {
            float length = BedrockAnimationPlayer.animationLength(selection);
            if (length <= 0.0F || time >= length * playback.durationOrCount()) {
                PLAYBACKS.remove(player.getId());
                return null;
            }
        }
        boolean forceLoop = playback.mode() == PowerAnimationPacket.Mode.LOOP
                || playback.mode() == PowerAnimationPacket.Mode.COUNT;
        return new ActiveAnimation(selection, time, forceLoop, playback.clock());
    }

    public static void clear() {
        PLAYBACKS.clear();
    }

    private record Playback(ResourceLocation animationId, PowerAnimationPacket.Mode mode,
                            int durationOrCount, PlaybackClock clock) { }

    public record ActiveAnimation(FormAnimationSystem.Selection selection, float timeSeconds, boolean forceLoop, PlaybackClock clock) { }
}
