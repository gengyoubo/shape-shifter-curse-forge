package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.resources.ResourceLocation;
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
        this.animation = new ResourceLocation(model.getNamespace(), "animations/missing.animation.json");
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
     * These forms only ship an empty looping GeckoLib animation, so keep that behaviour here instead
     * of relying on a missing per-form animation file.
     */
    @Override
    public void setCustomAnimations(FormGeoAnimatable animatable, long instanceId,
                                    AnimationState<FormGeoAnimatable> animationState) {
        Player player = animatable.getPlayer();
        if (player == null) {
            return;
        }

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

        float stride = age * (player.isSprinting() ? 0.90F : 0.60F);
        float armSwing = Mth.cos(stride) * 0.95F * movement;
        float legSwing = Mth.cos(stride) * 1.40F * movement;

        // Vanilla's PlayerModel receives netHeadYaw = headYaw - bodyYaw.
        setRotation("bipedHead", headPitch * DEG_TO_RAD,
                Mth.wrapDegrees(headYaw - bodyYaw) * DEG_TO_RAD, 0.0F);
        setRotation("bipedBody", !inventoryPreview && player.isCrouching() ? 0.50F : 0.0F, 0.0F, 0.0F);
        setRotation("bipedRightArm", -armSwing, 0.0F, 0.0F);
        setRotation("bipedLeftArm", armSwing, 0.0F, 0.0F);
        setRotation("bipedRightLeg", legSwing, 0.0F, 0.0F);
        setRotation("bipedLeftLeg", -legSwing, 0.0F, 0.0F);

        // Several form models have optional wings or tails.  Give them a restrained idle motion
        // when the matching bones exist; regular biped forms are unaffected.
        float flutter = Mth.sin(age * 0.35F) * 0.15F;
        setRotation("wing_l", 0.0F, 0.0F, flutter);
        setRotation("wing_r", 0.0F, 0.0F, -flutter);
        setRotation("leftWing", 0.0F, 0.0F, flutter);
        setRotation("rightWing", 0.0F, 0.0F, -flutter);
        setRotation("tail", 0.0F, Mth.sin(age * 0.20F) * 0.10F, 0.0F);
    }

    private void setRotation(String boneName, float x, float y, float z) {
        getBone(boneName).ifPresent(bone -> {
            bone.setRotX(x);
            bone.setRotY(y);
            bone.setRotZ(z);
        });
    }
}
