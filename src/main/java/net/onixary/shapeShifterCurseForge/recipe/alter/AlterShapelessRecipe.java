package net.onixary.shapeShifterCurseForge.recipe.alter;

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
import net.onixary.shapeShifterCurseForge.recipe.altar.AltarRecipe;
import net.onixary.shapeShifterCurseForge.registry.ModRecipeSerializers;

public class AlterShapelessRecipe implements AltarRecipe {
    private final ResourceLocation id;
    private final ItemStack result;
    private final NonNullList<Ingredient> ingredients;
    private final Ingredient catalyst;
    private final int recipeTime;
    private final int fuelCost;
    private final ResourceLocation requireAdvancement;

    public AlterShapelessRecipe(ResourceLocation id, ItemStack result, NonNullList<Ingredient> ingredients, Ingredient catalyst, int recipeTime, int fuelCost, ResourceLocation requireAdvancement) {
        this.id = id; this.result = result; this.ingredients = ingredients; this.catalyst = catalyst; this.recipeTime = recipeTime; this.fuelCost = fuelCost; this.requireAdvancement = requireAdvancement;
    }
    @Override public int getRecipeTime() { return recipeTime; }
    @Override public int getFuelCost() { return fuelCost; }
    @Override public Ingredient getCatalyst() { return catalyst; }
    @Override public boolean canCraft(Player p) {
        if (requireAdvancement == null) return true;
        if (!(p instanceof net.minecraft.server.level.ServerPlayer sp)) return false;
        var server = sp.getServer(); if (server == null) return false;
        var adv = server.getAdvancements().getAdvancement(requireAdvancement);
        if (adv == null) return false;
        var prog = sp.getAdvancements().getOrStartProgress(adv);
        return prog.isDone();
    }
    @Override public boolean matches(Container inv, Level level) {
        if (catalyst != null && !catalyst.isEmpty()) {
            ItemStack cat = inv.getItem(9);
            if (!catalyst.test(cat)) return false;
        }
        java.util.List<ItemStack> inputs = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) { var s = inv.getItem(i); if (!s.isEmpty()) inputs.add(s); }
        if (inputs.size() != ingredients.size()) return false;
        java.util.List<Ingredient> rem = new java.util.ArrayList<>(ingredients);
        for (ItemStack s : inputs) {
            boolean m = false; var it = rem.iterator();
            while (it.hasNext()) { if (it.next().test(s)) { it.remove(); m = true; break; } }
            if (!m) return false;
        }
        return rem.isEmpty();
    }
    @Override public boolean canCraftInDimensions(int w, int h) { return w*h >= ingredients.size(); }
    @Override public ItemStack assemble(Container inv, RegistryAccess a) { return result.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess a) { return result.copy(); }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipeSerializers.ALTER_SHAPELESS.get(); }
    @Override public RecipeType<?> getType() { return ModRecipeSerializers.ALTER_SHAPELESS_TYPE.get(); }
    public NonNullList<Ingredient> getIngredients() { return ingredients; }

    public static class Serializer implements RecipeSerializer<AlterShapelessRecipe> {
        @Override public AlterShapelessRecipe fromJson(ResourceLocation id, JsonObject json) {
            int time = GsonHelper.getAsInt(json, "time", 200);
            JsonArray arr = GsonHelper.getAsJsonArray(json, "ingredients");
            NonNullList<Ingredient> ings = NonNullList.create();
            for (int i=0;i<arr.size();i++) { var ing = Ingredient.fromJson(arr.get(i), false); if (!ing.isEmpty()) ings.add(ing); }
            Ingredient cat = json.has("catalyst") ? Ingredient.fromJson(json.get("catalyst"), false) : null;
            ResourceLocation adv = json.has("require_advancement") ? ResourceLocation.parse(GsonHelper.getAsString(json, "require_advancement")) : null;
            int fuel = GsonHelper.getAsInt(json, "fuel_cost", 1);
            if (ings.isEmpty()) throw new JsonParseException("No ingredients");
            if (ings.size()>9) throw new JsonParseException("Too many");
            ItemStack out = net.minecraft.world.item.crafting.ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new AlterShapelessRecipe(id, out, ings, cat, time, fuel, adv);
        }
        @Override public AlterShapelessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            boolean hasCat = buf.readBoolean(); Ingredient cat = hasCat ? Ingredient.fromNetwork(buf) : null;
            boolean hasAdv = buf.readBoolean(); ResourceLocation adv = hasAdv ? buf.readResourceLocation() : null;
            int size = buf.readVarInt(); NonNullList<Ingredient> ings = NonNullList.withSize(size, Ingredient.EMPTY);
            for (int i=0;i<size;i++) ings.set(i, Ingredient.fromNetwork(buf));
            ItemStack out = buf.readItem(); int time = buf.readVarInt(); int fuel = buf.readVarInt();
            return new AlterShapelessRecipe(id, out, ings, cat, time, fuel, adv);
        }
        @Override public void toNetwork(FriendlyByteBuf buf, AlterShapelessRecipe r) {
            if (r.catalyst!=null && !r.catalyst.isEmpty()) { buf.writeBoolean(true); r.catalyst.toNetwork(buf); } else buf.writeBoolean(false);
            if (r.requireAdvancement!=null) { buf.writeBoolean(true); buf.writeResourceLocation(r.requireAdvancement); } else buf.writeBoolean(false);
            buf.writeVarInt(r.ingredients.size()); for (var ing: r.ingredients) ing.toNetwork(buf);
            buf.writeItem(r.result); buf.writeVarInt(r.recipeTime); buf.writeVarInt(r.fuelCost);
        }
    }
}
