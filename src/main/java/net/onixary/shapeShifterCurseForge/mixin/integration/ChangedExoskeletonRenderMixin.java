package net.onixary.shapeShifterCurseForge.mixin.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ltxprogrammer.changed.client.renderer.model.ExoskeletonModel;
import net.ltxprogrammer.changed.data.AccessorySlotContext;
import net.ltxprogrammer.changed.util.Cacheable;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.client.render.GeoBonePose;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Worn Changed exoskeleton on SSC forms (issue #21).
 *
 * <p>Head pieces are hidden (Four Addon parity) and the replacement human legs only show on
 * the original form. When the Geo form is drawn for the wearer, each suit part is drawn in
 * the full render matrix of its Geo bone, relative to this layer's {@code PoseStack}, with an
 * identity {@code ModelPart} pose: torso on {@code bipedBody}, arms on the arm bones, legs on
 * the leg bones and, for four-legged forms, again on the hind legs. This works for every SSC
 * model; parts line up with each limb's main cube group (see {@link GeoBonePose#partFrame}).</p>
 */
@Pseudo
@Mixin(targets = "net.ltxprogrammer.changed.client.renderer.accessory.WornExoskeletonRenderer", remap = false)
public abstract class ChangedExoskeletonRenderMixin {
    /**
     * Changed lifts a vanilla-shaped wearer by 2 world pixels so the suit's longer legs reach
     * the ground (see {@code ExoskeletonModel#animateWearerPose}). Form players aren't lifted,
     * so the suit legs reserve that length below the form's foot instead.
     */
    @Unique private static final float SSC$SUIT_LEG_EXTRA = 2.0F / 0.9375F;
    @Unique private static final GeoBonePose.PartBox SSC$SUIT_LEG =
            new GeoBonePose.PartBox(-2, 2, 0, 12 + SSC$SUIT_LEG_EXTRA, -2, 2, true);

    @Shadow @Final private Cacheable<ExoskeletonModel.VisorModel> suitVisorModel;
    @Shadow @Final private Cacheable<ExoskeletonModel.ReplacementLimbs> suitLegsModel;

    @Shadow public abstract ExoskeletonModel getModel();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void ssc$renderOnForm(AccessorySlotContext<?> context, PoseStack poseStack,
                                  RenderLayerParent<?, ?> parent, MultiBufferSource buffers,
                                  int packedLight, float limbSwing, float limbSwingAmount,
                                  float partialTick, float ageInTicks, float netHeadYaw,
                                  float headPitch, CallbackInfo ci) {
        ExoskeletonModel model = getModel();
        if (model == null) return;
        model.getHead().visible = false;
        ChangedExoskeletonModelAccessor parts = (ChangedExoskeletonModelAccessor) (Object) model;
        parts.ssc$getHeadSupport().visible = false;
        ((ChangedExoskeletonVisorAccessor) (Object) suitVisorModel.get()).ssc$getVisorHead().visible = false;

        boolean showReplacementLegs = !(context.wearer() instanceof Player player)
                || FormRegistry.ORIGINAL_BEFORE_ENABLE.equals(FormManager.current(player).id());
        ChangedExoskeletonLegsAccessor legs = (ChangedExoskeletonLegsAccessor) (Object) suitLegsModel.get();
        legs.ssc$getReplacementRoot().visible = showReplacementLegs;
        legs.ssc$getReplacementLeftLeg().visible = showReplacementLegs;
        legs.ssc$getReplacementRightLeg().visible = showReplacementLegs;

        // Without captured bones (Geo pass failed or skipped) Changed's own render runs.
        if (!(context.wearer() instanceof Player player) || !GeoBonePose.hasCapturedBones(player)) return;
        ItemStack stack = context.stack();
        model.prepareMobModel(stack, limbSwing, limbSwingAmount, partialTick);
        VertexConsumer buffer = buffers.getBuffer(model.renderType(model.getTexture(stack)));
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        Matrix4f layerPose = new Matrix4f(poseStack.last().pose());
        ssc$renderPart(parts.ssc$getTorso(), player, GeoBonePose.BODY, GeoBonePose.PartBox.BODY,
                poseStack, layerPose, buffer, packedLight, overlay);
        ssc$renderPart(parts.ssc$getRightArm(), player, GeoBonePose.RIGHT_ARM, GeoBonePose.PartBox.RIGHT_ARM,
                poseStack, layerPose, buffer, packedLight, overlay);
        ssc$renderPart(parts.ssc$getLeftArm(), player, GeoBonePose.LEFT_ARM, GeoBonePose.PartBox.LEFT_ARM,
                poseStack, layerPose, buffer, packedLight, overlay);
        ssc$renderPart(parts.ssc$getRightLeg(), player, GeoBonePose.RIGHT_LEG, SSC$SUIT_LEG,
                poseStack, layerPose, buffer, packedLight, overlay);
        ssc$renderPart(parts.ssc$getLeftLeg(), player, GeoBonePose.LEFT_LEG, SSC$SUIT_LEG,
                poseStack, layerPose, buffer, packedLight, overlay);
        ssc$renderPart(parts.ssc$getRightLeg(), player, GeoBonePose.RIGHT_HIND_LEG, SSC$SUIT_LEG,
                poseStack, layerPose, buffer, packedLight, overlay);
        ssc$renderPart(parts.ssc$getLeftLeg(), player, GeoBonePose.LEFT_HIND_LEG, SSC$SUIT_LEG,
                poseStack, layerPose, buffer, packedLight, overlay);
        ci.cancel();
    }

    @Unique
    private static void ssc$renderPart(ModelPart part, Player player, String boneName, GeoBonePose.PartBox box,
                                       PoseStack poseStack, Matrix4f layerPose, VertexConsumer buffer,
                                       int light, int overlay) {
        Matrix4f frame = GeoBonePose.partFrame(player, boneName, layerPose, box);
        if (frame == null || !part.visible) return;
        poseStack.pushPose();
        try {
            GeoBonePose.mulFrame(poseStack, frame);
            // The PoseStack carries the bone frame; the part itself stays at the origin.
            part.setPos(0.0F, 0.0F, 0.0F);
            part.setRotation(0.0F, 0.0F, 0.0F);
            part.render(poseStack, buffer, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        } finally {
            poseStack.popPose();
        }
    }
}
