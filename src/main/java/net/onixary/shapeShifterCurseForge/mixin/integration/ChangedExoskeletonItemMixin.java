package net.onixary.shapeShifterCurseForge.mixin.integration;

import net.ltxprogrammer.changed.data.AccessorySlotContext;
import net.ltxprogrammer.changed.data.AccessorySlotType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.integration.changed.ChangedIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.ltxprogrammer.changed.item.ExoskeletonItem", remap = false)
public abstract class ChangedExoskeletonItemMixin {
    @Inject(method = "allowedInSlot", at = @At("HEAD"), cancellable = true)
    private void ssc$restrictWearer(ItemStack stack, LivingEntity wearer, AccessorySlotType slot,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (!ChangedIntegration.canWearExoskeleton(wearer)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "accessoryTick", at = @At("HEAD"), cancellable = true)
    private void ssc$ejectInvalidWearer(AccessorySlotContext<?> context, CallbackInfo ci) {
        LivingEntity wearer = context.wearer();
        if (!wearer.level().isClientSide && !ChangedIntegration.canWearExoskeleton(wearer)) {
            // Reconcile worn exoskeletons from old saves or direct slot writes.
            ChangedIntegration.dropInvalidExoskeleton(wearer);
            ci.cancel();
        }
    }
}
