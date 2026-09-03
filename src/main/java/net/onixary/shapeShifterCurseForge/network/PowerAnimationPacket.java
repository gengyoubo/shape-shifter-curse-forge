package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.PowerAnimationClientHandler;

import java.util.function.Supplier;

/** Replicates a high-priority power animation from the server to tracking clients. */
public record PowerAnimationPacket(int entityId, String animationId, Mode mode, int durationOrCount) {
    public enum Mode { TIME, COUNT, LOOP, STOP }

    public static PowerAnimationPacket start(ResourceLocation animationId, Mode mode, int durationOrCount) {
        return new PowerAnimationPacket(-1, animationId.toString(), mode, durationOrCount);
    }

    public static PowerAnimationPacket stop() {
        return new PowerAnimationPacket(-1, "", Mode.STOP, -1);
    }

    public PowerAnimationPacket forEntity(int entityId) {
        return new PowerAnimationPacket(entityId, animationId, mode, durationOrCount);
    }

    public static void encode(PowerAnimationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeUtf(packet.animationId, 256);
        buffer.writeEnum(packet.mode);
        buffer.writeVarInt(packet.durationOrCount);
    }

    public static PowerAnimationPacket decode(FriendlyByteBuf buffer) {
        return new PowerAnimationPacket(buffer.readInt(), buffer.readUtf(256), buffer.readEnum(Mode.class),
                buffer.readVarInt());
    }

    public static void handle(PowerAnimationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> PowerAnimationClientHandler.apply(packet)));
        context.setPacketHandled(true);
    }
}
