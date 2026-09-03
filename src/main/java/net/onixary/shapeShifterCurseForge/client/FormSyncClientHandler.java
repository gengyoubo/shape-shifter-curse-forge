package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
        }

        Player player = minecraft.level.getPlayerByUUID(minecraft.player.getUUID());
        if (player != null && player.getId() == packet.entityId()) {
            FormManager.applySyncedForm(player, packet.formId(), packet.groupId(), packet.tier(), packet.enabled());
            // The first packet in a client world restores persisted data. It must not look
            // like a transform from the temporary default form rendered before the sync.
            if (INITIAL_SYNCED_PLAYERS.add(player.getUUID())) {
                FormAnimationSystem.prime(player);
            }
        }
    }
}
