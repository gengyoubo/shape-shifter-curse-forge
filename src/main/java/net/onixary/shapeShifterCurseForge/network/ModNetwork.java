package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.capability.ModCapabilities;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.util.Optional;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

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
    }

    public static void sendFormSync(ServerPlayer player) {
        sendFormSync(player, false);
    }

    /** A true marker is sent only by FormManager after a genuine server-side form change. */
    public static void sendFormSync(ServerPlayer player, boolean playTransformAnimation) {
        player.getCapability(ModCapabilities.PLAYER_FORM).ifPresent(data -> CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                packetFor(player, data, playTransformAnimation)
        ));
    }

    public static void sendFormSyncTo(ServerPlayer target, ServerPlayer receiver) {
        target.getCapability(ModCapabilities.PLAYER_FORM).ifPresent(data -> CHANNEL.send(
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
        player.getCapability(ModCapabilities.PLAYER_SKIN).ifPresent(data -> CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                SyncSkinPacket.forPlayer(player, data)
        ));
    }

    public static void sendSkinSyncTo(ServerPlayer target, ServerPlayer receiver) {
        target.getCapability(ModCapabilities.PLAYER_SKIN).ifPresent(data -> CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> receiver),
                SyncSkinPacket.forPlayer(target, data)
        ));
    }

    private static SyncFormPacket packetFor(ServerPlayer player,
                                            net.onixary.shapeShifterCurseForge.capability.IPlayerFormData data,
                                            boolean playTransformAnimation) {
        return new SyncFormPacket(player.getId(), data.getFormId(), data.getPreviousFormId(), data.getFormGroupId(),
                data.getFormTier(), data.isContentEnabled(), playTransformAnimation);
    }
}
