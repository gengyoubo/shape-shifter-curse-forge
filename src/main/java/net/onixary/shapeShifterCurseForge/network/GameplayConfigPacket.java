package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.other.config.GameplayConfigState;
import java.util.function.Supplier;

public record GameplayConfigPacket(boolean foodHabit, boolean fullStack, boolean shiftCrawl) {
    public static void encode(GameplayConfigPacket packet, FriendlyByteBuf buffer) { buffer.writeBoolean(packet.foodHabit); buffer.writeBoolean(packet.fullStack); buffer.writeBoolean(packet.shiftCrawl); }
    public static GameplayConfigPacket decode(FriendlyByteBuf buffer) { return new GameplayConfigPacket(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean()); }
    public static void handle(GameplayConfigPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> GameplayConfigState.setClient(packet.foodHabit, packet.fullStack, packet.shiftCrawl));
        context.setPacketHandled(true);
    }
}
