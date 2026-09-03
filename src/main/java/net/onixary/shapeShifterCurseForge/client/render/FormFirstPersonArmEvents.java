package net.onixary.shapeShifterCurseForge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Forge counterpart of Fabric's first-person form rendering
 * ({@code FormRenderFeature#rFPM_PartA}/{@code rFPM_PartB} plus
 * {@code DefaultModelAnimationSystem#beforeRenderFirstPerson}/
 * {@code processAnimationFirstPerson}).
 *
 * <p>Fabric hooks the arm render to hide the vanilla arm when the form hides it and
 * to render the form's arm GeoBone with the currently playing clip pose. The clip pose
 * comes from PAL there; here it comes from the shared {@link FormGeoAnimatable} pose
 * preparation running on a scratch vanilla model, so cross-fades and power-animation
 * layers behave exactly like the third-person pass.</p>
 */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class FormFirstPersonArmEvents {
    private static PlayerModel<AbstractClientPlayer> scratchWideModel;
    private static PlayerModel<AbstractClientPlayer> scratchSlimModel;

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
        if (renderer == null) {
            return;
        }
        FormGeoModel model = (FormGeoModel) renderer.getGeoModel();
        boolean right = event.getArm() == HumanoidArm.RIGHT;
        // Mirrors rFPM_PartA: the vanilla arm is hidden when the form hides it.
        if (model.isVanillaPartHidden(right ? "rightArm" : "leftArm")) {
            event.setCanceled(true);
        }
        // Mirrors beforeRenderFirstPerson: the mapped arm bone (biped arms by default,
        // overridable through first_person_render) is rendered when the Geo model has it.
        Optional<GeoBone> armBone = model.getBone(model.firstPersonArmBone(right));
        if (armBone.isEmpty()) {
            return;
        }

        // GeckoLib nulls the renderer's animatable field in doPostRenderCleanup, so
        // restore it before touching animation state.
        renderer.setPlayer(player);
        FormGeoAnimatable animatable = renderer.getAnimatable();
        if (animatable == null) {
            return;
        }
        PlayerModel<AbstractClientPlayer> scratch = scratchModel(minecraft, player);
        Player previousPlayer = animatable.getPlayer();
        PlayerModel<?> previousModel = animatable.getVanillaPlayerModel();
        boolean wasPreview = animatable.isInventoryPreview();
        renderer.setVanillaPlayerModel(scratch);
        renderer.setInventoryPreview(false);
        BakedGeoModel baked;
        try {
            renderer.prepareVanillaPlayerPose(minecraft.getFrameTime());
            baked = model.getBakedModel(model.getModelResource(animatable));
        } catch (RuntimeException exception) {
            restoreRendererState(renderer, previousPlayer, previousModel, wasPreview);
            return;
        }
        if (baked == null) {
            restoreRendererState(renderer, previousPlayer, previousModel, wasPreview);
            return;
        }

        // Hide every other top-level bone so the full render path draws only the arm
        // subtree. Flags are snapshotted and restored: the baked model is shared with
        // the third-person pass.
        GeoBone target = armBone.get();
        Map<GeoBone, Boolean> hiddenBefore = new IdentityHashMap<>();
        for (GeoBone topLevel : baked.topLevelBones()) {
            hiddenBefore.put(topLevel, topLevel.isHidden());
            topLevel.setHidden(topLevel != target);
        }
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        // When the vanilla arm render survives, it already shows the hand item;
        // the Geo layer only fills in for arms vanilla no longer draws.
        animatable.setSuppressHeldItems(!event.isCanceled());
        try {
            // Same Geo coordinate conversion as the third-person pass, applied on top
            // of the first-person hand matrix like Fabric's rFPM_PartB.
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.translate(0.0D, -1.51D, 0.0D);
            poseStack.translate(-0.5D, -0.5D, -0.5D);
            ResourceLocation texture = renderer.getTextureLocation(animatable);
            RenderType renderType = RenderType.entityTranslucent(texture);
            renderer.render(poseStack, animatable, event.getMultiBufferSource(), renderType,
                    event.getMultiBufferSource().getBuffer(renderType), event.getPackedLight());
        } catch (RuntimeException ignored) {
            // A first-person failure must never hide the vanilla arm fallback: the
            // event was only cancelled when the form hides the arm anyway.
        } finally {
            poseStack.popPose();
            for (Map.Entry<GeoBone, Boolean> entry : hiddenBefore.entrySet()) {
                entry.getKey().setHidden(entry.getValue());
            }
            restoreRendererState(renderer, previousPlayer, previousModel, wasPreview);
        }
    }

    private static void restoreRendererState(FormGeoRenderer renderer, Player player,
                                             PlayerModel<?> vanillaModel, boolean inventoryPreview) {
        renderer.setPlayer(player);
        renderer.setVanillaPlayerModel(vanillaModel);
        renderer.setInventoryPreview(inventoryPreview);
    }

    private static PlayerModel<AbstractClientPlayer> scratchModel(Minecraft minecraft,
                                                                  AbstractClientPlayer player) {
        boolean slim = "slim".equals(player.getModelName());
        if (slim) {
            if (scratchSlimModel == null) {
                scratchSlimModel = new PlayerModel<>(
                        minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
            }
            return scratchSlimModel;
        }
        if (scratchWideModel == null) {
            scratchWideModel = new PlayerModel<>(
                    minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        }
        return scratchWideModel;
    }
}
