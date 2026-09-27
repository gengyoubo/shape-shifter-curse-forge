package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.power.PowerAnimationService;
import java.util.function.Supplier;

/** Read-only recovery request when a client player entity is first observed. */
public record RequestPowerAnimationPacket(int entityId) {
    public static void encode(RequestPowerAnimationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
    }

    public static RequestPowerAnimationPacket decode(FriendlyByteBuf buffer) {
        return new RequestPowerAnimationPacket(buffer.readVarInt());
    }

    public static void handle(RequestPowerAnimationPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer receiver = context.getSender();
            if (receiver != null && receiver.serverLevel().getEntity(packet.entityId) instanceof ServerPlayer target
                    && (receiver == target || receiver.distanceToSqr(target) <= 256 * 256)) {
                PowerAnimationService.synchronizeTo(target, receiver);
            }
        });
        context.setPacketHandled(true);
    }
}
