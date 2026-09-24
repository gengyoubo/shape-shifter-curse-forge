package net.onixary.shapeShifterCurseForge.registry;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.recipe.altar.AltarShapedRecipe;
import net.onixary.shapeShifterCurseForge.recipe.altar.AltarShapelessRecipe;

/**
 * Fabric 1.10.0 parity: altar and morph-scale upgrade recipe types.
 */
public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(
            Registries.RECIPE_SERIALIZER, ShapeShifterCurseForge.RESOURCE_NAMESPACE);
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(
            Registries.RECIPE_TYPE, ShapeShifterCurseForge.RESOURCE_NAMESPACE);

    private record DummyRecipeType<T extends Recipe<?>>(String id) implements RecipeType<T> {
        @Override public String toString() { return ShapeShifterCurseForge.RESOURCE_NAMESPACE + ":" + id; }
    }

    private static RecipeSerializer<Recipe<?>> makeDummySerializer() {
        return new RecipeSerializer<>() {
            @Override public Recipe<?> fromJson(net.minecraft.resources.ResourceLocation id, com.google.gson.JsonObject json) { return new DummyRecipe(id, this); }
            @Override public Recipe<?> fromNetwork(net.minecraft.resources.ResourceLocation id, FriendlyByteBuf buf) { return new DummyRecipe(id, this); }
            @Override public void toNetwork(FriendlyByteBuf buf, Recipe<?> recipe) {}
        };
    }

    private record DummyRecipe(ResourceLocation id, RecipeSerializer<?> serializer) implements Recipe<Container> {
        DummyRecipe(ResourceLocation id) {
            this(id, null);
        }

        DummyRecipe() {
            this(ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, "dummy"), null);
        }

        @Override
        public boolean matches(Container c, Level l) {
            return false;
        }

        @Override
        public ItemStack assemble(Container c, RegistryAccess a) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canCraftInDimensions(int w, int h) {
            return false;
        }

        @Override
        public ItemStack getResultItem(RegistryAccess a) {
            return ItemStack.EMPTY;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return serializer != null ? serializer : ALTAR_SHAPELESS.get();
        }

        @Override
        public RecipeType<?> getType() {
            return ALTAR_SHAPELESS_TYPE.get();
        }
        }

    public static final RegistryObject<RecipeType<?>> ALTAR_SHAPELESS_TYPE = TYPES.register("altar_shapeless", () -> new DummyRecipeType<>("altar_shapeless"));
    public static final RegistryObject<RecipeType<?>> ALTAR_SHAPED_TYPE = TYPES.register("altar_shaped", () -> new DummyRecipeType<>("altar_shaped"));
    public static final RegistryObject<RecipeType<?>> MORPH_SCALE_UPGRADE_TYPE = TYPES.register("morph_scale_upgrade", () -> new DummyRecipeType<>("morph_scale_upgrade"));

    public static final RegistryObject<RecipeSerializer<?>> ALTAR_SHAPELESS = SERIALIZERS.register("altar_shapeless", AltarShapelessRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<?>> ALTAR_SHAPED = SERIALIZERS.register("altar_shaped", AltarShapedRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<?>> MORPH_SCALE_UPGRADE = SERIALIZERS.register(
            "morph_scale_upgrade", net.onixary.shapeShifterCurseForge.recipe.MorphScaleUpgradeRecipe.Serializer::new);

    private ModRecipeSerializers() {}
}
