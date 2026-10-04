package net.onixary.shapeShifterCurseForge.diet;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Datapack corrections are kept as selectors until server tags have finished binding. */
public record DietInheritanceConfig(Map<Integer, List<String>> seeds,
                                    Map<Integer, List<String>> whitelist,
                                    Map<Integer, List<String>> blacklist,
                                    List<String> foodBlacklist, List<String> neutralIngredients,
                                    List<String> recipeBlacklist) {
    static final Map<String, Integer> CATEGORIES = Map.of(
            "vegetarian", RecipeDietGraph.VEGETARIAN, "meat", RecipeDietGraph.MEAT,
            "fish", RecipeDietGraph.FISH, "ignore_diet", RecipeDietGraph.IGNORE_DIET);
    private static final Set<String> FIELDS = Set.of("replace", "seeds", "whitelist", "blacklist",
            "food_blacklist", "neutral_ingredients", "recipe_blacklist");
    public static final DietInheritanceConfig EMPTY = new DietInheritanceConfig(
            Map.of(), Map.of(), Map.of(), List.of(), List.of(), List.of());
    static volatile DietInheritanceConfig current = EMPTY;

    public static DietInheritanceConfig parse(JsonObject data) {
        for (String key : data.keySet()) {
            if (!FIELDS.contains(key)) throw new IllegalArgumentException("Unknown field: " + key);
        }
        if (data.has("replace") && (!data.get("replace").isJsonPrimitive()
                || !data.getAsJsonPrimitive("replace").isBoolean())) {
            throw new IllegalArgumentException("replace must be a boolean");
        }
        return new DietInheritanceConfig(categories(data, "seeds"), categories(data, "whitelist"),
                categories(data, "blacklist"), selectors(data, "food_blacklist"),
                selectors(data, "neutral_ingredients"), strings(data, "recipe_blacklist", true));
    }

    private static Map<Integer, List<String>> categories(JsonObject data, String field) {
        if (!data.has(field)) return Map.of();
        Map<Integer, List<String>> result = new LinkedHashMap<>();
        JsonObject object = data.getAsJsonObject(field);
        for (String key : object.keySet()) {
            Integer category = CATEGORIES.get(key);
            if (category == null) throw new IllegalArgumentException("Unknown diet category: " + key);
            result.put(category, selectors(object, key));
        }
        return Map.copyOf(result);
    }

    private static List<String> selectors(JsonObject data, String field) {
        return strings(data, field, false);
    }

    private static List<String> strings(JsonObject data, String field, boolean glob) {
        if (!data.has(field)) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonElement entry : data.getAsJsonArray(field)) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException(field + " entries must be strings");
            }
            String value = entry.getAsString();
            String id = !glob && value.startsWith("#") ? value.substring(1) : value;
            ResourceLocation parsed = ResourceLocation.tryParse(glob ? id.replace("*", "wildcard") : id);
            if (parsed == null || parsed.getPath().isEmpty()) {
                throw new IllegalArgumentException("Invalid " + field + " selector: " + value);
            }
            result.add(value);
        }
        return List.copyOf(result);
    }

    static Pattern recipePattern(String glob) {
        if (!glob.contains(":")) glob = "minecraft:" + glob;
        return Pattern.compile(java.util.Arrays.stream(glob.split("\\*", -1))
                .map(Pattern::quote).collect(java.util.stream.Collectors.joining(".*")));
    }

    private DietInheritanceConfig merge(DietInheritanceConfig other) {
        return new DietInheritanceConfig(mergeCategories(seeds, other.seeds),
                mergeCategories(whitelist, other.whitelist), mergeCategories(blacklist, other.blacklist),
                concat(foodBlacklist, other.foodBlacklist), concat(neutralIngredients, other.neutralIngredients),
                concat(recipeBlacklist, other.recipeBlacklist));
    }

    private static Map<Integer, List<String>> mergeCategories(Map<Integer, List<String>> first,
                                                              Map<Integer, List<String>> second) {
        Map<Integer, List<String>> result = new LinkedHashMap<>(first);
        second.forEach((category, values) -> result.merge(category, values, DietInheritanceConfig::concat));
        return Map.copyOf(result);
    }

    private static List<String> concat(List<String> first, List<String> second) {
        List<String> values = new ArrayList<>(first);
        values.addAll(second);
        return List.copyOf(values);
    }

    public static final class ReloadListener extends SimpleJsonResourceReloadListener {
        public ReloadListener() {
            super(new GsonBuilder().create(), "diet_inheritance");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager,
                             ProfilerFiller profiler) {
            DietInheritanceConfig combined = EMPTY;
            String defaults = ShapeShifterCurseForge.RESOURCE_NAMESPACE + ":default";
            List<ResourceLocation> ids = resources.keySet().stream().sorted(
                    Comparator.<ResourceLocation, Boolean>comparing(id -> !id.toString().equals(defaults))
                            .thenComparing(ResourceLocation::toString)).toList();
            for (ResourceLocation id : ids) {
                try {
                    JsonObject object = resources.get(id).getAsJsonObject();
                    DietInheritanceConfig parsed = parse(object);
                    boolean replace = object.has("replace") && object.get("replace").getAsBoolean();
                    combined = (replace ? EMPTY : combined).merge(parsed);
                } catch (RuntimeException exception) {
                    ShapeShifterCurseForge.LOGGER.error("Invalid diet inheritance config {}: {}", id,
                            exception.getMessage());
                }
            }
            current = combined;
        }
    }
}
