package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.codex.NormalFormSelectScreen;

import java.util.UUID;
import java.util.function.Supplier;

/** Server to client: open the form select menu for acting on a target player. */
public record OpenSelectFormPacket(String targetName, UUID targetUUID) {
    public static void encode(OpenSelectFormPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.targetName, 256);
        buffer.writeUUID(packet.targetUUID);
    }

    public static OpenSelectFormPacket decode(FriendlyByteBuf buffer) {
        return new OpenSelectFormPacket(buffer.readUtf(256), buffer.readUUID());
    }

    public static void handle(OpenSelectFormPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft.getInstance().setScreen(new NormalFormSelectScreen(
                    net.minecraft.network.chat.Component.literal("FormSelectScreen"),
                    packet.targetName, packet.targetUUID));
        }));
        context.setPacketHandled(true);
    }
}
