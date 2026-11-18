package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.items.RippleMirror;

import java.util.function.Supplier;

/** Survival mirror request, deliberately separate from the admin SetFormPacket. */
public record SelectMirrorFormPacket(ResourceLocation formId) {
    public static void encode(SelectMirrorFormPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.formId);
    }

    public static SelectMirrorFormPacket decode(FriendlyByteBuf buffer) {
        return new SelectMirrorFormPacket(buffer.readResourceLocation());
    }

    public static void handle(SelectMirrorFormPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                RippleMirror.selectForm(context.getSender(), packet.formId);
            }
        });
        context.setPacketHandled(true);
    }
}
