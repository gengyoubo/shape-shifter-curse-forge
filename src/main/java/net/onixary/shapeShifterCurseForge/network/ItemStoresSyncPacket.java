package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Owner-only snapshot of persisted virtual item slots. */
public record ItemStoresSyncPacket(CompoundTag stores) {
    public static void encode(ItemStoresSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.stores);
    }

    public static ItemStoresSyncPacket decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new ItemStoresSyncPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(ItemStoresSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> net.onixary.shapeShifterCurseForge.client.ClientPacketHandlers
                        .setItemStores(packet.stores)));
        context.setPacketHandled(true);
    }
}
