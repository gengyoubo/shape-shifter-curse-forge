package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.screen.FormAttunerScreen;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/** Server-authoritative request to show the temporary Form Attuner status screen. */
public record OpenFormAttunerPacket(int level, int maxLevel, String formGroupId, Set<String> unlockedPerks,
                                    String statusKey) {
    public static void encode(OpenFormAttunerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.level);
        buffer.writeVarInt(packet.maxLevel);
        buffer.writeUtf(packet.formGroupId, 256);
        buffer.writeVarInt(packet.unlockedPerks.size());
        packet.unlockedPerks.forEach(id -> buffer.writeUtf(id, 256));
        buffer.writeUtf(packet.statusKey, 256);
    }

    public static OpenFormAttunerPacket decode(FriendlyByteBuf buffer) {
        int level = buffer.readVarInt();
        int maxLevel = buffer.readVarInt();
        String formGroupId = buffer.readUtf(256);
        int size = buffer.readVarInt();
        if (size < 0 || size > 512) throw new IllegalArgumentException("Invalid Form Attuner perk count: " + size);
        Set<String> unlockedPerks = new LinkedHashSet<>();
        for (int index = 0; index < size; index++) unlockedPerks.add(buffer.readUtf(256));
        return new OpenFormAttunerPacket(level, maxLevel, formGroupId, Set.copyOf(unlockedPerks), buffer.readUtf(256));
    }

    public static void handle(OpenFormAttunerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Minecraft.getInstance().setScreen(new FormAttunerScreen(packet.level, packet.maxLevel,
                        packet.formGroupId, packet.unlockedPerks, packet.statusKey))));
        context.setPacketHandled(true);
    }
}
