package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.ClientPacketHandlers;
import java.util.function.Supplier;

public record ChargeStatePacket(ResourceLocation id, boolean charging) {
    public static void encode(ChargeStatePacket packet, FriendlyByteBuf buffer) { buffer.writeResourceLocation(packet.id); buffer.writeBoolean(packet.charging); }
    public static ChargeStatePacket decode(FriendlyByteBuf buffer) { return new ChargeStatePacket(buffer.readResourceLocation(), buffer.readBoolean()); }
    public static void handle(ChargeStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.setChargeState(packet.id, packet.charging)));
        context.setPacketHandled(true);
    }
}
