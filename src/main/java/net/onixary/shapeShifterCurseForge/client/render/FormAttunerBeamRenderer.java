package net.onixary.shapeShifterCurseForge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.blockentity.FormAttunerBlockEntity;

/** Forge rendering counterpart to Fabric's FormAttunerBeamRenderer. */
public final class FormAttunerBeamRenderer implements BlockEntityRenderer<FormAttunerBlockEntity> {
    private static final ResourceLocation BEAM_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/beacon_beam.png");

    public FormAttunerBeamRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FormAttunerBlockEntity attuner, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (attuner.getLevel() == null) {
            return;
        }
        long gameTime = attuner.getLevel().getGameTime();
        int heightOffset = 0;
        var segments = attuner.getBeamSegments();
        for (int i = 0; i < segments.size(); i++) {
            FormAttunerBlockEntity.BeamSegment segment = segments.get(i);
            int renderHeight = i == segments.size() - 1 ? BeaconRenderer.MAX_RENDER_Y : segment.height();
            BeaconRenderer.renderBeaconBeam(poseStack, buffers, BEAM_TEXTURE, partialTick, 1.0F,
                    gameTime, heightOffset, renderHeight, segment.color(), 0.2F, 0.25F);
            heightOffset += segment.height();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(FormAttunerBlockEntity attuner) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
