package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.form.FormManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.slf4j.Logger;

/**
 * Replaces Apoli's power registry for this mod only.  It reads the retained JSON directly from
 * the datapack, so normal Forge /reload also refreshes form powers without requiring Apoli.
 */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class FormPowerRegistry {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile Map<ResourceLocation, FormPowerDefinition> powers = Map.of();
    private static volatile Map<ResourceLocation, List<ResourceLocation>> formPowers = Map.of();

    private FormPowerRegistry() {
    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new PowerReloadListener());
        event.addListener(new OriginReloadListener());
    }

    public static FormPowerDefinition get(ResourceLocation id) {
        return powers.get(id);
    }

    public static Map<ResourceLocation, FormPowerDefinition> all() {
        return powers;
    }

    /** Compact server-side view used to diagnose the Forge-native power data pipeline. */
    public static DebugInfo debug(Player player) {
        List<ResourceLocation> assigned = idsFor(player);
        int resolved = (int) assigned.stream().filter(powers::containsKey).count();
        return new DebugInfo(powers.size(), formPowers.size(), FormManager.current(player).id(), assigned, resolved);
    }

    public static List<ResourceLocation> idsFor(Player player) {
        ResourceLocation formId = FormManager.current(player).id();

        List<ResourceLocation> direct = formPowers.get(formId);
        if (direct != null) {
            return direct;
        }

        ResourceLocation legacyOriginId = ResourceLocation.fromNamespaceAndPath(
                formId.getNamespace(),
                "form_" + formId.getPath()
        );

        return formPowers.getOrDefault(legacyOriginId, List.of());
    }

    public static boolean has(Player player, ResourceLocation id) {
        return idsFor(player).contains(id);
    }

    public static void visitActive(Player player, BiConsumer<ResourceLocation, JsonObject> visitor) {
        ResourceLocation formId = FormManager.current(player).id();
        List<ResourceLocation> ids = idsFor(player);

        //System.out.println("formId = " + formId);
        //System.out.println("power ids = " + ids);

        for (ResourceLocation id : ids) {
            FormPowerDefinition definition = powers.get(id);
            //System.out.println("power = " + id + ", definition = " + definition);

            if (definition != null) {
                visitDefinition(id, definition.data(), visitor);
            }
        }
    }

    private static void visitDefinition(ResourceLocation id, JsonObject data,
                                        BiConsumer<ResourceLocation, JsonObject> visitor) {
        visitor.accept(id, data);
        if (!"apoli:multiple".equals(typeOf(data))) {
            return;
        }

        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
            if (entry.getValue().isJsonObject() && entry.getValue().getAsJsonObject().has("type")) {
                visitDefinition(id, entry.getValue().getAsJsonObject(), visitor);
            }
        }
    }

    public static String typeOf(JsonObject data) {
        return data.has("type") ? data.get("type").getAsString() : "";
    }

    private static void replacePowers(Map<ResourceLocation, FormPowerDefinition> loaded) {
        powers = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
        LOGGER.info("Loaded {} Shape Shifter Curse power definitions", powers.size());
    }

    private static void replaceOrigins(Map<ResourceLocation, List<ResourceLocation>> loaded) {
        Map<ResourceLocation, List<ResourceLocation>> immutable = new LinkedHashMap<>();
        loaded.forEach((id, entries) -> immutable.put(id, List.copyOf(entries)));
        formPowers = Collections.unmodifiableMap(immutable);
        int assignments = formPowers.values().stream().mapToInt(List::size).sum();
        LOGGER.info("Loaded {} form power assignments across {} forms", assignments, formPowers.size());
    }

    private static final class PowerReloadListener extends SimpleJsonResourceReloadListener {
        private PowerReloadListener() {
            super(GSON, "powers");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, FormPowerDefinition> loaded = new LinkedHashMap<>();
            json.forEach((id, element) -> {
                if (element.isJsonObject()) {
                    loaded.put(id, FormPowerDefinition.fromJson(id, element.getAsJsonObject()));
                }
            });
            replacePowers(loaded);
        }
    }

    private static final class OriginReloadListener extends SimpleJsonResourceReloadListener {
        private OriginReloadListener() {
            super(GSON, "origins");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, List<ResourceLocation>> loaded = new LinkedHashMap<>();
            json.forEach((id, element) -> {
                if (!element.isJsonObject()) {
                    return;
                }
                JsonObject origin = element.getAsJsonObject();
                JsonArray entries = origin.has("powers") && origin.get("powers").isJsonArray()
                        ? origin.getAsJsonArray("powers") : null;
                if (entries == null) {
                    return;
                }

                Set<ResourceLocation> powerIds = new LinkedHashSet<>();
                for (JsonElement entry : entries) {
                    if (entry.isJsonPrimitive()) {
                        ResourceLocation powerId = ResourceLocation.tryParse(entry.getAsString());
                        if (powerId != null) {
                            powerIds.add(powerId);
                        }
                    }
                }
                loaded.put(id, new ArrayList<>(powerIds));
            });
            replaceOrigins(loaded);
        }
    }

    public record DebugInfo(int loadedPowers, int assignedForms, ResourceLocation currentForm,
                            List<ResourceLocation> assignedPowers, int resolvedPowers) {
    }
}
