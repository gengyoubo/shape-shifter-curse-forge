package net.onixary.shapeShifterCurseForge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.client.render.LongNeckHeadAlignment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Port of Fabric's {@code LongNeckArmorFeatureRendererMixin}: helmets follow the long-neck
 * head. Injected before Forge's armor-model hook, so GeckoLib armor (morphscale) copies the
 * moved head too. The armor model is re-copied from the player model for every piece, so no
 * restore is needed.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class LongNeckArmorLayerMixin {
    @Inject(method = "renderArmorPiece", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;setPartVisibility(Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            shift = At.Shift.AFTER))
    private void ssc$followLongNeck(PoseStack pose, MultiBufferSource buffers, LivingEntity entity,
                                    EquipmentSlot slot, int light, HumanoidModel<?> armor, CallbackInfo ci) {
        if (slot != EquipmentSlot.HEAD || !(entity instanceof Player player)) return;
        if (LongNeckHeadAlignment.apply(player, armor.head)) {
            armor.hat.copyFrom(armor.head);
        }
    }
}
