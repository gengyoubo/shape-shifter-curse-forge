package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.onixary.shapeShifterCurseForge.client.FormSyncClientHandler;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Form state plus an explicit real-transform marker; login/respawn synchronisation never sets it. */
public record SyncFormPacket(int entityId, String formId, String previousFormId, String groupId, int tier,
                             boolean enabled, List<ResourceLocation> assignedPowerIds,
                             boolean playTransformAnimation) {
    private static final int MAX_POWER_IDS = 4096;

    public SyncFormPacket {
        assignedPowerIds = List.copyOf(assignedPowerIds);
    }

    public static void encode(SyncFormPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeUtf(packet.formId);
        buffer.writeUtf(packet.previousFormId);
        buffer.writeUtf(packet.groupId);
        buffer.writeInt(packet.tier);
        buffer.writeBoolean(packet.enabled);
        buffer.writeVarInt(packet.assignedPowerIds.size());
        for (ResourceLocation powerId : packet.assignedPowerIds) {
            buffer.writeResourceLocation(powerId);
        }
        buffer.writeBoolean(packet.playTransformAnimation);
    }

    public static SyncFormPacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        String formId = buffer.readUtf(256);
        String previousFormId = buffer.readUtf(256);
        String groupId = buffer.readUtf(256);
        int tier = buffer.readInt();
        boolean enabled = buffer.readBoolean();
        int powerCount = buffer.readVarInt();
        if (powerCount < 0 || powerCount > MAX_POWER_IDS) {
            throw new IllegalArgumentException("Invalid synced form power count: " + powerCount);
        }
        List<ResourceLocation> powerIds = new ArrayList<>(powerCount);
        for (int i = 0; i < powerCount; i++) {
            powerIds.add(buffer.readResourceLocation());
        }
        boolean playTransformAnimation = buffer.readBoolean();
        return new SyncFormPacket(
                entityId,
                formId,
                previousFormId,
                groupId,
                tier,
                enabled,
                powerIds,
                playTransformAnimation
        );
    }

    public static void handle(SyncFormPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FormSyncClientHandler.apply(packet)));
        context.setPacketHandled(true);
    }
}
