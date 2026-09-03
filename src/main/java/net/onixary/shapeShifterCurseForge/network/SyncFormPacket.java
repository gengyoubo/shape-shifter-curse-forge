package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.FormSyncClientHandler;

import java.util.function.Supplier;

/** Form state plus an explicit real-transform marker; login/respawn synchronisation never sets it. */
public record SyncFormPacket(int entityId, String formId, String previousFormId, String groupId, int tier,
                             boolean enabled, boolean playTransformAnimation) {
    public static void encode(SyncFormPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeUtf(packet.formId);
        buffer.writeUtf(packet.previousFormId);
        buffer.writeUtf(packet.groupId);
        buffer.writeInt(packet.tier);
        buffer.writeBoolean(packet.enabled);
        buffer.writeBoolean(packet.playTransformAnimation);
    }

    public static SyncFormPacket decode(FriendlyByteBuf buffer) {
        return new SyncFormPacket(
                buffer.readInt(),
                buffer.readUtf(256),
                buffer.readUtf(256),
                buffer.readUtf(256),
                buffer.readInt(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    public static void handle(SyncFormPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FormSyncClientHandler.apply(packet)));
        context.setPacketHandled(true);
    }
}
