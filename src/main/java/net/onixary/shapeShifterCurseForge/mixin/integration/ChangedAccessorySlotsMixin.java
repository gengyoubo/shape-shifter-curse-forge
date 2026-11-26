package net.onixary.shapeShifterCurseForge.mixin.integration;

import net.ltxprogrammer.changed.data.AccessorySlotType;
import net.ltxprogrammer.changed.item.ExoskeletonItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.integration.changed.ChangedIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.ltxprogrammer.changed.data.AccessorySlots", remap = false)
public abstract class ChangedAccessorySlotsMixin {
    @Inject(method = "tryReplaceSlot", at = @At("HEAD"), cancellable = true)
    private static void ssc$restrictForcedEquip(LivingEntity wearer, AccessorySlotType slot, ItemStack stack,
                                                CallbackInfoReturnable<Boolean> cir) {
        // Changed's restraint AI replaces the slot directly, bypassing allowedInSlot.
        if (stack.getItem() instanceof ExoskeletonItem<?> && !ChangedIntegration.canWearExoskeleton(wearer)) {
            cir.setReturnValue(false);
        }
    }
}
