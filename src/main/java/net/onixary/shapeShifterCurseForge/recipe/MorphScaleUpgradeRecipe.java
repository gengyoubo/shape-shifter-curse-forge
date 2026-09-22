package net.onixary.shapeShifterCurseForge.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.registry.ModRecipeSerializers;

/**
 * Forge equivalent of Fabric's MorphScaleUpgradeRecipe.  The template and
 * addition are data-driven; the middle smithing slot may be any item that has
 * not already received the MorphScaleItem marker.  SSC item conditions use
 * this exact marker when checking morph-scale equipment.
 */
public final class MorphScaleUpgradeRecipe implements SmithingRecipe {
    private static final String MORPH_SCALE_ITEM = "MorphScaleItem";

    private final ResourceLocation id;
    private final Ingredient template;
    private final Ingredient addition;

    public MorphScaleUpgradeRecipe(ResourceLocation id, Ingredient template, Ingredient addition) {
        this.id = id;
        this.template = template;
        this.addition = addition;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return template.test(stack);
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return !stack.isEmpty() && (!stack.hasTag() || !stack.getTag().getBoolean(MORPH_SCALE_ITEM));
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return addition.test(stack);
    }

    @Override
    public boolean matches(Container container, Level level) {
        return isTemplateIngredient(container.getItem(0))
                && isBaseIngredient(container.getItem(1))
                && isAdditionIngredient(container.getItem(2));
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        ItemStack result = container.getItem(1).copy();
        result.setCount(1);
        result.getOrCreateTag().putBoolean(MORPH_SCALE_ITEM, true);
        return result;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        ItemStack preview = new ItemStack(Items.STONE);
        preview.getOrCreateTag().putBoolean(MORPH_SCALE_ITEM, true);
        return preview;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.MORPH_SCALE_UPGRADE.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.SMITHING;
    }

    public static final class Serializer implements RecipeSerializer<MorphScaleUpgradeRecipe> {
        @Override
        public MorphScaleUpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new MorphScaleUpgradeRecipe(id,
                    Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "template"), false),
                    Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "addition"), false));
        }

        @Override
        public MorphScaleUpgradeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new MorphScaleUpgradeRecipe(id, Ingredient.fromNetwork(buffer), Ingredient.fromNetwork(buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, MorphScaleUpgradeRecipe recipe) {
            recipe.template.toNetwork(buffer);
            recipe.addition.toNetwork(buffer);
        }
    }
}
