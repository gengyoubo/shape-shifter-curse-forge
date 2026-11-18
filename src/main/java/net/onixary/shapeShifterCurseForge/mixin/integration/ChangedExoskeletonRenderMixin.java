package net.onixary.shapeShifterCurseForge.mixin.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ltxprogrammer.changed.client.renderer.model.ExoskeletonModel;
import net.ltxprogrammer.changed.data.AccessorySlotContext;
import net.ltxprogrammer.changed.util.Cacheable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Matches Four Addon's head hiding and form-dependent replacement-leg visibility. */
@Pseudo
@Mixin(targets = "net.ltxprogrammer.changed.client.renderer.accessory.WornExoskeletonRenderer", remap = false)
public abstract class ChangedExoskeletonRenderMixin {
    @Shadow @Final private Cacheable<ExoskeletonModel.VisorModel> suitVisorModel;
    @Shadow @Final private Cacheable<ExoskeletonModel.ReplacementLimbs> suitLegsModel;

    @Shadow public abstract ExoskeletonModel getModel();

    @Inject(method = "render", at = @At("HEAD"))
    private void ssc$setExoskeletonVisibility(AccessorySlotContext<?> context, PoseStack poseStack,
                                              RenderLayerParent<?, ?> parent, MultiBufferSource buffers,
                                              int packedLight, float limbSwing, float limbSwingAmount,
                                              float partialTick, float ageInTicks, float netHeadYaw,
                                              float headPitch, CallbackInfo ci) {
        ExoskeletonModel model = getModel();
        model.getHead().visible = false;
        ((ChangedExoskeletonModelAccessor) (Object) model).ssc$getHeadSupport().visible = false;
        ((ChangedExoskeletonVisorAccessor) (Object) suitVisorModel.get()).ssc$getVisorHead().visible = false;

        boolean showReplacementLegs = !(context.wearer() instanceof Player player)
                || FormRegistry.ORIGINAL_BEFORE_ENABLE.equals(FormManager.current(player).id());
        ChangedExoskeletonLegsAccessor legs = (ChangedExoskeletonLegsAccessor) (Object) suitLegsModel.get();
        legs.ssc$getReplacementRoot().visible = showReplacementLegs;
        legs.ssc$getReplacementLeftLeg().visible = showReplacementLegs;
        legs.ssc$getReplacementRightLeg().visible = showReplacementLegs;
    }
}
