package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.ClientPacketHandlers;

import java.util.function.Supplier;

/** Server to client: open the form color menu (always V2 on Forge). */
@SuppressWarnings("EmptyMethod")
public record OpenColorMenuPacket() {
    public static void encode(OpenColorMenuPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenColorMenuPacket decode(FriendlyByteBuf buffer) {
        return new OpenColorMenuPacket();
    }

    public static void handle(OpenColorMenuPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> ClientPacketHandlers::openColorMenu));
        context.setPacketHandled(true);
    }
}