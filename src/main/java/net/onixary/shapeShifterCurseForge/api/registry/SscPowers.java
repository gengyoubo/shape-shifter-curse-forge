package net.onixary.shapeShifterCurseForge.api.registry;

import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

/**
 * Named references to SSC's reusable bundled Power definitions.
 *
 * <p>Use these with {@link PowerRegistrar#add(ResourceLocation)} rather than expressing a
 * gameplay ability as a form flag. They are identifiers for data-defined powers, not mutable
 * power instances; per-player cooldowns and state remain in the runtime layer.</p>
 */
public final class SscPowers {
    public static final ResourceLocation NIGHT_VISION = id("cat_vision");
    public static final ResourceLocation WATER_BREATHING = id("breathing_under_water");
    public static final ResourceLocation AQUA_AFFINITY = id("aqua_affinity");
    public static final ResourceLocation FALL_IMMUNITY = id("fall_immunity");

    private SscPowers() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, path);
    }
}
