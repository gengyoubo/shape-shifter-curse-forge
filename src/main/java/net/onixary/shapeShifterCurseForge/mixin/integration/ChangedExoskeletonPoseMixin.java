package net.onixary.shapeShifterCurseForge.mixin.integration;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.client.render.GeoBonePose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Changed adapts a vanilla-shaped wearer to the exoskeleton: it lifts the whole renderer by
 * 2 world pixels and spreads the arms / bends the legs. On an SSC form the suit is drawn in the
 * form's own bone frames instead, so the form keeps its pose and root, and the suit follows.
 * Cancelling here also keeps armor and held items on the (unlifted) form.
 */
@Pseudo
@Mixin(targets = "net.ltxprogrammer.changed.client.renderer.model.ExoskeletonModel", remap = false)
public abstract class ChangedExoskeletonPoseMixin {
    @Inject(method = "animateWearerPose", at = @At("HEAD"), cancellable = true)
    private void ssc$keepFormRoot(net.minecraft.world.entity.LivingEntity wearer,
                                  net.minecraft.client.model.EntityModel<?> wearerModel,
                                  com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  net.minecraft.world.item.ItemStack stack, CallbackInfo ci) {
        if (wearer instanceof Player player && GeoBonePose.hasCapturedBones(player)) ci.cancel();
    }

    /** Runs inside {@code setupAnim}, both in SSC's Geo pre-pass and in vanilla's pass. */
    @Inject(method = "animateWearerLimbs", at = @At("HEAD"), cancellable = true)
    private void ssc$keepFormLimbs(net.minecraft.client.model.EntityModel<?> wearerModel,
                                   net.minecraft.world.item.ItemStack stack, CallbackInfo ci) {
        if (wearerModel instanceof PlayerModel<?> && GeoBonePose.hasFramePlayer()) ci.cancel();
    }
}
