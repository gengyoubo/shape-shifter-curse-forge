package net.onixary.shapeShifterCurseForge.integration.jei;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.onixary.shapeShifterCurseForge.recipe.altar.AltarRecipe;
import net.onixary.shapeShifterCurseForge.recipe.altar.AltarShapedRecipe;
import net.onixary.shapeShifterCurseForge.recipe.altar.AltarShapelessRecipe;

import java.util.Comparator;
import java.util.List;

/** JEI presentation data for the altar custom recipe types. */
public record MachineJeiRecipe(ResourceLocation id, List<Ingredient> grid, Ingredient catalyst,
                               ItemStack output, int recipeTime, int fuelCost) {
    public static List<MachineJeiRecipe> load(RecipeManager recipes, RegistryAccess registries) {
        return recipes.getRecipes().stream()
                .filter(AltarRecipe.class::isInstance)
                .map(AltarRecipe.class::cast)
                .sorted(Comparator.comparing(Recipe::getId))
                .map(recipe -> from(recipe, registries))
                .toList();
    }

    private static MachineJeiRecipe from(AltarRecipe recipe, RegistryAccess registries) {
        NonNullList<Ingredient> grid = NonNullList.withSize(9, Ingredient.EMPTY);
        if (recipe instanceof AltarShapedRecipe shaped) {
            for (int y = 0; y < shaped.getHeight(); y++) {
                for (int x = 0; x < shaped.getWidth(); x++) {
                    grid.set(x + y * 3, shaped.ingredientAt(x, y));
                }
            }
        } else if (recipe instanceof AltarShapelessRecipe shapeless) {
            for (int i = 0; i < shapeless.getIngredients().size(); i++) grid.set(i, shapeless.getIngredients().get(i));
        }
        return new MachineJeiRecipe(recipe.getId(), List.copyOf(grid), recipe.getCatalyst(),
                recipe.getResultItem(registries), recipe.getRecipeTime(),
                recipe.getTotalFuelCost() >= 0 ? recipe.getTotalFuelCost() : recipe.getFuelCost() * recipe.getRecipeTime());
    }
}
