package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.codex.CursedMoonData;

import java.util.function.Supplier;

/** Server-authoritative Cursed Moon day flag used by the client Codex and warning message. */
public record CursedMoonSyncPacket(boolean cursedMoonDay) {
    public static void encode(CursedMoonSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.cursedMoonDay);
    }

    public static CursedMoonSyncPacket decode(FriendlyByteBuf buffer) {
        return new CursedMoonSyncPacket(buffer.readBoolean());
    }

    public static void handle(CursedMoonSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> CursedMoonData.setServerCursedMoonDay(packet.cursedMoonDay)));
        context.setPacketHandled(true);
    }
}
