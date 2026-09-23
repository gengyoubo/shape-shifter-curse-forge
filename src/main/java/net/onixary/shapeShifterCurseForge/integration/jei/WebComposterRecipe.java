package net.onixary.shapeShifterCurseForge.integration.jei;

import com.google.common.base.Preconditions;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.block.WebComposterBlock;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * One JEI display recipe: a single input item and its chance to raise the Web
 * Composter level. Forge port of Fabric's {@code WebComposterRecipe}.
 */
public final class WebComposterRecipe {
    private final List<ItemStack> inputs;
    private final float chance;
    private final ResourceLocation uid;

    public WebComposterRecipe(ItemStack input, float chance, ResourceLocation uid) {
        Preconditions.checkArgument(chance > 0.0F, "web_composting chance must be greater than 0");
        this.inputs = List.of(input);
        this.chance = chance;
        this.uid = uid;
    }

    public List<ItemStack> getInputs() {
        return inputs;
    }

    public float getChance() {
        return chance;
    }

    public ResourceLocation getUid() {
        return uid;
    }

    public static List<WebComposterRecipe> getRecipes(IIngredientManager ingredientManager) {
        Collection<ItemStack> allIngredients = ingredientManager.getAllItemStacks();
        IIngredientHelper<ItemStack> ingredientHelper = ingredientManager.getIngredientHelper(VanillaTypes.ITEM_STACK);
        return allIngredients.stream()
                .filter(WebComposterBlock::canIncrease)
                .map(stack -> {
                    float chance = WebComposterBlock.getIncreaseChance(stack);
                    ResourceLocation registryId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    String path = sanitize(registryId.getNamespace() + "_" + registryId.getPath())
                            + (stack.getDamageValue() != 0 ? "_" + stack.getDamageValue() : "");
                    ResourceLocation uid = ResourceLocation.fromNamespaceAndPath(
                            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "jei/web_composting/" + path);
                    return new WebComposterRecipe(stack, chance, uid);
                })
                .sorted(Comparator.comparingDouble(WebComposterRecipe::getChance))
                .collect(Collectors.toList());
    }

    private static String sanitize(String input) {
        return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
    }
}