package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.ClientPacketHandlers;

import java.util.function.Supplier;

/** Server-authoritative dynamic keep_sneaking condition for client pose and crawl animation. */
public record SyncForcedSneakingPacket(int entityId, boolean forced) {
    public static void encode(SyncForcedSneakingPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId());
        buffer.writeBoolean(packet.forced());
    }

    public static SyncForcedSneakingPacket decode(FriendlyByteBuf buffer) {
        return new SyncForcedSneakingPacket(buffer.readInt(), buffer.readBoolean());
    }

    public static void handle(SyncForcedSneakingPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.applyForcedSneaking(packet.entityId(), packet.forced())));
        context.setPacketHandled(true);
    }
}
