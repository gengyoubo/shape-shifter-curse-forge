package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.ClientPacketHandlers;

import java.util.UUID;
import java.util.function.Supplier;

/** Authoritative wall/ceiling attachment state for the owner and tracking clients. */
public record BatAttachStatePacket(UUID playerId, BlockPos pos, Direction side) {
    public static void encode(BatAttachStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId());
        buffer.writeBoolean(packet.pos() != null && packet.side() != null);
        if (packet.pos() != null && packet.side() != null) {
            buffer.writeBlockPos(packet.pos());
            buffer.writeEnum(packet.side());
        }
    }

    public static BatAttachStatePacket decode(FriendlyByteBuf buffer) {
        UUID playerId = buffer.readUUID();
        if (!buffer.readBoolean()) return new BatAttachStatePacket(playerId, null, null);
        return new BatAttachStatePacket(playerId, buffer.readBlockPos(), buffer.readEnum(Direction.class));
    }

    public static void handle(BatAttachStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.setBatAttachState(packet.playerId(), packet.pos(), packet.side())));
        context.setPacketHandled(true);
    }
}
