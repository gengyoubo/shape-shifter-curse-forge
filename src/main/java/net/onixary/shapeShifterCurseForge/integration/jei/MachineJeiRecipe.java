package net.onixary.shapeShifterCurseForge.integration.jei;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.recipe.altar.AltarRecipe;
import net.onixary.shapeShifterCurseForge.recipe.altar.AltarShapedRecipe;
import net.onixary.shapeShifterCurseForge.recipe.altar.AltarShapelessRecipe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** JEI presentation data for the altar custom recipe types. */
public record MachineJeiRecipe(ResourceLocation id, List<Ingredient> grid, Ingredient catalyst,
                               ItemStack output, int recipeTime, int fuelCost) {
    public static List<MachineJeiRecipe> load(ResourceManager resources) {
        String prefix = "recipes/altar/";
        List<MachineJeiRecipe> recipes = new ArrayList<>();
        resources.listResources("recipes", id -> id.getNamespace().equals(ShapeShifterCurseForge.RESOURCE_NAMESPACE)
                        && id.getPath().startsWith(prefix) && id.getPath().endsWith(".json"))
                .entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> {
                    try (var reader = entry.getValue().openAsReader()) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                                entry.getKey().getNamespace(),
                                entry.getKey().getPath().substring("recipes/".length(),
                                        entry.getKey().getPath().length() - ".json".length()));
                        AltarRecipe recipe = parse(recipeId, json);
                        if (recipe != null) recipes.add(from(recipe));
                    } catch (Exception exception) {
                        ShapeShifterCurseForge.LOGGER.warn("Could not add altar recipe {} to JEI",
                                entry.getKey(), exception);
                    }
                });
        return List.copyOf(recipes);
    }

    private static AltarRecipe parse(ResourceLocation id, JsonObject json) {
        String type = json.has("type") ? json.get("type").getAsString() : "";
        return switch (type) {
            case "shape-shifter-curse:altar_shaped" -> new AltarShapedRecipe.Serializer().fromJson(id, json);
            case "shape-shifter-curse:altar_shapeless" -> new AltarShapelessRecipe.Serializer().fromJson(id, json);
            default -> null;
        };
    }

    private static MachineJeiRecipe from(AltarRecipe recipe) {
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
                recipe.getResultItem(RegistryAccess.EMPTY), recipe.getRecipeTime(), recipe.getFuelCost());
    }
}
