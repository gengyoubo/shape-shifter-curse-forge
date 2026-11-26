package net.onixary.shapeShifterCurseForge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.onixary.shapeShifterCurseForge.client.render.FormFirstPersonArmEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies SSC's first-person arm rules at PlayerRenderer.renderHand. */
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

    // The arm pose is ready at the first buffer request. Draw before acquiring the vanilla arm's buffer.
    @Inject(method = "renderHand", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
            cancellable = true)
    private void ssc$renderPreparedFormArm(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                                           AbstractClientPlayer player, ModelPart arm, ModelPart sleeve,
                                           CallbackInfo ci) {
        PlayerRenderer renderer = (PlayerRenderer) (Object) this;
        HumanoidArm hand = arm == renderer.getModel().rightArm ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        if (FormFirstPersonArmEvents.renderArm(player, hand, arm, poseStack, buffers, packedLight)) {
            ci.cancel();
        }
    }
}
