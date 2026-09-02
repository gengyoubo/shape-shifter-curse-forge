package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

public final class FormGeoRenderer extends GeoObjectRenderer<FormGeoAnimatable> {
    private final FormGeoAnimatable formAnimatable;

    public FormGeoRenderer(ResourceLocation model, ResourceLocation texture) {
        super(new FormGeoModel(model, texture));
        this.formAnimatable = new FormGeoAnimatable();
        this.animatable = formAnimatable;
    }

    public void setPlayer(Player player) {
        this.formAnimatable.setPlayer(player);
        this.animatable = formAnimatable;
    }

    @Override
    public long getInstanceId(FormGeoAnimatable animatable) {
        Player player = animatable.getPlayer();
        if (player == null) {
            return System.identityHashCode(animatable);
        }

        return player.getUUID().getMostSignificantBits() ^ player.getUUID().getLeastSignificantBits();
    }
}
