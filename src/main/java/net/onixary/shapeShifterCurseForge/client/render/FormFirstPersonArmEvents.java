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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
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

    /** Also receives canceled events so another renderer cannot make form arms reappear. */
    @SubscribeEvent(receiveCanceled = true)
    public static void renderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        Minecraft minecraft = Minecraft.getInstance();
        if (player != minecraft.player || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        boolean hideArms = shouldHideArms(player);
        if (hideArms) {
            event.setCanceled(true);
            return;
        }

        FormDefinition form = FormManager.current(player);
        if (!form.hasFlag("special_form") && form.stage() <= 0) {
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

        // Mirrors processAnimationFirstPerson: reset the bone to its initial state,
        // then copy the vanilla first-person arm part pose onto it.
        PlayerModel<AbstractClientPlayer> rendererModel = playerRenderer.getModel();
        ModelPart armPart = right ? rendererModel.rightArm : rendererModel.leftArm;
        FormGeoModel.resetToInitial(geoBone);
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
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.translate(0.0D, -1.51D, 0.0D);

            ResourceLocation texture = renderer.getTextureLocation(animatable);
            RenderType renderType = RenderType.entityTranslucent(texture);
            VertexConsumer buffer = event.getMultiBufferSource().getBuffer(renderType);

            renderer.renderRecursively(
                    poseStack,
                    animatable,
                    geoBone,
                    renderType,
                    event.getMultiBufferSource(),
                    buffer,
                    false,
                    partialTick,
                    event.getPackedLight(),
                    OverlayTexture.NO_OVERLAY,
                    1.0F, 1.0F, 1.0F, 1.0F);
        } catch (RuntimeException ignored) {
            // A first-person arm must never break hand rendering.
        } finally {
            poseStack.popPose();
        }
    }

    public static boolean shouldHideArms(AbstractClientPlayer player) {
        final boolean[] hide = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!hide[0] && "shape-shifter-curse:no_render_arm".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                hide[0] = true;
            }
        });
        return hide[0];
    }
}
