package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;

import java.util.function.Supplier;

/**
 * Start-book confirm button (client to server, empty). Enables the mod for players
 * still on the pre-enable form, mirroring Fabric's VALIDATE_START_BOOK_BUTTON flow.
 */
public record ValidateStartBookPacket() {
    public static void encode(ValidateStartBookPacket packet, FriendlyByteBuf buffer) {
    }

    public static ValidateStartBookPacket decode(FriendlyByteBuf buffer) {
        return new ValidateStartBookPacket();
    }

    public static void handle(ValidateStartBookPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            ResourceLocation current = FormManager.current(player).id();
            if (FormRegistry.ORIGINAL_BEFORE_ENABLE.equals(current)) {
                FormManager.setForm(player, FormRegistry.ORIGINAL_SHIFTER);
                // TODO: ON_ENABLE_MOD advancement trigger once the trigger system lands.
                player.sendSystemMessage(Component.translatable(
                        "info.shape-shifter-curse.on_enable_mod"
                ).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        });
        context.setPacketHandled(true);
    }
}
