package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ShapeShifterCurseForge.RESOURCE_NAMESPACE, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

    public static void initialize() {
        // Packet registrations will be added as Fabric networking is migrated.
    }
}
