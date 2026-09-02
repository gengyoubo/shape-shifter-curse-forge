package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;

import java.util.function.Supplier;

/** Client input only; every power is validated and executed by the server. */
public record ActivePowerKeyPacket(String key, boolean pressed) {
    public static void encode(ActivePowerKeyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.key, 96);
        buffer.writeBoolean(packet.pressed);
    }

    public static ActivePowerKeyPacket decode(FriendlyByteBuf buffer) {
        return new ActivePowerKeyPacket(buffer.readUtf(96), buffer.readBoolean());
    }

    public static void handle(ActivePowerKeyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                FormActivePowerService.setKeyPressed(context.getSender(), packet.key, packet.pressed);
            }
        });
        context.setPacketHandled(true);
    }
}
