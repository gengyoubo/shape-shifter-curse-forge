package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.PerkClientState;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/** Syncs only the local player's Perks, allowing passive ability movement to predict client-side. */
public record SyncPerksPacket(Set<ResourceLocation> perkIds) {
    public static void encode(SyncPerksPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.perkIds.size());
        packet.perkIds.forEach(buffer::writeResourceLocation);
    }

    public static SyncPerksPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > 512) throw new IllegalArgumentException("Invalid Perk sync size: " + size);
        Set<ResourceLocation> perkIds = new LinkedHashSet<>();
        for (int index = 0; index < size; index++) perkIds.add(buffer.readResourceLocation());
        return new SyncPerksPacket(Set.copyOf(perkIds));
    }

    public static void handle(SyncPerksPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().player != null) PerkClientState.replace(packet.perkIds);
        }));
        context.setPacketHandled(true);
    }
}
