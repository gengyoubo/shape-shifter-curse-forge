package net.onixary.shapeShifterCurseForge.items.armors;

import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class MorphscaleArmorRenderer extends GeoArmorRenderer<MorphScaleArmor> {
    public MorphscaleArmorRenderer() {
        super(new DefaultedItemGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, "morphscale_armor")));
    }
}