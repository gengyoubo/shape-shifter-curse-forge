package net.onixary.shapeShifterCurseForge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.client.render.FormGogglesAlignment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adjust only the copied armor model, leaving the player's head and other helmets unchanged. */
@Mixin(HumanoidArmorLayer.class)
public abstract class FormGogglesArmorMixin {
    @Inject(method = "renderArmorPiece", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;setPartVisibility(Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            shift = At.Shift.AFTER))
    private void ssc$alignGoggles(PoseStack pose, MultiBufferSource buffers, LivingEntity entity,
                                  EquipmentSlot slot, int light, HumanoidModel<?> armor, CallbackInfo ci) {
        if (slot != EquipmentSlot.HEAD || !(entity instanceof Player player)
                || !entity.getItemBySlot(slot).is(ItemTags.create(new ResourceLocation("tc4modern", "form_eye_goggles")))) return;
        var form = FormManager.current(player);
        if (form.stage() != 4) return;
        FormGogglesAlignment.apply(armor, player);
    }
}
