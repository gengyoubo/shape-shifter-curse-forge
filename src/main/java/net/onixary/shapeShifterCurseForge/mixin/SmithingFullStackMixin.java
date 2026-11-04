package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.onixary.shapeShifterCurseForge.other.config.SscCommonConfig;
import net.onixary.shapeShifterCurseForge.recipe.MorphScaleUpgradeRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingMenu.class)
public abstract class SmithingFullStackMixin {
    @Shadow private SmithingRecipe selectedRecipe;
    @Shadow private void shrinkStackInSlot(int index) { throw new AssertionError(); }
    @Unique private boolean ssc$fullStack;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void ssc$rememberRecipe(Player player, ItemStack output, CallbackInfo ci) {
        ssc$fullStack = selectedRecipe instanceof MorphScaleUpgradeRecipe && SscCommonConfig.FULL_STACK_UPGRADE.get();
    }

    @Redirect(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/SmithingMenu;shrinkStackInSlot(I)V"))
    private void ssc$consume(SmithingMenu menu, int slot) {
        if (slot == 1 && ssc$fullStack) menu.getSlot(1).set(ItemStack.EMPTY);
        else shrinkStackInSlot(slot);
    }
}
