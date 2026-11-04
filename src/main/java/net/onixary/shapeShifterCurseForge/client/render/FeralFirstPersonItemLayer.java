package net.onixary.shapeShifterCurseForge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.mixin.accessor.ItemInHandRendererInvoker;

/**
 * Fabric's ExtraItemFeatureRenderer: First-person Model shows a feral form's body, so its
 * held item is drawn from this layer at the mouth, following the head. Fabric's copied item
 * renderer matches vanilla's renderArmWithItem for held items, so vanilla's is reused.
 */
public final class FeralFirstPersonItemLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    // Fabric never updates its renderer's equip state, so its item always sits at progress 1;
    // the mouth offsets below are tuned for that position.
    private static final float EQUIP_PROGRESS = 1.0F;

    public FeralFirstPersonItemLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemStack stack = player.getMainHandItem();
        if (player != minecraft.player || stack.isEmpty()
                || !minecraft.options.getCameraType().isFirstPerson()
                || FormManager.current(player).bodyType() != FormBodyType.FERAL
                || !FirstPersonModelCompat.isEnabled()
                || (minecraft.screen != null && packedLight == LightTexture.FULL_BRIGHT)) {
            return;
        }
        poseStack.pushPose();
        // Fabric adds (formEyeScale - pehkuiEyeScale) to Y here; Forge applies the form's eye
        // scale directly, so that difference is always zero.
        poseStack.translate(0.0D, -4.0D / 16.0D, -6.0D / 16.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(netHeadYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(headPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(240.0F));
        poseStack.translate(-12.0D / 16.0D, 15.0D / 16.0D, 4.0D / 16.0D);
        float pitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
        ((ItemInHandRendererInvoker) minecraft.getEntityRenderDispatcher().getItemInHandRenderer())
                .ssc$renderArmWithItem(player, partialTick, pitch, InteractionHand.MAIN_HAND,
                        player.getAttackAnim(partialTick), stack, EQUIP_PROGRESS, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
