package net.onixary.shapeShifterCurseForge.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.ManaClientState;

/** Owner-only sync for the active mana pool used by the HUD and local feedback. */
public record ManaSyncPacket(String manaType, float mana, float maximum) {
    public static void encode(ManaSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.manaType);
        buffer.writeFloat(packet.mana);
        buffer.writeFloat(packet.maximum);
    }

    public static ManaSyncPacket decode(FriendlyByteBuf buffer) {
        return new ManaSyncPacket(buffer.readUtf(256), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(ManaSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ManaClientState.set(packet.manaType, packet.mana, packet.maximum)));
        context.setPacketHandled(true);
    }
}
