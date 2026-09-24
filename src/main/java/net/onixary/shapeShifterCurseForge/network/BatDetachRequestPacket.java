package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.power.BatAttachService;

import java.util.function.Supplier;

/** A jump request; the server checks its own attachment state before detaching. */
public record BatDetachRequestPacket() {
    public static void encode(BatDetachRequestPacket packet, FriendlyByteBuf buffer) {
    }

    public static BatDetachRequestPacket decode(FriendlyByteBuf buffer) {
        return new BatDetachRequestPacket();
    }

    public static void handle(BatDetachRequestPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) BatAttachService.detachForJump(context.getSender());
        });
        context.setPacketHandled(true);
    }
}
