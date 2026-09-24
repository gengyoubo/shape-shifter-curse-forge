package net.onixary.shapeShifterCurseForge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.onixary.shapeShifterCurseForge.client.render.FormFirstPersonArmEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Matches Fabric's NoRenderArmPower hook at PlayerRenderer.renderHand. */
@Mixin(PlayerRenderer.class)
public abstract class NoRenderArmMixin {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void ssc$hideFirstPersonArm(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                                        AbstractClientPlayer player, ModelPart arm, ModelPart sleeve,
                                        CallbackInfo ci) {
        if (FormFirstPersonArmEvents.shouldHideArms(player)) {
            ci.cancel();
        }
    }
}
