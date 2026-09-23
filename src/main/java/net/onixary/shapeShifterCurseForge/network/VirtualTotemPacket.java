package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: play the totem activation animation with a specific (possibly virtual) stack. */
public record VirtualTotemPacket(ItemStack stack) {
    public static void encode(VirtualTotemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeItem(packet.stack);
    }

    public static VirtualTotemPacket decode(FriendlyByteBuf buffer) {
        return new VirtualTotemPacket(buffer.readItem());
    }

    public static void handle(VirtualTotemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> net.onixary.shapeShifterCurseForge.client.ClientPacketHandlers.displayTotem(packet.stack)));
        context.setPacketHandled(true);
    }
}