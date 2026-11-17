package net.onixary.shapeShifterCurseForge.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.registry.ModBlocks;
import org.jetbrains.annotations.NotNull;

/**
 * Optional integration discovered by JEI when present.
 */
@JeiPlugin
public class SscJeiPlugin implements IModPlugin {
    public static final String MOD_ID = ShapeShifterCurseForge.RESOURCE_NAMESPACE;
    public static final RecipeType<WebComposterRecipe> WEB_COMPOSTING =
            RecipeType.create(MOD_ID, "web_compostable", WebComposterRecipe.class);
    public static final RecipeType<MachineJeiRecipe> ALTAR_RECIPES =
            RecipeType.create(MOD_ID, "altar", MachineJeiRecipe.class);

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new WebComposterCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new MachineJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        IIngredientManager ingredientManager = registration.getIngredientManager();
        registration.addRecipes(WEB_COMPOSTING, WebComposterRecipe.getRecipes(ingredientManager));
        var level = Minecraft.getInstance().level;
        if (level != null) {
            registration.addRecipes(ALTAR_RECIPES, MachineJeiRecipe.load(level.getRecipeManager(), level.registryAccess()));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ALTAR.get()), ALTAR_RECIPES);
    }
}
