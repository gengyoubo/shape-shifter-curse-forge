package net.onixary.shapeShifterCurseForge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Implements the third-person held-item visibility power without replacing PlayerRenderer. */
@Mixin(ItemInHandLayer.class)
public abstract class HideThirdPersonItemMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void ssc$hideHeldItem(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext,
                                   HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer,
                                   int packedLight, CallbackInfo ci) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) return;
        final boolean[] hide = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!hide[0] && "shape-shifter-curse:hide_tp_held_item".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) hide[0] = true;
        });
        if (hide[0]) ci.cancel();
    }
}
