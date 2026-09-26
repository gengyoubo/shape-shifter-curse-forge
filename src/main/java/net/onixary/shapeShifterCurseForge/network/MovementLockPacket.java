package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.ClientPacketHandlers;

import java.util.function.Supplier;

/** Re-locks the local player's movement/jump for the current transform phase. */
public record MovementLockPacket(int noMoveTicks, int noJumpTicks) {
    public static void encode(MovementLockPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.noMoveTicks);
        buffer.writeInt(packet.noJumpTicks);
    }

    public static MovementLockPacket decode(FriendlyByteBuf buffer) {
        return new MovementLockPacket(buffer.readInt(), buffer.readInt());
    }

    public static void handle(MovementLockPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.applyMovementLock(packet.noMoveTicks(), packet.noJumpTicks())));
        context.setPacketHandled(true);
    }
}
