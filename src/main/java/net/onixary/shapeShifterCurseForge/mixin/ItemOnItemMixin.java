package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.power.MissingPowerEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mirrors Apoli's inventory item-on-item hook after the item's own click handling. */
@Mixin(ItemStack.class)
public abstract class ItemOnItemMixin {
    @Inject(method = "overrideOtherStackedOnMe", at = @At("RETURN"), cancellable = true)
    private void ssc$itemOnItem(ItemStack using, Slot slot, ClickAction click, Player player,
                               SlotAccess cursor, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || click != ClickAction.PRIMARY) return;
        if (MissingPowerEvents.itemOnItem(player, using, slot.getItem(), slot)) {
            cir.setReturnValue(true);
        }
    }
}
