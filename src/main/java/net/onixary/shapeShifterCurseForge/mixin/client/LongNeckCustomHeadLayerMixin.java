package net.onixary.shapeShifterCurseForge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.client.render.LongNeckHeadAlignment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Port of Fabric's {@code LongNeckHeadFeatureRendererMixin}: skulls, pumpkins and other head
 * items follow the long-neck head. This layer borrows the shared player-model head, so the
 * pose is restored once the layer returns.
 */
@Mixin(CustomHeadLayer.class)
public abstract class LongNeckCustomHeadLayerMixin<T extends LivingEntity, M extends EntityModel<T> & HeadedModel>
        extends RenderLayer<T, M> {
    @Unique
    private static LongNeckHeadAlignment.SavedHead ssc$savedHead;

    protected LongNeckCustomHeadLayerMixin(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"))
    private void ssc$followLongNeck(PoseStack pose, MultiBufferSource buffers, int light, T entity,
                                    float limbSwing, float limbSwingAmount, float partialTick, float age,
                                    float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        var head = getParentModel().getHead();
        LongNeckHeadAlignment.SavedHead saved = LongNeckHeadAlignment.SavedHead.of(head);
        if (LongNeckHeadAlignment.apply(player, head)) {
            ssc$savedHead = saved;
        }
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("RETURN"))
    private void ssc$restoreHead(PoseStack pose, MultiBufferSource buffers, int light, T entity,
                                 float limbSwing, float limbSwingAmount, float partialTick, float age,
                                 float headYaw, float headPitch, CallbackInfo ci) {
        if (ssc$savedHead != null) {
            ssc$savedHead.restore();
            ssc$savedHead = null;
        }
    }
}
