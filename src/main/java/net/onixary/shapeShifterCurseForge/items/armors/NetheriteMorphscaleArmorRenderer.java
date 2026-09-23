package net.onixary.shapeShifterCurseForge.items.armors;

import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class NetheriteMorphscaleArmorRenderer extends GeoArmorRenderer<NetheriteMorphScaleArmor> {
    public NetheriteMorphscaleArmorRenderer() {
        super(new DefaultedItemGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, "netherite_morphscale_armor")));
    }
}