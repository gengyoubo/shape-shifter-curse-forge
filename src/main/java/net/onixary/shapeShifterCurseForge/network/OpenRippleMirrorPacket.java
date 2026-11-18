package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record OpenRippleMirrorPacket(List<ResourceLocation> forms) {
    public static void encode(OpenRippleMirrorPacket packet, FriendlyByteBuf buffer) {
        buffer.writeCollection(packet.forms, FriendlyByteBuf::writeResourceLocation);
    }

    public static OpenRippleMirrorPacket decode(FriendlyByteBuf buffer) {
        return new OpenRippleMirrorPacket(buffer.readList(FriendlyByteBuf::readResourceLocation));
    }

    public static void handle(OpenRippleMirrorPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> net.onixary.shapeShifterCurseForge.client.ClientPacketHandlers
                        .openRippleMirror(packet.forms)));
        context.setPacketHandled(true);
    }
}
