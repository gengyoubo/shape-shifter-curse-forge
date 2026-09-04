package net.onixary.shapeShifterCurseForge.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client to server form change request. Mirrors Fabric: packets can be forged, so
 * applying requires operator level 2 or creative mode.
 */
public record SetFormPacket(UUID targetUUID, ResourceLocation formId, boolean immediate) {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void encode(SetFormPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.targetUUID);
        buffer.writeResourceLocation(packet.formId);
        buffer.writeBoolean(packet.immediate);
    }

    public static SetFormPacket decode(FriendlyByteBuf buffer) {
        return new SetFormPacket(buffer.readUUID(), buffer.readResourceLocation(), buffer.readBoolean());
    }

    public static void handle(SetFormPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.server == null) {
                return;
            }
            ServerPlayer target = sender.server.getPlayerList().getPlayer(packet.targetUUID);
            if (target == null) {
                LOGGER.warn("[SetForm] Player {} not found", packet.targetUUID);
                return;
            }
            if (FormRegistry.get(packet.formId) == null) {
                LOGGER.warn("[SetForm] Unknown form {}", packet.formId);
                return;
            }
            if (sender.hasPermissions(2) || sender.getAbilities().instabuild) {
                FormManager.setForm(target, packet.formId, !packet.immediate);
            } else {
                LOGGER.warn("[SetForm] Player {} lacks permission to set forms", sender.getGameProfile().getName());
            }
        });
        context.setPacketHandled(true);
    }
}
