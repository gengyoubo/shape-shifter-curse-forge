package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Owner-only server snapshot for the instinct HUD. */
public record InstinctSyncPacket(float value, float rate, boolean visible, boolean locked) {
    public static void encode(InstinctSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.value);
        buffer.writeFloat(packet.rate);
        buffer.writeBoolean(packet.visible);
        buffer.writeBoolean(packet.locked);
    }

    public static InstinctSyncPacket decode(FriendlyByteBuf buffer) {
        return new InstinctSyncPacket(buffer.readFloat(), buffer.readFloat(),
                buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(InstinctSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> net.onixary.shapeShifterCurseForge.client.ClientPacketHandlers.setInstinct(
                        packet.value, packet.rate, packet.visible, packet.locked)));
        context.setPacketHandled(true);
    }
}
