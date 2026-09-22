package net.onixary.shapeShifterCurseForge.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

/**
 * Fabric 1.10.0 parity: altar/alter/morph-scale upgrade recipe types.
 * Forge port stubs – they accept the fabric JSONs (type shape-shifter-curse:altar_shapeless etc.)
 * so datapack loading does not fail. Real crafting logic can be added later.
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

    private static class DummyRecipe implements Recipe<net.minecraft.world.Container> {
        private final net.minecraft.resources.ResourceLocation id;
        private final RecipeSerializer<?> serializer;
        DummyRecipe(net.minecraft.resources.ResourceLocation id, RecipeSerializer<?> ser) { this.id = id; this.serializer = ser; }
        DummyRecipe(net.minecraft.resources.ResourceLocation id) { this(id, null); }
        DummyRecipe() { this.id = ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, "dummy"); this.serializer = null; }
        @Override public boolean matches(net.minecraft.world.Container c, net.minecraft.world.level.Level l) { return false; }
        @Override public net.minecraft.world.item.ItemStack assemble(net.minecraft.world.Container c, net.minecraft.core.RegistryAccess a) { return net.minecraft.world.item.ItemStack.EMPTY; }
        @Override public boolean canCraftInDimensions(int w, int h) { return false; }
        @Override public net.minecraft.world.item.ItemStack getResultItem(net.minecraft.core.RegistryAccess a) { return net.minecraft.world.item.ItemStack.EMPTY; }
        @Override public net.minecraft.resources.ResourceLocation getId() { return id; }
        @Override public RecipeSerializer<?> getSerializer() { return serializer != null ? serializer : ALTAR_SHAPELESS.get(); }
        @Override public RecipeType<?> getType() { return ALTAR_SHAPELESS_TYPE.get(); }
    }

    public static final RegistryObject<RecipeType<?>> ALTAR_SHAPELESS_TYPE = TYPES.register("altar_shapeless", () -> new DummyRecipeType<>("altar_shapeless"));
    public static final RegistryObject<RecipeType<?>> ALTAR_SHAPED_TYPE = TYPES.register("altar_shaped", () -> new DummyRecipeType<>("altar_shaped"));
    public static final RegistryObject<RecipeType<?>> ALTER_SHAPELESS_TYPE = TYPES.register("alter_shapeless", () -> new DummyRecipeType<>("alter_shapeless"));
    public static final RegistryObject<RecipeType<?>> ALTER_SHAPED_TYPE = TYPES.register("alter_shaped", () -> new DummyRecipeType<>("alter_shaped"));
    public static final RegistryObject<RecipeType<?>> MORPH_SCALE_UPGRADE_TYPE = TYPES.register("morph_scale_upgrade", () -> new DummyRecipeType<>("morph_scale_upgrade"));

    public static final RegistryObject<RecipeSerializer<?>> ALTAR_SHAPELESS = SERIALIZERS.register("altar_shapeless", () -> new net.onixary.shapeShifterCurseForge.recipe.altar.AltarShapelessRecipe.Serializer());
    public static final RegistryObject<RecipeSerializer<?>> ALTAR_SHAPED = SERIALIZERS.register("altar_shaped", () -> new net.onixary.shapeShifterCurseForge.recipe.altar.AltarShapelessRecipe.Serializer());
    public static final RegistryObject<RecipeSerializer<?>> ALTER_SHAPELESS = SERIALIZERS.register("alter_shapeless", () -> new net.onixary.shapeShifterCurseForge.recipe.alter.AlterShapelessRecipe.Serializer());
    public static final RegistryObject<RecipeSerializer<?>> ALTER_SHAPED = SERIALIZERS.register("alter_shaped", () -> new net.onixary.shapeShifterCurseForge.recipe.alter.AlterShapelessRecipe.Serializer());
    public static final RegistryObject<RecipeSerializer<?>> MORPH_SCALE_UPGRADE = SERIALIZERS.register("morph_scale_upgrade", ModRecipeSerializers::makeDummySerializer);

    private ModRecipeSerializers() {}
}
