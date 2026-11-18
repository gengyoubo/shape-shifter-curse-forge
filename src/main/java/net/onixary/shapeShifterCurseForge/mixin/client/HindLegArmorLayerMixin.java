package net.onixary.shapeShifterCurseForge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.client.render.GeoBonePose;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leg armor on four-legged forms (Spider 3) is bound to the Geo leg bones, front and hind.
 *
 * <p>The ordinary leggings/boots pass keeps the body but hides its legs. Each captured leg
 * bone then gets its own pass of the same armor piece: the bone's full render matrix,
 * relative to this layer's {@code PoseStack}, is multiplied onto the stack and the armor
 * leg part is left at the identity pose, so the leg is drawn exactly in the bone's frame.
 * Re-entering {@code renderArmorPiece} keeps vanilla armor, trims, glint and GeckoLib armor
 * (morphscale) working unchanged.</p>
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HindLegArmorLayerMixin {
    /** Leg bones in draw order, and whether each uses the armor model's left leg. */
    @Unique private static final String[] SSC_LEG_BONES = {
            GeoBonePose.LEFT_LEG, GeoBonePose.RIGHT_LEG, GeoBonePose.LEFT_HIND_LEG, GeoBonePose.RIGHT_HIND_LEG};
    @Unique private static final boolean[] SSC_LEFT_SIDE = {true, false, true, false};

    /** 0 = not in a leg pass, 1 = left leg pass, 2 = right leg pass. */
    @Unique private static int ssc$legPass;

    @Shadow
    private void renderArmorPiece(PoseStack pose, MultiBufferSource buffers, LivingEntity entity,
                                  EquipmentSlot slot, int light, HumanoidModel<?> armor) {
        throw new AssertionError();
    }

    @Unique
    private static boolean ssc$isQuadrupedLegSlot(LivingEntity entity, EquipmentSlot slot) {
        return (slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET)
                && entity instanceof Player player && GeoBonePose.hasHindLegs(player);
    }

    @Inject(method = "renderArmorPiece", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;setPartVisibility(Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            shift = At.Shift.AFTER))
    private void ssc$bindLegParts(PoseStack pose, MultiBufferSource buffers, LivingEntity entity,
                                  EquipmentSlot slot, int light, HumanoidModel<?> armor, CallbackInfo ci) {
        if (ssc$legPass == 0) {
            if (ssc$isQuadrupedLegSlot(entity, slot)) {
                // Legs are drawn by the per-bone passes; leggings still draw the body here.
                armor.leftLeg.visible = false;
                armor.rightLeg.visible = false;
            }
            return;
        }
        boolean left = ssc$legPass == 1;
        armor.head.visible = false;
        armor.hat.visible = false;
        armor.body.visible = false;
        armor.leftArm.visible = false;
        armor.rightArm.visible = false;
        (left ? armor.rightLeg : armor.leftLeg).visible = false;
        // The PoseStack already carries the bone frame; the part itself stays at the origin.
        ModelPart leg = left ? armor.leftLeg : armor.rightLeg;
        leg.setPos(0.0F, 0.0F, 0.0F);
        leg.setRotation(0.0F, 0.0F, 0.0F);
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"))
    private void ssc$renderLegPasses(PoseStack pose, MultiBufferSource buffers, LivingEntity entity,
                                     EquipmentSlot slot, int light, HumanoidModel<?> armor, CallbackInfo ci) {
        if (ssc$legPass != 0 || !ssc$isQuadrupedLegSlot(entity, slot)) return;
        if (entity.getItemBySlot(slot).isEmpty()) return;
        Player player = (Player) entity;
        Matrix4f layerPose = new Matrix4f(pose.last().pose());
        for (int i = 0; i < SSC_LEG_BONES.length; i++) {
            Matrix4f frame = GeoBonePose.legArmorFrame(player, SSC_LEG_BONES[i], layerPose);
            if (frame == null) continue;
            pose.pushPose();
            ssc$legPass = SSC_LEFT_SIDE[i] ? 1 : 2;
            try {
                GeoBonePose.mulFrame(pose, frame);
                renderArmorPiece(pose, buffers, entity, slot, light, armor);
            } finally {
                ssc$legPass = 0;
                pose.popPose();
            }
        }
    }
}
