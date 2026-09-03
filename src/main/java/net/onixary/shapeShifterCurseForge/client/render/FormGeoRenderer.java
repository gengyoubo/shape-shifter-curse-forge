package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

public final class FormGeoRenderer extends GeoObjectRenderer<FormGeoAnimatable> {
    private final FormGeoAnimatable formAnimatable;

    public FormGeoRenderer(ResourceLocation model, ResourceLocation texture, ResourceLocation animationConfig) {
        super(new FormGeoModel(model, texture, animationConfig));
        this.formAnimatable = new FormGeoAnimatable();
        this.animatable = formAnimatable;
    }

    /**
     * GeckoLib nulls the inherited field in {@code doPostRenderCleanup} after every
     * render, which used to leave downstream readers (setupAnim / setupRotations hooks,
     * first-person pass) with a null animatable. The form animatable is final and
     * per-renderer state is keyed per player anyway, so always hand it out.
     */
    @Override
    public FormGeoAnimatable getAnimatable() {
        return formAnimatable;
    }

    public void setPlayer(Player player) {
        this.formAnimatable.setPlayer(player);
        this.animatable = formAnimatable;
    }

    public void setVanillaPlayerModel(PlayerModel<?> vanillaPlayerModel) {
        this.formAnimatable.setVanillaPlayerModel(vanillaPlayerModel);
    }

    public void prepareVanillaPlayerPose(float partialTick) {
        this.formAnimatable.prepareVanillaPlayerPose(partialTick);
    }

    public void setInventoryPreview(boolean inventoryPreview) {
        this.formAnimatable.setInventoryPreview(inventoryPreview);
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
