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
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.util.TrinketUtils;

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
    private static volatile Map<ResourceLocation, FormPowerDefinition> dynamicPowers = Map.of();
    private static volatile Map<ResourceLocation, List<ResourceLocation>> dynamicPowerAdds = Map.of();
    private static volatile Map<ResourceLocation, List<ResourceLocation>> dynamicPowerRemoves = Map.of();
    private static volatile Map<ResourceLocation, List<ResourceLocation>> extraPowerAdds = Map.of();

    private FormPowerRegistry() {
    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new PowerReloadListener());
        event.addListener(new OriginReloadListener());
        event.addListener(new DynamicFormReloadListener());
        event.addListener(new ExtraPowerReloadListener());
        event.addListener(new AccessoryPowerReloadListener());
    }

    /**
     * Registers the same data pipeline on the client resource manager.  AddReloadListenerEvent
     * is server-only, so without this mirror a synced dynamic form is unknown on dedicated
     * clients and FormManager falls back to the pre-enable form.
     */
    public static void addClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new PowerReloadListener());
        event.registerReloadListener(new OriginReloadListener());
        event.registerReloadListener(new DynamicFormReloadListener());
        event.registerReloadListener(new ExtraPowerReloadListener());
        event.registerReloadListener(new AccessoryPowerReloadListener());
    }

    public static FormPowerDefinition get(ResourceLocation id) {
        FormPowerDefinition dynamic = dynamicPowers.get(id);
        return dynamic == null ? powers.get(id) : dynamic;
    }

    public static Map<ResourceLocation, FormPowerDefinition> all() {
        Map<ResourceLocation, FormPowerDefinition> all = new LinkedHashMap<>(powers);
        all.putAll(dynamicPowers);
        return Collections.unmodifiableMap(all);
    }

    /** Compact server-side view used to diagnose the Forge-native power data pipeline. */
    public static DebugInfo debug(Player player) {
        List<ResourceLocation> assigned = idsFor(player);
        int resolved = (int) assigned.stream().filter(id -> get(id) != null).count();
        return new DebugInfo(all().size(), formPowers.size(), FormManager.current(player).id(), assigned, resolved);
    }

    public static List<ResourceLocation> idsFor(Player player) {
        ResourceLocation formId = FormManager.current(player).id();
        ResourceLocation legacyOriginId = ResourceLocation.fromNamespaceAndPath(
                formId.getNamespace(),
                "form_" + formId.getPath()
        );
        ResourceLocation customOriginId = FormRegistry.originIdFor(formId);
        LinkedHashSet<ResourceLocation> originKeys = new LinkedHashSet<>();
        originKeys.add(formId);
        originKeys.add(legacyOriginId);
        if (customOriginId != null) originKeys.add(customOriginId);

        LinkedHashSet<ResourceLocation> assigned = new LinkedHashSet<>();
        LinkedHashSet<ResourceLocation> removed = new LinkedHashSet<>();
        for (ResourceLocation originKey : originKeys) {
            addAll(assigned, formPowers.get(originKey));
            addAll(assigned, dynamicPowerAdds.get(originKey));
            addAll(assigned, extraPowerAdds.get(originKey));
            addAll(removed, dynamicPowerRemoves.get(originKey));
        }
        assigned.removeAll(removed);
        return TrinketUtils.effectivePowerIds(player, List.copyOf(assigned));
    }

    private static void addAll(Set<ResourceLocation> target, List<ResourceLocation> values) {
        if (values != null) target.addAll(values);
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
            FormPowerDefinition definition = get(id);
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

    private static Map<ResourceLocation, List<ResourceLocation>> immutableLists(
            Map<ResourceLocation, List<ResourceLocation>> loaded) {
        Map<ResourceLocation, List<ResourceLocation>> result = new LinkedHashMap<>();
        loaded.forEach((id, entries) -> result.put(id, List.copyOf(entries)));
        return Collections.unmodifiableMap(result);
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

    /** Loads the Fabric mod's non-standard data/ssc_form/*.json files. */
    private static final class DynamicFormReloadListener extends SimpleJsonResourceReloadListener {
        private DynamicFormReloadListener() {
            super(GSON, "ssc_form");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager manager,
                             ProfilerFiller profiler) {
            FormRegistry.reloadDynamicForms(json);
            Map<ResourceLocation, FormPowerDefinition> loadedPowers = new LinkedHashMap<>();
            Map<ResourceLocation, List<ResourceLocation>> additions = new LinkedHashMap<>();
            Map<ResourceLocation, List<ResourceLocation>> removals = new LinkedHashMap<>();

            json.forEach((resourceId, element) -> {
                if (!element.isJsonObject()) return;
                JsonObject form = element.getAsJsonObject();
                ResourceLocation formId = resourceLocation(form, "FormID", resourceId);
                if (formId == null) return;
                List<ResourceLocation> add = new ArrayList<>();
                List<ResourceLocation> remove = new ArrayList<>();
                int index = 0;
                JsonArray extra = form.has("ExtraPower") && form.get("ExtraPower").isJsonArray()
                        ? form.getAsJsonArray("ExtraPower") : new JsonArray();
                for (JsonElement power : extra) {
                    if (power.isJsonPrimitive()) {
                        ResourceLocation id = ResourceLocation.tryParse(power.getAsString());
                        if (id != null) add.add(id);
                    } else if (power.isJsonObject()) {
                        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                                formId.getNamespace(), formId.getPath() + "_tpower_" + index++);
                        loadedPowers.put(id, FormPowerDefinition.fromJson(id, power.getAsJsonObject()));
                        add.add(id);
                    }
                }
                JsonArray removed = form.has("RemovedPower") && form.get("RemovedPower").isJsonArray()
                        ? form.getAsJsonArray("RemovedPower") : new JsonArray();
                for (JsonElement power : removed) {
                    if (power.isJsonPrimitive()) {
                        ResourceLocation id = ResourceLocation.tryParse(power.getAsString());
                        if (id != null) remove.add(id);
                    }
                }
                if (!add.isEmpty()) additions.put(formId, add);
                if (!remove.isEmpty()) removals.put(formId, remove);
            });
            dynamicPowers = Collections.unmodifiableMap(loadedPowers);
            dynamicPowerAdds = immutableLists(additions);
            dynamicPowerRemoves = immutableLists(removals);
            LOGGER.info("Loaded {} dynamic forms and {} inline dynamic powers", json.size(), loadedPowers.size());
        }
    }

    /** Loads Fabric's origins_power_extra bridge and applies it to the matching form origin. */
    private static final class ExtraPowerReloadListener extends SimpleJsonResourceReloadListener {
        private ExtraPowerReloadListener() {
            super(GSON, "origins_power_extra");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager manager,
                             ProfilerFiller profiler) {
            Map<ResourceLocation, List<ResourceLocation>> loaded = new LinkedHashMap<>();
            json.forEach((id, element) -> {
                if (!element.isJsonObject()) return;
                JsonObject data = element.getAsJsonObject();
                ResourceLocation target = resourceLocation(data, "TargetOriginsID", null);
                JsonArray entries = data.has("ExtraPowers") && data.get("ExtraPowers").isJsonArray()
                        ? data.getAsJsonArray("ExtraPowers") : null;
                if (target == null || entries == null) return;
                List<ResourceLocation> powers = new ArrayList<>();
                for (JsonElement entry : entries) {
                    if (entry.isJsonPrimitive()) {
                        ResourceLocation power = ResourceLocation.tryParse(entry.getAsString());
                        if (power != null) powers.add(power);
                    }
                }
                if (!powers.isEmpty()) loaded.computeIfAbsent(target, ignored -> new ArrayList<>()).addAll(powers);
            });
            extraPowerAdds = immutableLists(loaded);
            LOGGER.info("Loaded {} origins_power_extra assignments", extraPowerAdds.size());
        }
    }

    private static ResourceLocation resourceLocation(JsonObject data, String key, ResourceLocation fallback) {
        if (!data.has(key) || !data.get(key).isJsonPrimitive()) return fallback;
        return ResourceLocation.tryParse(data.get(key).getAsString());
    }

    private static final class AccessoryPowerReloadListener extends SimpleJsonResourceReloadListener {
        private AccessoryPowerReloadListener() {
            super(GSON, "accessory_power");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager manager,
                             ProfilerFiller profiler) {
            TrinketUtils.clearAccessoryPower();
            json.values().forEach(element -> {
                if (element.isJsonObject()) TrinketUtils.loadAccessoryPowerData(element.getAsJsonObject());
            });
            LOGGER.info("Loaded {} accessory power definitions", TrinketUtils.accessoryPowerRegistry.size());
        }
    }

    public record DebugInfo(int loadedPowers, int assignedForms, ResourceLocation currentForm,
                            List<ResourceLocation> assignedPowers, int resolvedPowers) {
    }
}
