package net.onixary.shapeShifterCurseForge.recipe.altar;

import com.google.gson.JsonElement;
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
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.registry.ModRecipeSerializers;

import java.util.HashMap;
import java.util.Map;

/**
 * Positional (shaped) counterpart of {@link AltarShapelessRecipe} for the Altar's
 * 3x3 grid (slots 0-8) plus catalyst slot 9. Pattern/key parsing follows the
 * vanilla shaped-recipe JSON format.
 */
public class AltarShapedRecipe implements AltarRecipe {
    private final ResourceLocation id;
    private final int width;
    private final int height;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final Ingredient catalyst;
    private final int recipeTime;
    private final int fuelCost;
    private final ResourceLocation requireAdvancement;

    public AltarShapedRecipe(ResourceLocation id, int width, int height, NonNullList<Ingredient> ingredients,
                             ItemStack result, Ingredient catalyst, int recipeTime, int fuelCost,
                             ResourceLocation requireAdvancement) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.result = result;
        this.catalyst = catalyst;
        this.recipeTime = recipeTime;
        this.fuelCost = fuelCost;
        this.requireAdvancement = requireAdvancement;
    }

    @Override public int getRecipeTime() { return recipeTime; }
    @Override public int getFuelCost() { return fuelCost; }
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
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                Ingredient expected = ingredientAt(x, y);
                ItemStack stack = inv.getItem(x + y * 3);
                if (expected.isEmpty()) {
                    if (!stack.isEmpty()) return false;
                } else if (!expected.test(stack)) {
                    return false;
                }
            }
        }
        return true;
    }

    public Ingredient ingredientAt(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return Ingredient.EMPTY;
        return ingredients.get(x + y * width);
    }

    @Override public boolean canCraftInDimensions(int w, int h) { return w >= width && h >= height; }

    @Override public ItemStack assemble(Container inv, RegistryAccess access) { return result.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess access) { return result.copy(); }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipeSerializers.ALTAR_SHAPED.get(); }
    @Override public RecipeType<?> getType() { return ModRecipeSerializers.ALTAR_SHAPED_TYPE.get(); }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public NonNullList<Ingredient> getIngredients() { return ingredients; }

    public static class Serializer implements RecipeSerializer<AltarShapedRecipe> {
        @Override
        public AltarShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
            Map<String, Ingredient> keys = readKeys(GsonHelper.getAsJsonObject(json, "key"));
            String[] pattern = shrinkPattern(readPattern(GsonHelper.getAsJsonArray(json, "pattern")));
            int width = pattern[0].length();
            int height = pattern.length;
            NonNullList<Ingredient> ings = NonNullList.withSize(width * height, Ingredient.EMPTY);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    char symbol = pattern[y].charAt(x);
                    if (symbol == ' ') continue;
                    Ingredient ing = keys.get(String.valueOf(symbol));
                    if (ing == null) {
                        throw new JsonParseException("Pattern references symbol '" + symbol
                                + "' but it is not defined in \"key\"");
                    }
                    ings.set(x + y * width, ing);
                }
            }
            Ingredient catalyst = null;
            if (json.has("catalyst")) catalyst = Ingredient.fromJson(json.get("catalyst"), false);
            ResourceLocation adv = null;
            if (json.has("require_advancement")) {
                adv = ResourceLocation.parse(GsonHelper.getAsString(json, "require_advancement"));
            }
            int time = GsonHelper.getAsInt(json, "time", 200);
            int fuel = GsonHelper.getAsInt(json, "fuel_cost", 1);
            ItemStack out = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new AltarShapedRecipe(id, width, height, ings, out, catalyst, time, fuel, adv);
        }

        @Override
        public AltarShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            int width = buf.readVarInt();
            int height = buf.readVarInt();
            NonNullList<Ingredient> ings = NonNullList.withSize(width * height, Ingredient.EMPTY);
            ings.replaceAll(ignored -> Ingredient.fromNetwork(buf));
            boolean hasCat = buf.readBoolean();
            Ingredient cat = hasCat ? Ingredient.fromNetwork(buf) : null;
            boolean hasAdv = buf.readBoolean();
            ResourceLocation adv = hasAdv ? buf.readResourceLocation() : null;
            ItemStack out = buf.readItem();
            int time = buf.readVarInt();
            int fuel = buf.readVarInt();
            return new AltarShapedRecipe(id, width, height, ings, out, cat, time, fuel, adv);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, AltarShapedRecipe recipe) {
            buf.writeVarInt(recipe.width);
            buf.writeVarInt(recipe.height);
            for (Ingredient ing : recipe.ingredients) ing.toNetwork(buf);
            if (recipe.catalyst != null && !recipe.catalyst.isEmpty()) {
                buf.writeBoolean(true);
                recipe.catalyst.toNetwork(buf);
            } else {
                buf.writeBoolean(false);
            }
            if (recipe.requireAdvancement != null) {
                buf.writeBoolean(true);
                buf.writeResourceLocation(recipe.requireAdvancement);
            } else {
                buf.writeBoolean(false);
            }
            buf.writeItem(recipe.result);
            buf.writeVarInt(recipe.recipeTime);
            buf.writeVarInt(recipe.fuelCost);
        }

        private static String[] readPattern(com.google.gson.JsonArray array) {
            if (array.isEmpty() || array.size() > 3) {
                throw new JsonParseException("Invalid pattern: must have 1-3 rows");
            }
            String[] rows = new String[array.size()];
            for (int i = 0; i < array.size(); i++) {
                String row = GsonHelper.convertToString(array.get(i), "pattern[" + i + "]");
                if (row.isEmpty() || row.length() > 3) {
                    throw new JsonParseException("Invalid pattern row length (must be 1-3): \"" + row + "\"");
                }
                rows[i] = row;
            }
            int width = rows[0].length();
            for (String row : rows) {
                if (row.length() != width) {
                    throw new JsonParseException("Invalid pattern: all rows must be the same width");
                }
            }
            return rows;
        }

        private static String[] shrinkPattern(String[] pattern) {
            int top = 0;
            int bottom = pattern.length - 1;
            while (top <= bottom && pattern[top].isBlank()) top++;
            while (bottom >= top && pattern[bottom].isBlank()) bottom--;
            if (top > bottom) throw new JsonParseException("Invalid pattern: empty pattern");
            int left = Integer.MAX_VALUE;
            int right = Integer.MIN_VALUE;
            for (int i = top; i <= bottom; i++) {
                String row = pattern[i];
                for (int j = 0; j < row.length(); j++) {
                    if (row.charAt(j) != ' ') {
                        left = Math.min(left, j);
                        right = Math.max(right, j);
                    }
                }
            }
            String[] shrunk = new String[bottom - top + 1];
            for (int i = top; i <= bottom; i++) {
                shrunk[i - top] = pattern[i].substring(left, right + 1);
            }
            return shrunk;
        }

        private static Map<String, Ingredient> readKeys(JsonObject json) {
            Map<String, Ingredient> keys = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (entry.getKey().length() != 1) {
                    throw new JsonParseException("Invalid key entry '" + entry.getKey()
                            + "': must be a single character");
                }
                if (" ".equals(entry.getKey())) {
                    throw new JsonParseException("Invalid key entry ' ': reserved character");
                }
                keys.put(entry.getKey(), Ingredient.fromJson(entry.getValue(), false));
            }
            keys.put(" ", Ingredient.EMPTY);
            return keys;
        }
    }
}
