package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.network.SyncFormPacket;

public final class FormSyncClientHandler {
    private FormSyncClientHandler() {
    }

    public static void apply(SyncFormPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        Player player = minecraft.level.getPlayerByUUID(minecraft.player.getUUID());
        if (player != null && player.getId() == packet.entityId()) {
            FormManager.applySyncedForm(player, packet.formId(), packet.groupId(), packet.tier(), packet.enabled());
        }
    }
}
