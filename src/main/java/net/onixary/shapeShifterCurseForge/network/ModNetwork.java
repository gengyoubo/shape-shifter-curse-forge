package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.api.PlayerFormData;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.api.PlayerSkinData;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.util.Optional;
import java.util.Set;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "3";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

    // TODO[C-S] Packets are registered on both dists during mod construction. Client-bound handlers
    //   must not reference client-only classes (Screens/Minecraft) directly, or the dedicated server
    //   throws "invalid dist DEDICATED_SERVER" while registering them. Route client work through
    //   net.onixary.shapeShifterCurseForge.client.ClientPacketHandlers.
    public static void initialize() {
        CHANNEL.registerMessage(
                0,
                SyncFormPacket.class,
                SyncFormPacket::encode,
                SyncFormPacket::decode,
                SyncFormPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                1,
                ActivePowerKeyPacket.class,
                ActivePowerKeyPacket::encode,
                ActivePowerKeyPacket::decode,
                ActivePowerKeyPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                2,
                PowerAnimationPacket.class,
                PowerAnimationPacket::encode,
                PowerAnimationPacket::decode,
                PowerAnimationPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                3,
                UpdateSkinPacket.class,
                UpdateSkinPacket::encode,
                UpdateSkinPacket::decode,
                UpdateSkinPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                4,
                SyncSkinPacket.class,
                SyncSkinPacket::encode,
                SyncSkinPacket::decode,
                SyncSkinPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                5,
                ValidateStartBookPacket.class,
                ValidateStartBookPacket::encode,
                ValidateStartBookPacket::decode,
                ValidateStartBookPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                6,
                OpenSelectFormPacket.class,
                OpenSelectFormPacket::encode,
                OpenSelectFormPacket::decode,
                OpenSelectFormPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                7,
                SetFormPacket.class,
                SetFormPacket::encode,
                SetFormPacket::decode,
                SetFormPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                8,
                ModifyFcdPacket.class,
                ModifyFcdPacket::encode,
                ModifyFcdPacket::decode,
                ModifyFcdPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                9,
                OpenColorMenuPacket.class,
                OpenColorMenuPacket::encode,
                OpenColorMenuPacket::decode,
                OpenColorMenuPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                10,
                CursedMoonSyncPacket.class,
                CursedMoonSyncPacket::encode,
                CursedMoonSyncPacket::decode,
                CursedMoonSyncPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                11,
                OpenFormAttunerPacket.class,
                OpenFormAttunerPacket::encode,
                OpenFormAttunerPacket::decode,
                OpenFormAttunerPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                12,
                UnlockPerkPacket.class,
                UnlockPerkPacket::encode,
                UnlockPerkPacket::decode,
                UnlockPerkPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                13,
                ManaSyncPacket.class,
                ManaSyncPacket::encode,
                ManaSyncPacket::decode,
                ManaSyncPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void sendFormSync(ServerPlayer player) {
        sendFormSync(player, false);
    }

    /** A true marker is sent only by FormManager after a genuine server-side form change. */
    public static void sendFormSync(ServerPlayer player, boolean playTransformAnimation) {
        SscApi.currentForm(player).ifPresent(data -> CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                packetFor(player, data, playTransformAnimation)
        ));
    }

    public static void sendFormSyncTo(ServerPlayer target, ServerPlayer receiver) {
        SscApi.currentForm(target).ifPresent(data -> CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> receiver),
                packetFor(target, data, false)
        ));
    }

    public static void sendPowerAnimation(ServerPlayer player, PowerAnimationPacket packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet.forEntity(player.getId()));
    }

    public static void sendPowerAnimationTo(ServerPlayer target, ServerPlayer receiver, PowerAnimationPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver), packet.forEntity(target.getId()));
    }

    public static void sendSkinSync(ServerPlayer player) {
        SscApi.currentSkin(player).ifPresent(data -> CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                SyncSkinPacket.forPlayer(player, data)
        ));
    }

    public static void sendSkinSyncTo(ServerPlayer target, ServerPlayer receiver) {
        SscApi.currentSkin(target).ifPresent(data -> CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> receiver),
                SyncSkinPacket.forPlayer(target, data)
        ));
    }

    public static void sendCursedMoonSync(ServerPlayer player, boolean cursedMoonDay) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CursedMoonSyncPacket(cursedMoonDay));
    }

    public static void sendManaSync(ServerPlayer player, String manaType, float mana, float maximum) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ManaSyncPacket(manaType, mana, maximum));
    }

    /** Opens the form select menu for {@code player}, acting on {@code target}. */
    public static void sendOpenSelectForm(ServerPlayer player, Player target) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenSelectFormPacket(target.getGameProfile().getName(), target.getUUID()));
    }

    public static void sendOpenFormAttuner(ServerPlayer player, int level, int maxLevel, String formGroupId) {
        sendOpenFormAttuner(player, level, maxLevel, formGroupId, "");
    }

    private static void sendOpenFormAttuner(ServerPlayer player, int level, int maxLevel, String formGroupId,
                                             String statusKey) {
        Set<String> unlocked = SscApi.currentForm(player).map(data -> data.getUnlockedPerks().stream()
                .map(ResourceLocation::toString).collect(java.util.stream.Collectors.toUnmodifiableSet())).orElse(Set.of());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenFormAttunerPacket(level, maxLevel, formGroupId == null ? "" : formGroupId, unlocked,
                        statusKey == null ? "" : statusKey));
    }

    /** Reopens/refreshes the client view after an authoritative Perk purchase. */
    public static void refreshFormAttuner(ServerPlayer player) {
        refreshFormAttuner(player, "");
    }

    public static void refreshFormAttuner(ServerPlayer player, String statusKey) {
        var attuner = net.onixary.shapeShifterCurseForge.blockentity.FormAttunerBlockEntity.getLastUsed(player);
        if (attuner == null) return;
        sendOpenFormAttuner(player, attuner.getAttunementLevel(),
                net.onixary.shapeShifterCurseForge.blockentity.FormAttunerBlockEntity.getMaxLevel(),
                SscApi.currentForm(player).map(data -> data.getFormGroupId()).orElse(""), statusKey);
    }


    private static SyncFormPacket packetFor(ServerPlayer player,
                                            PlayerFormData data,
                                            boolean playTransformAnimation) {
        return new SyncFormPacket(player.getId(), data.getFormId(), data.getPreviousFormId(), data.getFormGroupId(),
                data.getFormTier(), data.isContentEnabled(), playTransformAnimation);
    }
}
