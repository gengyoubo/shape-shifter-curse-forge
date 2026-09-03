package net.onixary.shapeShifterCurseForge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Optional;

/**
 * Held-item layer for transformed players. The vanilla player render (which carries
 * the vanilla ItemInHandLayer) is cancelled for forms, so hand items are drawn here
 * attached to the animated arm GeoBones instead.
 *
 * <p>A hand renders only while its arm bone is visible, which also keeps the
 * first-person single-arm pass correct. When the vanilla arm render survives (no
 * hiding power or config), the vanilla item is the one shown and this layer stays
 * out via the per-pass suppress flag, so items never render twice.</p>
 */
public final class HeldItemGeoLayer extends GeoRenderLayer<FormGeoAnimatable> {
    public HeldItemGeoLayer(GeoRenderer<FormGeoAnimatable> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, FormGeoAnimatable animatable, BakedGeoModel bakedGeoModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        Player player = animatable.getPlayer();
        if (player == null || animatable.suppressHeldItems()
                || !(getRenderer().getGeoModel() instanceof FormGeoModel model)) {
            return;
        }
        HumanoidArm mainArm = player.getMainArm();
        renderHandItem(poseStack, bufferSource, packedLight, player, model,
                player.getMainHandItem(), mainArm, mainArm == HumanoidArm.RIGHT);
        renderHandItem(poseStack, bufferSource, packedLight, player, model,
                player.getOffhandItem(), mainArm.getOpposite(), mainArm != HumanoidArm.RIGHT);
    }

    private static void renderHandItem(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                       Player player, FormGeoModel model, ItemStack stack,
                                       HumanoidArm side, boolean rightHandMode) {
        if (stack.isEmpty()) {
            return;
        }
        boolean rightSide = side == HumanoidArm.RIGHT;
        Optional<GeoBone> armBone = model.getBone(model.firstPersonArmBone(rightSide));
        if (armBone.isEmpty() || armBone.get().isHidden()) {
            return;
        }
        try {
            poseStack.pushPose();
            poseStack.mulPoseMatrix(armBone.get().getModelSpaceMatrix());
            // Mirrors ItemInHandLayer#renderArmWithItem after translateToHand.
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            boolean leftSide = !rightSide;
            poseStack.translate((leftSide ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack,
                    rightHandMode ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                            : ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    leftSide, poseStack, bufferSource, packedLight);
        } catch (RuntimeException ignored) {
            // A held item must never hide the owning player.
        } finally {
            poseStack.popPose();
        }
    }
}
