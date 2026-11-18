package net.onixary.shapeShifterCurseForge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.UseAnim;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormManager;

import java.util.Set;

/**
 * Third-person held items for feral forms, Forge port of Fabric's {@code MouthItemFeature}
 * (itself modelled on vanilla's {@code FoxHeldItemLayer}): the main-hand item is carried in the
 * mouth, following the head, and drawn with the GROUND display context; the off-hand item and
 * a blocking shield sit on the back. The vanilla hand positions are hidden for feral forms by
 * {@code HideThirdPersonItemMixin}.
 */
public final class FeralMouthItemLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Fabric's FeralRenderUtils blacklist: items that must not be drawn in the mouth. */
    private static final Set<ResourceLocation> MOUTH_BLACKLIST = Set.of(
            ResourceLocation.fromNamespaceAndPath("tacz", "modern_kinetic_gun"));

    public FeralMouthItemLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (FormManager.current(player).bodyType() != FormBodyType.FERAL) return;
        // Under First-person Model the local player's mouth item comes from FeralFirstPersonItemLayer;
        // the inventory preview still shows it here, as on Fabric.
        Minecraft minecraft = Minecraft.getInstance();
        boolean inventoryPreview = minecraft.screen != null && packedLight == LightTexture.FULL_BRIGHT;
        if (!inventoryPreview && FirstPersonModelCompat.isLocalFirstPerson(player)
                && FirstPersonModelCompat.isRenderingPlayer()) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean blocking = player.isUsingItem() && player.getUseItem().getUseAnimation() == UseAnim.BLOCK;
        InteractionHand activeHand = player.getUsedItemHand();
        if (!mainHand.isEmpty()) {
            if (blocking && activeHand == InteractionHand.MAIN_HAND && mainHand.getItem() instanceof ShieldItem) {
                renderShieldOnBack(poseStack, buffer, packedLight, player, mainHand, true);
            } else {
                renderItemInMouth(poseStack, buffer, packedLight, player, mainHand);
            }
        }
        if (!offHand.isEmpty()) {
            if (blocking && activeHand == InteractionHand.OFF_HAND && offHand.getItem() instanceof ShieldItem) {
                renderShieldOnBack(poseStack, buffer, packedLight, player, offHand, false);
            } else {
                renderItemOnBack(poseStack, buffer, packedLight, player, offHand);
            }
        }
    }

    /**
     * Vanilla fox: head pivot, head roll/yaw/pitch, a mouth offset, then 90° about X, GROUND.
     * The rotation is the head's posed rotation (vanilla look plus any clip rotation), which the
     * Geo head copies, so the item stays in the mouth even when a clip turns the head.
     */
    private void renderItemInMouth(PoseStack poseStack, MultiBufferSource buffer, int light,
                                   AbstractClientPlayer player, ItemStack stack) {
        if (MOUTH_BLACKLIST.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return;
        ModelPart head = getParentModel().head;
        LongNeckHeadAlignment.SavedHead saved = LongNeckHeadAlignment.SavedHead.of(head);
        // Long-neck forms carry the item at the end of the neck (Fabric: IModifyHead_MAS).
        LongNeckHeadAlignment.apply(player, head);
        poseStack.pushPose();
        try {
            poseStack.translate(head.x / 16.0F, head.y / 16.0F, head.z / 16.0F);
            poseStack.mulPose(Axis.ZP.rotation(head.zRot));
            poseStack.mulPose(Axis.YP.rotation(head.yRot));
            poseStack.mulPose(Axis.XP.rotation(head.xRot));
            poseStack.translate(0.06F, 0.085F, -0.35D);
            poseStack.scale(1.25F, 1.25F, 1.25F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.translate(0.0625D, -0.125D, 0.0625D);
            itemRenderer().renderItem(player, stack, ItemDisplayContext.GROUND, false, poseStack, buffer, light);
        } finally {
            poseStack.popPose();
            saved.restore();
        }
    }

    private void renderShieldOnBack(PoseStack poseStack, MultiBufferSource buffer, int light,
                                    AbstractClientPlayer player, ItemStack stack, boolean leftHand) {
        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(-0.7F, 1.2F, 0.8F);
        poseStack.mulPose(Axis.XP.rotationDegrees(160.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-75.0F));
        poseStack.scale(1.2F, 1.2F, 1.2F);
        itemRenderer().renderItem(player, stack, ItemDisplayContext.FIXED, leftHand, poseStack, buffer, light);
        poseStack.popPose();
    }

    private void renderItemOnBack(PoseStack poseStack, MultiBufferSource buffer, int light,
                                  AbstractClientPlayer player, ItemStack stack) {
        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 0.5F, 0.25F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        itemRenderer().renderItem(player, stack, ItemDisplayContext.GROUND, false, poseStack, buffer, light);
        poseStack.popPose();
    }

    private static net.minecraft.client.renderer.ItemInHandRenderer itemRenderer() {
        return Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer();
    }
}
