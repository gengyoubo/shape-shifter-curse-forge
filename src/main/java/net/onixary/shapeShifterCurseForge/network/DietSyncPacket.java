package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.diet.DietInheritanceService;
import net.onixary.shapeShifterCurseForge.diet.RecipeDietGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Compact item registry IDs are Forge-synchronized before play packets arrive. */
public record DietSyncPacket(Map<Integer, Integer> entries) {
    private static final int MAX_ENTRIES = 100_000;

    public DietSyncPacket {
        entries = Map.copyOf(entries);
        if (entries.size() > MAX_ENTRIES) throw new IllegalArgumentException("Diet cache too large");
    }

    public static void encode(DietSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entries.size());
        packet.entries.forEach((id, flags) -> {
            buffer.writeVarInt(id);
            buffer.writeByte(flags);
        });
    }

    public static DietSyncPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) throw new IllegalArgumentException("Invalid diet cache size");
        Map<Integer, Integer> entries = new HashMap<>();
        for (int index = 0; index < count; index++) {
            int id = buffer.readVarInt();
            int flags = buffer.readUnsignedByte();
            if (id < 0 || (flags & ~RecipeDietGraph.ALL) != 0) {
                throw new IllegalArgumentException("Invalid diet cache entry");
            }
            entries.put(id, flags);
        }
        return new DietSyncPacket(entries);
    }

    public static void handle(DietSyncPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DietInheritanceService.acceptClientSnapshot(packet.entries));
        context.setPacketHandled(true);
    }
}
