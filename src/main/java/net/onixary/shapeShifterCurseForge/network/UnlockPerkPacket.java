package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.other.perk.PerkService;

import java.util.function.Supplier;

/** Client request only; all Perk validation and persistence remain on the server. */
public record UnlockPerkPacket(ResourceLocation perkId) {
    public static void encode(UnlockPerkPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.perkId);
    }

    public static UnlockPerkPacket decode(FriendlyByteBuf buffer) {
        return new UnlockPerkPacket(buffer.readResourceLocation());
    }

    public static void handle(UnlockPerkPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) return;
            PerkService.UnlockResult result = PerkService.unlock(player, packet.perkId);
            String key = "screen." + ShapeShifterCurseForge.RESOURCE_NAMESPACE + ".form_attuner.purchase."
                    + result.name().toLowerCase(java.util.Locale.ROOT);
            player.displayClientMessage(Component.translatable(key), true);
            ModNetwork.refreshFormAttuner(player, key);
        });
        context.setPacketHandled(true);
    }
}
