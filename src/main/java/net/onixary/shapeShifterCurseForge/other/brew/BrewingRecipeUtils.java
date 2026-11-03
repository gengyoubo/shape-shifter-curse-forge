package net.onixary.shapeShifterCurseForge.other.brew;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.power.TransformativeEffectService;
import org.jetbrains.annotations.Nullable;

/**
 * Forge port of Fabric's {@code BrewingRecipeUtils}. Datapack recipes loaded from
 * {@code data/<namespace>/dynamic_brewing_recipes/*.json} are registered into
 * Forge's {@link BrewingRecipeRegistry}.
 *
 * <p>A recipe may declare {@code target_form}; the id is stamped into the brewed stack's
 * {@code targetForm} NBT (Fabric parity) and, when a player drinks the potion, applied through
 * {@link TransformativeEffectService}.</p>
 */
@SuppressWarnings({"EmptyMethod", "deprecation"})
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class BrewingRecipeUtils {
    /** NBT key used by Fabric's {@code CTPUtils}; kept identical for datapack/NBT parity. */
    public static final String TARGET_FORM_KEY = "targetForm";

    // One permanent Forge entry delegates to an immutable snapshot replaced on each reload.
    private static final ReloadableBrewingRecipe DELEGATE = new ReloadableBrewingRecipe();
    private static java.util.List<net.minecraftforge.common.brewing.IBrewingRecipe> pending = new java.util.ArrayList<>();
    private static boolean registered;

    private BrewingRecipeUtils() {
    }

    public static void onLoadDynamicBrewingRecipesStart() {
        pending = new java.util.ArrayList<>();
    }

    public static void onLoadDynamicBrewingRecipesEnd() {
        DELEGATE.recipes = java.util.List.copyOf(pending);
        if (!registered) {
            BrewingRecipeRegistry.addRecipe(DELEGATE);
            registered = true;
        }
    }

    private static final class ReloadableBrewingRecipe implements net.minecraftforge.common.brewing.IBrewingRecipe {
        private volatile java.util.List<net.minecraftforge.common.brewing.IBrewingRecipe> recipes = java.util.List.of();

        @Override
        public boolean isInput(ItemStack input) {
            return recipes.stream().anyMatch(recipe -> recipe.isInput(input));
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return recipes.stream().anyMatch(recipe -> recipe.isIngredient(ingredient));
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            for (var recipe : recipes) {
                ItemStack result = recipe.getOutput(input, ingredient);
                if (!result.isEmpty()) return result;
            }
            return ItemStack.EMPTY;
        }
    }

    /**
     * {@code type} is {@code potion} or {@code item}; {@code input}/{@code output} are
     * registry ids (potion ids for the potion type, item ids for the item type).
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
        ResourceLocation targetForm = recipeJson.has("target_form")
                ? ResourceLocation.tryParse(recipeJson.get("target_form").getAsString()) : null;
        String type = recipeJson.get("type").getAsString();
        net.minecraft.world.item.Item ingredientItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ingredient);
        if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(ingredient) || ingredientItem == Items.AIR) {
            ShapeShifterCurseForge.LOGGER.error("[ssc-brew] unknown ingredient item '{}'", ingredient);
            return;
        }
        Ingredient ingredientObject = Ingredient.of(ingredientItem);
        switch (type) {
            case "potion" -> {
                if (!net.minecraft.core.registries.BuiltInRegistries.POTION.containsKey(input)
                        || !net.minecraft.core.registries.BuiltInRegistries.POTION.containsKey(output)) {
                    ShapeShifterCurseForge.LOGGER.error("[ssc-brew] unknown potion in {} -> {}", input, output);
                    return;
                }
                Potion inputPotion = net.minecraft.core.registries.BuiltInRegistries.POTION.get(input);
                Potion outputPotion = net.minecraft.core.registries.BuiltInRegistries.POTION.get(output);
                ItemStack inputStack = PotionUtils.setPotion(new ItemStack(Items.POTION), inputPotion);
                ItemStack outputStack = PotionUtils.setPotion(new ItemStack(Items.POTION), outputPotion);
                add(new DynamicBrewingRecipe(inputStack, ingredientObject, outputStack, targetForm, true));
            }
            case "item" -> {
                if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(input)
                        || !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(output)) {
                    ShapeShifterCurseForge.LOGGER.error("[ssc-brew] unknown item in {} -> {}", input, output);
                    return;
                }
                net.minecraft.world.item.Item inputItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(input);
                net.minecraft.world.item.Item outputItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(output);
                add(new DynamicBrewingRecipe(new ItemStack(inputItem), ingredientObject, new ItemStack(outputItem), targetForm, false));
            }
            default -> ShapeShifterCurseForge.LOGGER.error("[ssc-brew] unknown recipe type '{}'", type);
        }
    }

    private static void add(net.minecraftforge.common.brewing.IBrewingRecipe recipe) {
        pending.add(recipe);
    }

    /**
     * Fabric's {@code BrewingRecipeRegistryMixin} stamps {@code targetForm} onto the brewed stack;
     * extending {@link BrewingRecipe} lets us do the same through {@link #getOutput}.
     */
    private static final class DynamicBrewingRecipe extends BrewingRecipe {
        private final @Nullable ResourceLocation targetForm;
        private final @Nullable Potion inputPotion;

        private DynamicBrewingRecipe(ItemStack input, Ingredient ingredient, ItemStack output,
                                     @Nullable ResourceLocation targetForm, boolean matchPotion) {
            super(Ingredient.of(input), ingredient, output);
            this.targetForm = targetForm;
            this.inputPotion = matchPotion ? PotionUtils.getPotion(input) : null;
        }

        @Override
        public boolean isInput(ItemStack input) {
            return super.isInput(input) && (inputPotion == null || PotionUtils.getPotion(input) == inputPotion);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            ItemStack result = super.getOutput(input, ingredient);
            if (targetForm != null && !result.isEmpty()) {
                result.getOrCreateTag().putString(TARGET_FORM_KEY, targetForm.toString());
            }
            return result;
        }
    }

    /** Fabric's {@code PotionItemMixin.finishUsing}: a {@code target_form} potion transforms the drinker. */
    @SubscribeEvent
    public static void onFinishUse(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItem();
        if (!(stack.getItem() instanceof PotionItem)) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TARGET_FORM_KEY)) {
            return;
        }
        ResourceLocation formId = ResourceLocation.tryParse(tag.getString(TARGET_FORM_KEY));
        if (formId != null) {
            TransformativeEffectService.apply(player, formId);
        }
    }
}
