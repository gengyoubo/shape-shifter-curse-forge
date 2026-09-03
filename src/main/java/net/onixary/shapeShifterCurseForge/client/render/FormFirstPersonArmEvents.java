package net.onixary.shapeShifterCurseForge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Optional;

/**
 * Forge counterpart of Fabric's first-person form rendering
 * ({@code FormRenderFeature#rFPM_PartA}/{@code rFPM_PartB} plus
 * {@code DefaultModelAnimationSystem#beforeRenderFirstPerson}/
 * {@code processAnimationFirstPerson}).
 *
 * <p>Fabric chain, mirrored here: resolve the target GeoBone, reset it, copy the
 * vanilla first-person arm part pose onto it (position negated, arm pivot offset,
 * rotation with Y/Z inverted), then {@code renderRecursively} just that bone subtree
 * via {@code renderGeoBone}. In first person the arm motion comes from the hand
 * matrix, so the clip is deliberately NOT applied here, exactly like Fabric.</p>
 */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class FormFirstPersonArmEvents {
    private FormFirstPersonArmEvents() {
    }

    /**
     * Must run even when the event was already cancelled: the no_render_arm power
     * (ClientPowerRenderEvents) cancels first-person arms before this handler, and
     * without {@code receiveCanceled} the form arm would never render for those forms.
     */
    @SubscribeEvent(receiveCanceled = true)
    public static void renderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || player != minecraft.player
                || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        FormDefinition form = FormManager.current(player);
        if (!form.hasFlag("special_form") && form.tier() <= 0) {
            return;
        }

        FormGeoRenderer renderer = FormClientRenderEvents.rendererFor(form);
        if (renderer == null
                || !(minecraft.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer playerRenderer)) {
            return;
        }
        FormGeoModel model = (FormGeoModel) renderer.getGeoModel();
        boolean right = event.getArm() == HumanoidArm.RIGHT;
        // Mirrors rFPM_PartA: the vanilla arm is hidden when the form hides it.
        if (model.isVanillaPartHidden(right ? "rightArm" : "leftArm")) {
            event.setCanceled(true);
        }

        // Mirrors beforeRenderFirstPerson: resolve the mapped arm bone
        // (biped arms by default, overridable through first_person_render).
        BakedGeoModel baked = model.getBakedModel(model.modelResource());
        if (baked == null) {
            return;
        }
        model.getAnimationProcessor().setActiveModel(baked);
        Optional<GeoBone> armBone = model.getBone(model.firstPersonArmBone(right));
        // Mirrors the null branch: nothing to draw for this arm.
        if (armBone.isEmpty()) {
            return;
        }
        GeoBone geoBone = armBone.get();

        // Mirrors processAnimationFirstPerson: reset the bone, then copy the vanilla
        // first-person arm part pose onto it.
        PlayerModel<AbstractClientPlayer> rendererModel = playerRenderer.getModel();
        ModelPart armPart = right ? rendererModel.rightArm : rendererModel.leftArm;
        resetBone(geoBone);
        geoBone.setPosX(geoBone.getPosX() + (-armPart.x));
        geoBone.setPosY(geoBone.getPosY() + (-armPart.y));
        geoBone.setPosZ(geoBone.getPosZ() + (-armPart.z));
        geoBone.setPosX(geoBone.getPosX() + (right ? -5.0F : 5.0F));
        geoBone.setPosY(geoBone.getPosY() + 2.0F);
        geoBone.setRotX(armPart.xRot);
        geoBone.setRotY(armPart.yRot);
        geoBone.setRotZ(armPart.zRot);
        geoBone.setRotY(-geoBone.getRotY());
        geoBone.setRotZ(-geoBone.getRotZ());

        renderer.setPlayer(player);
        FormGeoAnimatable animatable = renderer.getAnimatable();
        if (animatable == null) {
            return;
        }
        float partialTick = minecraft.getFrameTime();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        try {
            // Mirrors rFPM_PartB's matrix setup.
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.translate(0.0D, -1.51D, 0.0D);
            poseStack.translate(-0.5D, -0.5D, -0.5D);
            ResourceLocation texture = renderer.getTextureLocation(animatable);
            RenderType renderType = RenderType.entityTranslucent(texture);
            VertexConsumer buffer = event.getMultiBufferSource().getBuffer(renderType);
            if (renderer.firePreRenderEvent(poseStack, baked, event.getMultiBufferSource(),
                    partialTick, event.getPackedLight())) {
                renderer.updateAnimatedTextureFrame(animatable);
                // Mirrors renderGeoBone: draw just the arm subtree, nothing else.
                renderer.renderRecursively(poseStack, animatable, geoBone, renderType,
                        event.getMultiBufferSource(), buffer, false, partialTick,
                        event.getPackedLight(), OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                renderer.firePostRenderEvent(poseStack, baked, event.getMultiBufferSource(),
                        partialTick, event.getPackedLight());
            }
        } catch (RuntimeException ignored) {
            // A first-person arm must never break hand rendering.
        } finally {
            poseStack.popPose();
        }

        // Fabric has no item layer: the vanilla hand item renders through its own path.
        // When the arm event died (power or Hidden_*), vanilla skipped the item too, so
        // draw it here on the event's hand matrix, exactly where vanilla would have.
        if (event.isCanceled()) {
            renderHandItem(event, player, right);
        }
    }

    /** Mirrors Azurite's resetBone for the single reposed arm bone. */
    private static void resetBone(GeoBone bone) {
        bone.setPosX(0.0F);
        bone.setPosY(0.0F);
        bone.setPosZ(0.0F);
        bone.setRotX(0.0F);
        bone.setRotY(0.0F);
        bone.setRotZ(0.0F);
        bone.setScaleX(1.0F);
        bone.setScaleY(1.0F);
        bone.setScaleZ(1.0F);
    }

    private static void renderHandItem(RenderArmEvent event, AbstractClientPlayer player, boolean right) {
        boolean mainArmRight = player.getMainArm() == HumanoidArm.RIGHT;
        ItemStack stack = (right == mainArmRight) ? player.getMainHandItem() : player.getOffhandItem();
        if (stack.isEmpty()) {
            return;
        }
        try {
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            // Same tail of ItemInHandLayer#renderArmWithItem that vanilla would run.
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate((right ? 1.0F : -1.0F) / 16.0F, 0.125F, -0.625F);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack,
                    right ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                            : ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    !right, poseStack, event.getMultiBufferSource(), event.getPackedLight());
        } catch (RuntimeException ignored) {
            // A held item must never break hand rendering.
        } finally {
            event.getPoseStack().popPose();
        }
    }
}
