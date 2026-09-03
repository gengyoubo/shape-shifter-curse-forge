package net.onixary.shapeShifterCurseForge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.client.render.BedrockAnimationPlayer;
import net.onixary.shapeShifterCurseForge.client.render.FormClientRenderEvents;
import net.onixary.shapeShifterCurseForge.client.render.FormGeoAnimatable;
import net.onixary.shapeShifterCurseForge.client.render.FormGeoRenderer;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mirrors PAL's {@code PlayerRendererMixin#applyBodyTransforms}: the {@code body} clip
 * channel is a renderer-level transform that must sit inside vanilla's own matrix
 * stack. The Pre-pass copy only feeds the Geo overlay; vanilla layers (armor via
 * {@code copyPropertiesTo}, held items, capes) never see it, so armor keeps an
 * upright pose while the form prostrates. Applying the stored transform here puts
 * every vanilla layer under the same root motion, exactly like Fabric.
 *
 * <p>Player renderers only exist on the client, so this mixin never activates on a
 * dedicated server.</p>
 */
@Mixin(net.minecraft.client.renderer.entity.player.PlayerRenderer.class)
public abstract class PlayerRendererBodyMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
            at = @At("RETURN"))
    private void ssc$applyBodyTransform(AbstractClientPlayer entity, PoseStack poseStack,
                                        float animationProgress, float bodyYaw, float partialTick,
                                        CallbackInfo ci) {
        Player player = entity;
        FormDefinition form = FormManager.current(player);
        if (!form.hasFlag("special_form") && form.tier() <= 0) {
            return;
        }
        FormGeoRenderer renderer = FormClientRenderEvents.rendererFor(form);
        if (renderer == null) {
            return;
        }
        FormGeoAnimatable animatable = renderer.getAnimatable();
        if (animatable == null || animatable.isInventoryPreview() || !animatable.hasSafeRenderState()) {
            return;
        }
        BedrockAnimationPlayer.BodyTransform transform = animatable.getBodyTransform();
        if (transform == null || transform.isIdentity()) {
            return;
        }
        poseStack.translate(transform.x(), transform.y() + 0.7F, transform.z());
        poseStack.mulPose(Axis.ZP.rotation(transform.roll()));
        poseStack.mulPose(Axis.YP.rotation(transform.yaw()));
        poseStack.mulPose(Axis.XP.rotation(transform.pitch()));
        poseStack.translate(0.0D, -0.7D, 0.0D);
    }
}
