package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.core.animation.AnimationState;

public final class FormGeoModel extends GeoModel<FormGeoAnimatable> {
    private static final float DEG_TO_RAD = (float) Math.PI / 180.0F;
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public FormGeoModel(ResourceLocation model, ResourceLocation texture) {
        this.model = model;
        this.texture = texture;
        this.animation = ResourceLocation.fromNamespaceAndPath(model.getNamespace(), "animations/missing.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(FormGeoAnimatable animatable) {
        return model;
    }

    @Override
    public ResourceLocation getModelResource(FormGeoAnimatable animatable, GeoRenderer<FormGeoAnimatable> renderer) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(FormGeoAnimatable animatable) {
        return texture;
    }

    @Override
    public ResourceLocation getTextureResource(FormGeoAnimatable animatable, GeoRenderer<FormGeoAnimatable> renderer) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(FormGeoAnimatable animatable) {
        return animation;
    }

    /**
     * The Fabric implementation copies the current PlayerModel pose to the form bones every frame.
     * GeckoLib provides the model renderer; the original Bedrock/Azure animation files are applied
     * by the Forge-side animation state machine after the vanilla player pose is copied.
     */
    @Override
    public void setCustomAnimations(FormGeoAnimatable animatable, long instanceId,
                                    AnimationState<FormGeoAnimatable> animationState) {
        Player player = animatable.getPlayer();
        if (player == null) {
            return;
        }

        resetTransform("bipedHead");
        resetTransform("bipedBody");
        resetTransform("bipedLeftArm");
        resetTransform("bipedRightArm");
        resetTransform("bipedLeftLeg");
        resetTransform("bipedRightLeg");

        float partialTick = animationState.getPartialTick();
        boolean inventoryPreview = animatable.isInventoryPreview();
        // InventoryScreen temporarily writes the current rotations only.  Its previous-frame
        // rotations still belong to the world player, so interpolation tears the head away from
        // the body.  The preview must use the values set for this render call verbatim.
        float bodyYaw = inventoryPreview ? player.yBodyRot : Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float headYaw = inventoryPreview ? player.yHeadRot : Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float headPitch = inventoryPreview ? player.getXRot() : player.getViewXRot(partialTick);
        float age = player.tickCount + partialTick;
        float movement = inventoryPreview ? 0.0F : (float) Math.min(1.0D,
                Math.sqrt(player.getDeltaMovement().horizontalDistanceSqr()) * 8.0D);
        if (!player.onGround() || player.isFallFlying() || player.isSwimming()) {
            movement = 0.0F;
        }

        PlayerModel<?> vanillaModel = animatable.getVanillaPlayerModel();
        if (vanillaModel != null) {
            // Fabric copies the prepared PlayerModel pose, then inverts its Y/Z axes for
            // GeoBone space. This preserves swimming, crouching, attack and item poses.
            copyVanillaRotation("bipedHead", vanillaModel.head, true, true);
            copyVanillaRotation("bipedBody", vanillaModel.body, true, false);
            copyVanillaRotation("bipedRightArm", vanillaModel.rightArm, true, true);
            copyVanillaRotation("bipedLeftArm", vanillaModel.leftArm, true, true);
            copyVanillaRotation("bipedRightLeg", vanillaModel.rightLeg, true, true);
            copyVanillaRotation("bipedLeftLeg", vanillaModel.leftLeg, true, true);

            // ModelPart#getTransform().pivot is copied as a negated translation by the
            // Fabric renderer.  Forge 1.20.1 exposes the same pivot as x/y/z.  Keep the
            // vanilla biped offsets for arms and legs so the GeoBone origin matches the
            // PlayerModel origin before form animations are applied.
            copyVanillaPosition("bipedHead", vanillaModel.head, 0.0F, 0.0F, 0.0F);
            copyVanillaPosition("bipedBody", vanillaModel.body, 0.0F, 0.0F, 0.0F);
            copyVanillaPosition("bipedRightArm", vanillaModel.rightArm, -5.0F, 2.0F, 0.0F);
            copyVanillaPosition("bipedLeftArm", vanillaModel.leftArm, 5.0F, 2.0F, 0.0F);
            copyVanillaPosition("bipedRightLeg", vanillaModel.rightLeg, -2.0F, 12.0F, 0.0F);
            copyVanillaPosition("bipedLeftLeg", vanillaModel.leftLeg, 2.0F, 12.0F, 0.0F);
        } else {
            // Safe fallback for non-player preview callers that do not provide a renderer model.
            setRotation("bipedHead", headPitch * DEG_TO_RAD,
                    Mth.wrapDegrees(headYaw - bodyYaw) * DEG_TO_RAD, 0.0F);
            setRotation("bipedBody", !inventoryPreview && player.isCrouching() ? 0.50F : 0.0F, 0.0F, 0.0F);
            setRotation("bipedRightArm", -movement * 0.95F, 0.0F, 0.0F);
            setRotation("bipedLeftArm", movement * 0.95F, 0.0F, 0.0F);
        }

        // Several form models have optional wings or tails.  Give them a restrained idle motion
        // when the matching bones exist; regular biped forms are unaffected.
        float flutter = Mth.sin(age * 0.35F) * 0.15F;
        setRotation("wing_l", 0.0F, 0.0F, flutter);
        setRotation("wing_r", 0.0F, 0.0F, -flutter);
        setRotation("leftWing", 0.0F, 0.0F, flutter);
        setRotation("rightWing", 0.0F, 0.0F, -flutter);
        setRotation("tail", 0.0F, Mth.sin(age * 0.20F) * 0.10F, 0.0F);

        FormAnimationSystem.Selection selection = FormAnimationSystem.select(player);
        if (selection != null) {
            BedrockAnimationPlayer.apply(this, selection, animatable.animationTime(selection, partialTick));
        }
    }

    private void setRotation(String boneName, float x, float y, float z) {
        getBone(boneName).ifPresent(bone -> {
            bone.setRotX(x);
            bone.setRotY(y);
            bone.setRotZ(z);
        });
    }

    private void copyVanillaRotation(String boneName, ModelPart part, boolean invertGeoY, boolean invertGeoZ) {
        float y = invertGeoY ? -part.yRot : part.yRot;
        float z = invertGeoZ ? -part.zRot : part.zRot;
        setRotation(boneName, part.xRot, y, z);
    }

    private void copyVanillaPosition(String boneName, ModelPart part, float offsetX, float offsetY, float offsetZ) {
        setPosition(boneName, -part.x + offsetX, -part.y + offsetY, -part.z + offsetZ);
    }

    private void setPosition(String boneName, float x, float y, float z) {
        getBone(boneName).ifPresent(bone -> {
            bone.setPosX(x);
            bone.setPosY(y);
            bone.setPosZ(z);
        });
    }

    private void resetTransform(String boneName) {
        getBone(boneName).ifPresent(bone -> {
            bone.setPosX(0.0F);
            bone.setPosY(0.0F);
            bone.setPosZ(0.0F);
            bone.setScaleX(1.0F);
            bone.setScaleY(1.0F);
            bone.setScaleZ(1.0F);
        });
    }
}
