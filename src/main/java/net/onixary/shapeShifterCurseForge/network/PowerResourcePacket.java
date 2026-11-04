package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.ClientPacketHandlers;
import java.util.function.Supplier;

public record PowerResourcePacket(ResourceLocation id, double value) {
    public static void encode(PowerResourcePacket packet, FriendlyByteBuf buffer) { buffer.writeResourceLocation(packet.id); buffer.writeDouble(packet.value); }
    public static PowerResourcePacket decode(FriendlyByteBuf buffer) { return new PowerResourcePacket(buffer.readResourceLocation(), buffer.readDouble()); }
    public static void handle(PowerResourcePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.setPowerResource(packet.id, packet.value)));
        context.setPacketHandled(true);
    }
}
