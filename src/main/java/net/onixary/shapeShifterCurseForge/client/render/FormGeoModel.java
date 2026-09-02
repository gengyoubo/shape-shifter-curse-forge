package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public final class FormGeoModel extends GeoModel<FormGeoAnimatable> {
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public FormGeoModel(ResourceLocation model, ResourceLocation texture) {
        this.model = model;
        this.texture = texture;
        this.animation = new ResourceLocation(model.getNamespace(), "animations/missing.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(FormGeoAnimatable animatable) {
        return model;
    }

    @Override
    public ResourceLocation getModelResource(FormGeoAnimatable animatable, GeoRenderer<FormGeoAnimatable> renderer) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(FormGeoAnimatable animatable) {
        return texture;
    }

    @Override
    public ResourceLocation getTextureResource(FormGeoAnimatable animatable, GeoRenderer<FormGeoAnimatable> renderer) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(FormGeoAnimatable animatable) {
        return animation;
    }
}
