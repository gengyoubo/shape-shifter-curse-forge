package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/** A Forge-side view of one retained Fabric Origins/Apoli power JSON file. */
public record FormPowerDefinition(ResourceLocation id, ResourceLocation type, JsonObject data) {
    public static FormPowerDefinition fromJson(ResourceLocation id, JsonObject data) {
        ResourceLocation type = ResourceLocation.tryParse(data.has("type") ? data.get("type").getAsString() : "");
        return new FormPowerDefinition(id, type, data);
    }
}
