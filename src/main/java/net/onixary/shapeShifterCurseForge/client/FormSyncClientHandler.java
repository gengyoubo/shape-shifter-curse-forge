package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.client.render.FormAnimationSystem;
import net.onixary.shapeShifterCurseForge.network.SyncFormPacket;

import java.util.HashSet;
import java.util.Set;

public final class FormSyncClientHandler {
    private static final Set<java.util.UUID> INITIAL_SYNCED_PLAYERS = new HashSet<>();
    private static ClientLevel syncedLevel;

    private FormSyncClientHandler() {
    }

    public static void apply(SyncFormPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (syncedLevel != minecraft.level) {
            syncedLevel = minecraft.level;
            INITIAL_SYNCED_PLAYERS.clear();
            FormAnimationSystem.clearClientState();
            PowerAnimationClientHandler.clear();
        }

        Entity entity = minecraft.level.getEntity(packet.entityId());
        if (entity instanceof Player player) {
            FormManager.applySyncedForm(player, packet.formId(), packet.groupId(), packet.tier(), packet.enabled());
            // Login, respawn and tracking packets initialise the snapshot. Only a server
            // confirmed form change is allowed to start TransformingController's clip.
            if (packet.playTransformAnimation()) {
                FormAnimationSystem.startTransition(player, packet.previousFormId());
                INITIAL_SYNCED_PLAYERS.add(player.getUUID());
            } else if (INITIAL_SYNCED_PLAYERS.add(player.getUUID())) {
                FormAnimationSystem.prime(player);
            }
        }
    }
}
