package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
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

    /** Applies a per-form default color on form change, mirroring onClientFormChange. */
    private static void applyDefaultFormColor(ResourceLocation form) {
        net.onixary.shapeShifterCurseForge.client.color.FormColorData data =
                net.onixary.shapeShifterCurseForge.client.color.FormColorData.client();
        if (!data.enableDefaultFormColor
                || !net.onixary.shapeShifterCurseForge.config.SscClientConfig
                        .CUSTOM_ENABLE_FORM_DEFAULT_COLOR_SYSTEM.get()) {
            return;
        }
        net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils.ColorSetting def =
                data.formDefaultSetting.get(form);
        if (def == null) {
            return;
        }
        net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils.ColorSetting abgr =
                net.onixary.shapeShifterCurseForge.client.color.FormColorData.argb2Abgr(def);
        net.onixary.shapeShifterCurseForge.network.ModNetwork.CHANNEL.sendToServer(
                new net.onixary.shapeShifterCurseForge.network.UpdateSkinPacket(false, false, false,
                        abgr.primaryColor(), abgr.accentColor1(), abgr.accentColor2(),
                        abgr.eyeColorA(), abgr.eyeColorB(), abgr.primaryGreyReverse(),
                        abgr.accent1GreyReverse(), abgr.accent2GreyReverse()));
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
            ResourceLocation syncedForm = ResourceLocation.tryParse(packet.formId());
            if (syncedForm != null) {
                net.onixary.shapeShifterCurseForge.client.color.FormColorData.client().unlockForm(syncedForm);
                applyDefaultFormColor(syncedForm);
            }
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
