package net.onixary.shapeShifterCurseForge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.util.Accessory.AccessoryUtils;

import java.util.Set;

/**
 * Port of Fabric's {@code ThirdPersonExtraHandItemRender} with {@code SpiderTPEHR}: Spider 2
 * and 3 show the item in their {@code extra_hand} accessory slot in third person.
 *
 * <p>The item is anchored to the vanilla body part, which already carries the form pose and
 * root transforms shared with the Geo overlay, so it stays on the spider's body.</p>
 */
public final class SpiderExtraHandItemLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final Set<String> FORMS = Set.of("spider_2", "spider_3");
    private static final String SLOT_GROUP = "hand";
    private static final String SLOT_NAME = "extra_hand";

    public SpiderExtraHandItemLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!FORMS.contains(FormManager.current(player).id().getPath())) return;
        ItemStack stack = AccessoryUtils.getEntitySlot(player, "auto", SLOT_GROUP, SLOT_NAME, 0);
        if (stack == null || stack.isEmpty()) return;

        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        // Offsets copied verbatim from Fabric's SpiderTPEHR.
        if (stack.is(Items.SHIELD)) {
            poseStack.translate(0.1F, 0.2F, -0.2F);
            poseStack.scale(0.65F, 0.65F, 0.65F);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        } else {
            poseStack.translate(-0.1F, 0.6F, -0.5F);
            poseStack.scale(0.8F, 0.8F, 0.8F);
            poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(15.0F));
        }
        poseStack.translate(0.0625D, -0.125D, 0.0625D);
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(
                player, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
