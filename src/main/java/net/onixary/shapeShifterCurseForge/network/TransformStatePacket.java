package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.ClientPacketHandlers;

import java.util.function.Supplier;

/** Authoritative transform-state broadcast: which player is transforming from/to what form. */
public record TransformStatePacket(int entityId, boolean transforming, String startFormId, String endFormId) {
    public static void encode(TransformStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeBoolean(packet.transforming);
        buffer.writeUtf(packet.startFormId == null ? "" : packet.startFormId);
        buffer.writeUtf(packet.endFormId == null ? "" : packet.endFormId);
    }

    public static TransformStatePacket decode(FriendlyByteBuf buffer) {
        return new TransformStatePacket(
                buffer.readInt(),
                buffer.readBoolean(),
                buffer.readUtf(256),
                buffer.readUtf(256)
        );
    }

    public static void handle(TransformStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.setTransformState(
                        packet.entityId(), packet.transforming(), packet.startFormId(), packet.endFormId())));
        context.setPacketHandled(true);
    }
}
