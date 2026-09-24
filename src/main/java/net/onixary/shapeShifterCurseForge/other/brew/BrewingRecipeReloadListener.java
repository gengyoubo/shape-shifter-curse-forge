package net.onixary.shapeShifterCurseForge.other.brew;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.util.Map;

/**
 * Forge port of Fabric's {@code BrewingRecipeReloadListener}: loads
 * {@code data/<namespace>/dynamic_brewing_recipes/*.json} and registers them into
 * {@link net.minecraftforge.common.brewing.BrewingRecipeRegistry}.
 */
public final class BrewingRecipeReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    public BrewingRecipeReloadListener() {
        super(GSON, "dynamic_brewing_recipes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager manager, ProfilerFiller profiler) {
        BrewingRecipeUtils.onLoadDynamicBrewingRecipesStart();
        int count = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : json.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            BrewingRecipeUtils.registerPotionRecipe(entry.getValue().getAsJsonObject());
            count++;
        }
        BrewingRecipeUtils.onLoadDynamicBrewingRecipesEnd();
        ShapeShifterCurseForge.LOGGER.info("Loaded {} dynamic brewing recipes", count);
    }
}