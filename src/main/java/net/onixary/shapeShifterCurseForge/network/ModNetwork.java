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
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ShapeShifterCurseForge.RESOURCE_NAMESPACE, "main"),
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
    }

    public static void sendFormSync(ServerPlayer player) {
        player.getCapability(ModCapabilities.PLAYER_FORM).ifPresent(data -> CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                packetFor(player, data)
        ));
    }

    public static void sendFormSyncTo(ServerPlayer target, ServerPlayer receiver) {
        target.getCapability(ModCapabilities.PLAYER_FORM).ifPresent(data -> CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> receiver),
                packetFor(target, data)
        ));
    }

    private static SyncFormPacket packetFor(ServerPlayer player, net.onixary.shapeShifterCurseForge.capability.IPlayerFormData data) {
        return new SyncFormPacket(player.getId(), data.getFormId(), data.getFormGroupId(),
                data.getFormTier(), data.isContentEnabled());
    }
}
