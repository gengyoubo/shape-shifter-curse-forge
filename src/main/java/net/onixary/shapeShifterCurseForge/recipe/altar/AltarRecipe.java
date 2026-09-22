package net.onixary.shapeShifterCurseForge.recipe.altar;

import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.ItemStack;

public interface AltarRecipe extends Recipe<Container> {
    int getRecipeTime();
    int getFuelCost();
    Ingredient getCatalyst();
    boolean canCraft(net.minecraft.world.entity.player.Player player);
}
