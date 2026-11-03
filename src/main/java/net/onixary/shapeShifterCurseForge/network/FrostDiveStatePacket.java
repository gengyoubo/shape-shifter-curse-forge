package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.ClientPacketHandlers;
import java.util.function.Supplier;

public record FrostDiveStatePacket(boolean diving) {
    public static void encode(FrostDiveStatePacket packet, FriendlyByteBuf buffer) { buffer.writeBoolean(packet.diving()); }
    public static FrostDiveStatePacket decode(FriendlyByteBuf buffer) { return new FrostDiveStatePacket(buffer.readBoolean()); }
    public static void handle(FrostDiveStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.applyFrostDive(packet.diving())));
        context.setPacketHandled(true);
    }
}
