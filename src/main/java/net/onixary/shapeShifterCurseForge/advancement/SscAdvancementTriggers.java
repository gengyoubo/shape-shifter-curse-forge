package net.onixary.shapeShifterCurseForge.advancement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Forge counterparts for the custom advancement triggers used by the Fabric build. */
public final class SscAdvancementTriggers {
    public static final SscTrigger ON_ENABLE_MOD = register("on_enable_mod");
    public static final SscTrigger ON_END_CURSED_MOON = register("on_end_cursed_moon");
    public static final SscTrigger ON_END_CURSED_MOON_CURED = register("on_end_cursed_moon_cured");
    public static final SscTrigger ON_END_CURSED_MOON_CURED_FORM_2 = register("on_end_cursed_moon_cured_form_2");
    public static final SscTrigger ON_FIRST_JOIN_WITH_MOD = register("on_first_join_with_mod");
    public static final SscTrigger ON_GET_TRANSFORM_EFFECT = register("on_get_transform_effect");
    public static final SscTrigger ON_OPEN_BOOK_OF_SHAPE_SHIFTER = register("on_open_book_of_shape_shifter");
    public static final SscTrigger ON_SLEEP_WHEN_HAVE_TRANSFORM_EFFECT = register("on_sleep_when_have_transform_effect");
    public static final SscTrigger ON_TRANSFORM_BY_CATALYST = register("on_transform_by_catalyst");
    public static final SscTrigger ON_TRANSFORM_BY_CURE = register("on_transform_by_cure");
    public static final SscTrigger ON_TRANSFORM_BY_CURE_FINAL = register("on_transform_by_cure_final");
    public static final SscTrigger ON_TRANSFORM_EFFECT_FADE = register("on_transform_effect_fade");
    public static final SscTrigger ON_TRANSFORM_FORM = register("on_transform_form");
    public static final SscTrigger ON_TRIGGER_CURSED_MOON = register("on_trigger_cursed_moon");
    public static final SscTrigger ON_TRIGGER_CURSED_MOON_FORM_2 = register("on_trigger_cursed_moon_form_2");
    public static final SscTrigger ON_USE_GOLDEN_APPLE = register("on_use_golden_apple");
    public static final SscTrigger ON_WEB_ENTITY = register("on_web_entity");

    private SscAdvancementTriggers() {
    }

    public static void initialize() {
        // Forces class initialization before datapack advancements are parsed.
    }

    private static SscTrigger register(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                ShapeShifterCurseForge.RESOURCE_NAMESPACE, path);
        return CriteriaTriggers.register(new SscTrigger(id));
    }

    public static final class SscTrigger extends SimpleCriterionTrigger<SscInstance> {
        private final ResourceLocation id;

        private SscTrigger(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        protected SscInstance createInstance(JsonObject json, ContextAwarePredicate playerPredicate,
                                              DeserializationContext context) {
            return new SscInstance(id, playerPredicate,
                    resourceLocations(json, "form"), integers(json, "form_tier"),
                    strings(json, "flags"), strings(json, "not_flags"),
                    resourceLocations(json, "entity"));
        }

        public void trigger(ServerPlayer player) {
            trigger(player, ignored -> true);
        }

        public void triggerForm(ServerPlayer player, FormDefinition form) {
            trigger(player, condition -> condition.matchesForm(form));
        }

        public void triggerEntity(ServerPlayer player, ResourceLocation entityId) {
            trigger(player, condition -> condition.matchesEntity(entityId));
        }
    }

    public static final class SscInstance extends AbstractCriterionTriggerInstance {
        private final List<ResourceLocation> forms;
        private final List<Integer> tiers;
        private final List<String> flags;
        private final List<String> notFlags;
        private final List<ResourceLocation> entities;

        private SscInstance(ResourceLocation id, ContextAwarePredicate playerPredicate,
                            List<ResourceLocation> forms, List<Integer> tiers,
                            List<String> flags, List<String> notFlags, List<ResourceLocation> entities) {
            super(id, playerPredicate);
            this.forms = forms;
            this.tiers = tiers;
            this.flags = flags;
            this.notFlags = notFlags;
            this.entities = entities;
        }

        private boolean matchesForm(FormDefinition form) {
            if (!forms.isEmpty() && !forms.contains(form.id())) return false;
            if (!tiers.isEmpty() && !tiers.contains(form.tier())) return false;
            if (!flags.isEmpty() && !form.flags().containsAll(flags)) return false;
            return notFlags.isEmpty() || !form.flags().containsAll(notFlags);
        }

        private boolean matchesEntity(ResourceLocation entityId) {
            return entities.isEmpty() || entities.stream().anyMatch(candidate -> Objects.equals(candidate, entityId));
        }
    }

    private static List<String> strings(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) result.add(element.getAsString());
        }
        return List.copyOf(result);
    }

    private static List<ResourceLocation> resourceLocations(JsonObject json, String key) {
        List<ResourceLocation> result = new ArrayList<>();
        for (String value : strings(json, key)) {
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id != null) result.add(id);
        }
        return List.copyOf(result);
    }

    private static List<Integer> integers(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray()) return List.of();
        List<Integer> result = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray(key)) {
            try {
                if (element.isJsonPrimitive()) result.add(element.getAsInt());
            } catch (RuntimeException ignored) {
                // Malformed optional condition values simply do not match.
            }
        }
        return List.copyOf(result);
    }
}
