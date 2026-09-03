package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.client.render.BedrockAnimationPlayer;
import net.onixary.shapeShifterCurseForge.client.render.FormAnimationSystem;
import net.onixary.shapeShifterCurseForge.network.PowerAnimationPacket;

import java.util.HashMap;
import java.util.Map;

/** Client playback clock for server-synchronised high-priority power animations. */
public final class PowerAnimationClientHandler {
    private static final Map<Integer, Playback> PLAYBACKS = new HashMap<>();

    private PowerAnimationClientHandler() {
    }

    public static void apply(PowerAnimationPacket packet) {
        if (packet.mode() == PowerAnimationPacket.Mode.STOP) {
            PLAYBACKS.remove(packet.entityId());
            return;
        }
        ResourceLocation animationId = ResourceLocation.tryParse(packet.animationId());
        if (animationId == null) return;
        Entity entity = Minecraft.getInstance().level == null ? null
                : Minecraft.getInstance().level.getEntity(packet.entityId());
        Player player = entity instanceof Player candidate ? candidate : null;
        double startedAt = player == null ? 0.0D : player.tickCount + Minecraft.getInstance().getFrameTime();
        PLAYBACKS.put(packet.entityId(), new Playback(animationId, packet.mode(), packet.durationOrCount(), startedAt));
    }

    public static ActiveAnimation active(Player player, float partialTick) {
        Playback playback = PLAYBACKS.get(player.getId());
        if (playback == null) return null;
        FormAnimationSystem.Selection selection = FormAnimationSystem.powerSelection(player, playback.animationId());
        if (selection == null) {
            PLAYBACKS.remove(player.getId());
            return null;
        }
        double elapsed = player.tickCount + partialTick - playback.startedAt();
        if (playback.mode() == PowerAnimationPacket.Mode.TIME && elapsed >= playback.durationOrCount()) {
            PLAYBACKS.remove(player.getId());
            return null;
        }
        float time = (float) (elapsed / 20.0D);
        if (playback.mode() == PowerAnimationPacket.Mode.COUNT) {
            float length = BedrockAnimationPlayer.animationLength(selection);
            if (length <= 0.0F || time >= length * playback.durationOrCount()) {
                PLAYBACKS.remove(player.getId());
                return null;
            }
        }
        boolean forceLoop = playback.mode() == PowerAnimationPacket.Mode.LOOP
                || playback.mode() == PowerAnimationPacket.Mode.COUNT;
        return new ActiveAnimation(selection, time, forceLoop);
    }

    public static void clear() {
        PLAYBACKS.clear();
    }

    private record Playback(ResourceLocation animationId, PowerAnimationPacket.Mode mode,
                            int durationOrCount, double startedAt) { }

    public record ActiveAnimation(FormAnimationSystem.Selection selection, float timeSeconds, boolean forceLoop) { }
}
