package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import java.util.function.Supplier;

public record UpdateRandomSoundPacket(boolean enabled) {
    public static void encode(UpdateRandomSoundPacket packet, FriendlyByteBuf buffer) { buffer.writeBoolean(packet.enabled); }
    public static UpdateRandomSoundPacket decode(FriendlyByteBuf buffer) { return new UpdateRandomSoundPacket(buffer.readBoolean()); }
    public static void handle(UpdateRandomSoundPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) SscApi.currentSkin(player).ifPresent(data -> {
                data.setEnableFormRandomSound(packet.enabled);
                ModNetwork.sendSkinSync(player);
            });
        });
        context.setPacketHandled(true);
    }
}
