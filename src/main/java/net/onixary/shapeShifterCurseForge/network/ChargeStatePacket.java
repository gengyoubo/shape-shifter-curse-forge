package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.ClientPacketHandlers;
import java.util.function.Supplier;

/**
 * Owner-only charge state. {@code chargeKey} is the power's {@code charge_power_id} and
 * {@code tier} the completed charge tier (Fabric's {@code ChargePower.renderTier}), which the
 * web resource bar shows.
 */
public record ChargeStatePacket(ResourceLocation id, boolean charging, String chargeKey, int tier) {
    public static void encode(ChargeStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.id);
        buffer.writeBoolean(packet.charging);
        buffer.writeUtf(packet.chargeKey);
        buffer.writeVarInt(packet.tier);
    }

    public static ChargeStatePacket decode(FriendlyByteBuf buffer) {
        return new ChargeStatePacket(buffer.readResourceLocation(), buffer.readBoolean(),
                buffer.readUtf(256), buffer.readVarInt());
    }

    public static void handle(ChargeStatePacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.setChargeState(packet.id, packet.charging, packet.chargeKey, packet.tier)));
        context.setPacketHandled(true);
    }
}
