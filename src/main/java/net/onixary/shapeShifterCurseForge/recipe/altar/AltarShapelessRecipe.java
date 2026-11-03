package net.onixary.shapeShifterCurseForge.recipe.altar;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.registry.ModRecipeSerializers;

public class AltarShapelessRecipe implements AltarRecipe {
    private final ResourceLocation id;
    private final ItemStack result;
    private final NonNullList<Ingredient> ingredients;
    private final Ingredient catalyst;
    private final int recipeTime;
    private final int fuelCost;
    private final int totalFuelCost;
    private final ResourceLocation requireAdvancement;

    public AltarShapelessRecipe(ResourceLocation id, ItemStack result, NonNullList<Ingredient> ingredients, Ingredient catalyst, int recipeTime, int fuelCost, ResourceLocation requireAdvancement) {
        this(id, result, ingredients, catalyst, recipeTime, fuelCost, requireAdvancement, -1);
    }

    public AltarShapelessRecipe(ResourceLocation id, ItemStack result, NonNullList<Ingredient> ingredients, Ingredient catalyst, int recipeTime, int fuelCost, ResourceLocation requireAdvancement, int totalFuelCost) {
        this.id = id;
        this.result = result;
        this.ingredients = ingredients;
        this.catalyst = catalyst;
        this.recipeTime = recipeTime;
        this.fuelCost = fuelCost;
        this.totalFuelCost = totalFuelCost;
        this.requireAdvancement = requireAdvancement;
    }

    @Override public int getRecipeTime() { return recipeTime; }
    @Override public int getFuelCost() { return fuelCost; }
    @Override public int getTotalFuelCost() { return totalFuelCost; }
    @Override public Ingredient getCatalyst() { return catalyst; }

    @Override
    public boolean canCraft(Player player) {
        if (requireAdvancement == null) return true;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return false;
        var server = sp.getServer();
        if (server == null) return false;
        var adv = server.getAdvancements().getAdvancement(requireAdvancement);
        if (adv == null) return false;
        var progress = sp.getAdvancements().getOrStartProgress(adv);
        return progress.isDone();
    }

    @Override
    public boolean matches(Container inv, Level level) {
        if (catalyst != null && !catalyst.isEmpty()) {
            ItemStack catStack = inv.getItem(9);
            if (!catalyst.test(catStack)) return false;
        }
        java.util.List<ItemStack> inputs = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) inputs.add(s);
        }
        if (inputs.size() != ingredients.size()) return false;
        int[] ingredientInput = new int[ingredients.size()];
        java.util.Arrays.fill(ingredientInput, -1);
        for (int i = 0; i < inputs.size(); i++) {
            if (!matchInput(inputs, i, ingredientInput, new boolean[ingredients.size()])) return false;
        }
        return true;
    }

    // A broad tag can overlap a more specific ingredient, so a greedy match is insufficient.
    private boolean matchInput(java.util.List<ItemStack> inputs, int index, int[] ingredientInput, boolean[] visited) {
        for (int i = 0; i < ingredients.size(); i++) {
            if (!visited[i] && ingredients.get(i).test(inputs.get(index))) {
                visited[i] = true;
                if (ingredientInput[i] == -1 || matchInput(inputs, ingredientInput[i], ingredientInput, visited)) {
                    ingredientInput[i] = index;
                    return true;
                }
            }
        }
        return false;
    }

    @Override public boolean canCraftInDimensions(int w, int h) { return w * h >= ingredients.size(); }

    @Override public ItemStack assemble(Container inv, RegistryAccess access) { return result.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess access) { return result.copy(); }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipeSerializers.ALTAR_SHAPELESS.get(); }
    @Override public RecipeType<?> getType() { return ModRecipeSerializers.ALTAR_SHAPELESS_TYPE.get(); }

    public NonNullList<Ingredient> getIngredients() { return ingredients; }

    public static class Serializer implements RecipeSerializer<AltarShapelessRecipe> {
        @Override
        public AltarShapelessRecipe fromJson(ResourceLocation id, JsonObject json) {
            int time = GsonHelper.getAsInt(json, "time", 200);
            JsonArray arr = GsonHelper.getAsJsonArray(json, "ingredients");
            NonNullList<Ingredient> ings = NonNullList.create();
            for (int i = 0; i < arr.size(); i++) {
                Ingredient ing = Ingredient.fromJson(arr.get(i), false);
                if (!ing.isEmpty()) ings.add(ing);
            }
            Ingredient catalyst = null;
            if (json.has("catalyst")) catalyst = Ingredient.fromJson(json.get("catalyst"), false);
            ResourceLocation adv = null;
            if (json.has("require_advancement")) adv = ResourceLocation.parse(GsonHelper.getAsString(json, "require_advancement"));
            int fuel = GsonHelper.getAsInt(json, "fuel_cost", 1);
            int totalFuel = AltarRecipe.totalFuelCost(json, time, fuel);
            if (ings.isEmpty()) throw new JsonParseException("No ingredients for altar shapeless recipe");
            if (ings.size() > 9) throw new JsonParseException("Too many ingredients");
            JsonObject res = GsonHelper.getAsJsonObject(json, "result");
            ItemStack out = net.minecraft.world.item.crafting.ShapedRecipe.itemStackFromJson(res);
            return new AltarShapelessRecipe(id, out, ings, catalyst, time, fuel, adv, totalFuel);
        }

        @Override
        public AltarShapelessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            boolean hasCat = buf.readBoolean();
            Ingredient cat = null;
            if (hasCat) cat = Ingredient.fromNetwork(buf);
            boolean hasAdv = buf.readBoolean();
            ResourceLocation adv = hasAdv ? buf.readResourceLocation() : null;
            int size = buf.readVarInt();
            NonNullList<Ingredient> ings = NonNullList.withSize(size, Ingredient.EMPTY);
            for (int i = 0; i < size; i++) ings.set(i, Ingredient.fromNetwork(buf));
            ItemStack out = buf.readItem();
            int time = buf.readVarInt();
            int fuel = buf.readVarInt();
            int totalFuel = buf.readInt();
            return new AltarShapelessRecipe(id, out, ings, cat, time, fuel, adv, totalFuel);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, AltarShapelessRecipe r) {
            if (r.catalyst != null && !r.catalyst.isEmpty()) { buf.writeBoolean(true); r.catalyst.toNetwork(buf); } else buf.writeBoolean(false);
            if (r.requireAdvancement != null) { buf.writeBoolean(true); buf.writeResourceLocation(r.requireAdvancement); } else buf.writeBoolean(false);
            buf.writeVarInt(r.ingredients.size());
            for (Ingredient ing : r.ingredients) ing.toNetwork(buf);
            buf.writeItem(r.result);
            buf.writeVarInt(r.recipeTime);
            buf.writeVarInt(r.fuelCost);
            buf.writeInt(r.totalFuelCost);
        }
    }
}
