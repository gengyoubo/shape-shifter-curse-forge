package net.onixary.shapeShifterCurseForge.brew;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge port of Fabric's {@code BrewingRecipeUtils}. Datapack recipes loaded from
 * {@code data/<namespace>/dynamic_brewing_recipes/*.json} are registered into
 * Forge's {@link BrewingRecipeRegistry} and removed again on the next reload.
 */
public final class BrewingRecipeUtils {
    /**
     * Forge's {@link BrewingRecipeRegistry#getRecipes()} is unmodifiable, so dynamic
     * recipes cannot be removed on reload. We instead remember every recipe signature
     * ever registered this JVM session and only add a recipe the first time it is seen.
     */
    private static final java.util.Set<String> REGISTERED_SIGNATURES = new java.util.HashSet<>();
    private static final List<DynamicPotionInfo> DYNAMIC_POTION_INFO = new ArrayList<>();

    private BrewingRecipeUtils() {
    }

    /** Called before a datapack reload; only resets the per-reload info list. */
    public static void onLoadDynamicBrewingRecipesStart() {
        DYNAMIC_POTION_INFO.clear();
    }

    public static void onLoadDynamicBrewingRecipesEnd() {
        // Recipes were registered eagerly in registerPotionRecipe; nothing else to do.
    }

    /**
     * {@code type} is {@code potion} or {@code item}; {@code input}/{@code output} are
     * registry ids (potion ids for the potion type, item ids for the item type).
     * {@code target_form} is retained for callers that need to know which form a potion
     * grants (Fabric parity).
     */
    public static void registerPotionRecipe(JsonObject recipeJson) {
        if (recipeJson == null) {
            ShapeShifterCurseForge.LOGGER.error("[ssc-brew] recipe json is null");
            return;
        }
        if (!recipeJson.has("type") || !recipeJson.has("input")
                || !recipeJson.has("ingredient") || !recipeJson.has("output")) {
            ShapeShifterCurseForge.LOGGER.error("[ssc-brew] recipe json is missing type/input/ingredient/output");
            return;
        }
        ResourceLocation input = ResourceLocation.tryParse(recipeJson.get("input").getAsString());
        ResourceLocation ingredient = ResourceLocation.tryParse(recipeJson.get("ingredient").getAsString());
        ResourceLocation output = ResourceLocation.tryParse(recipeJson.get("output").getAsString());
        if (input == null || ingredient == null || output == null) {
            ShapeShifterCurseForge.LOGGER.error("[ssc-brew] recipe json has invalid input/ingredient/output");
            return;
        }
        String type = recipeJson.get("type").getAsString();
        String signature = type + "|" + input + "|" + ingredient + "|" + output;
        if (!REGISTERED_SIGNATURES.add(signature)) {
            return;
        }
        net.minecraft.world.item.Item ingredientItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ingredient);
        if (ingredientItem == null || ingredientItem == net.minecraft.world.item.Items.AIR) {
            ShapeShifterCurseForge.LOGGER.error("[ssc-brew] unknown ingredient item '{}'", ingredient);
            return;
        }
        Ingredient ingredientObject = Ingredient.of(ingredientItem);
        switch (type) {
            case "potion" -> {
                Potion inputPotion = net.minecraft.core.registries.BuiltInRegistries.POTION.get(input);
                Potion outputPotion = net.minecraft.core.registries.BuiltInRegistries.POTION.get(output);
                if (inputPotion == null || outputPotion == null) {
                    ShapeShifterCurseForge.LOGGER.error("[ssc-brew] unknown potion input/output '{}'/'{}'", input, output);
                    return;
                }
                ItemStack inputStack = PotionUtils.setPotion(new ItemStack(Items.POTION), inputPotion);
                ItemStack outputStack = PotionUtils.setPotion(new ItemStack(Items.POTION), outputPotion);
                add(new BrewingRecipe(Ingredient.of(inputStack), ingredientObject, outputStack));
                ResourceLocation targetForm = recipeJson.has("target_form")
                        ? ResourceLocation.tryParse(recipeJson.get("target_form").getAsString()) : null;
                DYNAMIC_POTION_INFO.add(new DynamicPotionInfo(outputPotion, targetForm));
            }
            case "item" -> {
                net.minecraft.world.item.Item inputItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(input);
                net.minecraft.world.item.Item outputItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(output);
                if (inputItem == null || outputItem == null) {
                    ShapeShifterCurseForge.LOGGER.error("[ssc-brew] unknown item input/output '{}'/'{}'", input, output);
                    return;
                }
                add(new BrewingRecipe(Ingredient.of(new ItemStack(inputItem)), ingredientObject, new ItemStack(outputItem)));
            }
            default -> ShapeShifterCurseForge.LOGGER.error("[ssc-brew] unknown recipe type '{}'", type);
        }
    }

    private static void add(IBrewingRecipe recipe) {
        BrewingRecipeRegistry.addRecipe(recipe);
    }

    public record DynamicPotionInfo(Potion outputPotion, ResourceLocation targetForm) {
    }
}