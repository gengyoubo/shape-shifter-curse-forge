package net.onixary.shapeShifterCurseForge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.client.render.FormGeoModel;
import net.onixary.shapeShifterCurseForge.client.render.GeoBonePose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * Leg armor on four-legged forms (Spider 3): the front legs already copy the vanilla legs,
 * so leggings and boots render there as usual. A second pass of the same armor piece then
 * places the leg parts on the Geo hind-leg bones, so both pairs of legs are covered.
 *
 * <p>The second pass re-enters {@code renderArmorPiece}, so vanilla armor, trims, glint and
 * GeckoLib armor (morphscale) all work unchanged. Only the legs are visible in that pass.</p>
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HindLegArmorLayerMixin {
    @Unique private static final String LEFT_HIND_LEG = "bipedLeftHindLeg";
    @Unique private static final String RIGHT_HIND_LEG = "bipedRightHindLeg";

    @Unique private static GeoBone ssc$leftHind;
    @Unique private static GeoBone ssc$rightHind;

    @Shadow
    private void renderArmorPiece(PoseStack pose, MultiBufferSource buffers, LivingEntity entity,
                                  EquipmentSlot slot, int light, HumanoidModel<?> armor) {
        throw new AssertionError();
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"))
    private void ssc$renderHindLegPass(PoseStack pose, MultiBufferSource buffers, LivingEntity entity,
                                       EquipmentSlot slot, int light, HumanoidModel<?> armor, CallbackInfo ci) {
        if (ssc$leftHind != null || ssc$rightHind != null) return; // already the hind pass
        if (slot != EquipmentSlot.LEGS && slot != EquipmentSlot.FEET) return;
        if (!(entity instanceof Player player) || entity.getItemBySlot(slot).isEmpty()) return;
        FormGeoModel model = GeoBonePose.activeModel(player);
        if (model == null) return;
        GeoBone left = model.getBone(LEFT_HIND_LEG).orElse(null);
        GeoBone right = model.getBone(RIGHT_HIND_LEG).orElse(null);
        if (left == null && right == null) return;
        ssc$leftHind = left;
        ssc$rightHind = right;
        try {
            renderArmorPiece(pose, buffers, entity, slot, light, armor);
        } finally {
            ssc$leftHind = null;
            ssc$rightHind = null;
        }
    }

    @Inject(method = "renderArmorPiece", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;setPartVisibility(Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            shift = At.Shift.AFTER))
    private void ssc$poseHindLegs(PoseStack pose, MultiBufferSource buffers, LivingEntity entity,
                                  EquipmentSlot slot, int light, HumanoidModel<?> armor, CallbackInfo ci) {
        if (ssc$leftHind == null && ssc$rightHind == null) return;
        // Leggings also show the body; the hind pass draws legs only.
        armor.head.visible = false;
        armor.hat.visible = false;
        armor.body.visible = false;
        armor.leftArm.visible = false;
        armor.rightArm.visible = false;
        if (ssc$leftHind != null) GeoBonePose.applyTo(ssc$leftHind, armor.leftLeg);
        else armor.leftLeg.visible = false;
        if (ssc$rightHind != null) GeoBonePose.applyTo(ssc$rightHind, armor.rightLeg);
        else armor.rightLeg.visible = false;
    }
}
