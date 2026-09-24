package net.onixary.shapeShifterCurseForge.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import org.jetbrains.annotations.NotNull;

/**
 * JEI integration, only loaded when JEI is present. The dependency is compileOnly,
 * so the plugin class is never touched on JEI-less clients.
 */
@JeiPlugin
public class SscJeiPlugin implements IModPlugin {
    public static final String MOD_ID = ShapeShifterCurseForge.RESOURCE_NAMESPACE;
    public static final RecipeType<WebComposterRecipe> WEB_COMPOSTING =
            RecipeType.create(MOD_ID, "web_compostable", WebComposterRecipe.class);
    public static final RecipeType<MachineJeiRecipe> ALTAR_RECIPES =
            RecipeType.create(MOD_ID, "altar", MachineJeiRecipe.class);
    public static final RecipeType<MachineJeiRecipe> ALTER_RECIPES =
            RecipeType.create(MOD_ID, "alter", MachineJeiRecipe.class);

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new WebComposterCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new MachineJeiCategory(registration.getJeiHelpers().getGuiHelper(), true));
        registration.addRecipeCategories(new MachineJeiCategory(registration.getJeiHelpers().getGuiHelper(), false));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        IIngredientManager ingredientManager = registration.getIngredientManager();
        registration.addRecipes(WEB_COMPOSTING, WebComposterRecipe.getRecipes(ingredientManager));
        var resourceManager = Minecraft.getInstance().getResourceManager();
        registration.addRecipes(ALTAR_RECIPES, MachineJeiRecipe.load(resourceManager, "altar"));
        registration.addRecipes(ALTER_RECIPES, MachineJeiRecipe.load(resourceManager, "alter"));
    }
}
