package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.color.FormColorClientHandler;

import java.util.function.Supplier;

/**
 * Server to client color-slot command. Contract (both ends owned here):
 * commandType one of save/load/delete/config/list; formId is the target form
 * (placeholder for config); arg1 is scope ("form"/"global"/"form_default") or
 * value; arg2 is the slot name; arg3/arg4 reserved.
 */
public record ModifyFcdPacket(String commandType, String formId, String arg1, String arg2,
                              String arg3, String arg4) {
    public static void encode(ModifyFcdPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.commandType, 32);
        buffer.writeUtf(packet.formId, 256);
        buffer.writeUtf(packet.arg1, 256);
        buffer.writeUtf(packet.arg2, 256);
        buffer.writeUtf(packet.arg3, 256);
        buffer.writeUtf(packet.arg4, 256);
    }

    public static ModifyFcdPacket decode(FriendlyByteBuf buffer) {
        return new ModifyFcdPacket(buffer.readUtf(32), buffer.readUtf(256), buffer.readUtf(256),
                buffer.readUtf(256), buffer.readUtf(256), buffer.readUtf(256));
    }

    public static void handle(ModifyFcdPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FormColorClientHandler.applyModifyFcd(packet)));
        context.setPacketHandled(true);
    }
}
