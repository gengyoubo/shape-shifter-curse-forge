package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.codex.FormColorSelectMenuV2;

import java.util.function.Supplier;

/** Server to client: open the form color menu (always V2 on Forge). */
public record OpenColorMenuPacket() {
    public static void encode(OpenColorMenuPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenColorMenuPacket decode(FriendlyByteBuf buffer) {
        return new OpenColorMenuPacket();
    }

    public static void handle(OpenColorMenuPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (FormColorSelectMenuV2.instance == null && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().setScreen(new FormColorSelectMenuV2(
                        Component.literal("text.shape-shifter-curse.config.form_color_select_menu_v2"),
                        Minecraft.getInstance().screen));
            }
        }));
        context.setPacketHandled(true);
    }
}
