package net.onixary.shapeShifterCurseForge.form;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.api.registry.Evolution;
import net.onixary.shapeShifterCurseForge.api.registry.SscForm;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FormRegistry {
    public static final ResourceLocation ORIGINAL_BEFORE_ENABLE = id("original_before_enable");
    public static final ResourceLocation ORIGINAL_SHIFTER = id("original_shifter");

    private static final Map<ResourceLocation, FormDefinition> FORMS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, FormGroup> GROUPS = new LinkedHashMap<>();
    private static final Set<ResourceLocation> DYNAMIC_FORMS = new LinkedHashSet<>();
    private static final Set<ResourceLocation> DYNAMIC_GROUPS = new LinkedHashSet<>();
    private static final Map<ResourceLocation, ResourceLocation> DYNAMIC_ORIGIN_IDS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, SscForm> JAVA_FORMS = new LinkedHashMap<>();
    /** Legacy builder-defined branch links, retained for source compatibility. */
    private static final Map<ResourceLocation, ResourceLocation> JAVA_VARIANT_PARENTS = new LinkedHashMap<>();
    private static final List<Evolution> JAVA_EVOLUTIONS = new ArrayList<>();
    private static final Set<ResourceLocation> RESOLVED_JAVA_FORMS = new LinkedHashSet<>();
    private static final Set<ResourceLocation> JAVA_GROUPS = new LinkedHashSet<>();
    private static boolean javaFormsDirty;
    private static boolean bootstrapped;

    private FormRegistry() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        add("original_before_enable", "base_form", -1, 1, FormBodyType.NORMAL, 1.0F, 1.0F, 1.0F,
                "no_instinct", "inhibitor_immune", "no_cursed_moon_effect", "no_cursed_moon_target");
        add("original_shifter", "base_form", 0, 1, FormBodyType.NORMAL, 1.0F, 1.0F, 1.0F,
                "can_have_transform_effect", "transform_effect_can_apply", "no_instinct", "inhibitor_immune", "no_cursed_moon_target");

        addCreatureGroup("bat", new float[]{0.90F, 0.75F, 0.60F, 0.60F}, new float[]{1.0F, 1.0F, 1.0F, 0.70F}, FormBodyType.NORMAL);
        addCreatureGroup("axolotl", new float[]{1.0F, 1.0F, 0.90F, 0.90F}, null, FormBodyType.NORMAL);
        addCreatureGroup("ocelot", new float[]{0.95F, 0.85F, 0.65F, 0.75F}, new float[]{1.0F, 1.0F, 1.0F, 0.60F}, FormBodyType.NORMAL);
        addCreatureGroup("familiar_fox", new float[]{0.80F, 0.65F, 0.55F, 0.55F}, new float[]{1.0F, 1.0F, 1.0F, 0.60F}, FormBodyType.NORMAL);
        addCreatureGroup("snow_fox", new float[]{0.80F, 0.65F, 0.55F, 0.55F}, new float[]{1.0F, 1.0F, 1.0F, 0.60F}, FormBodyType.NORMAL);
        addCreatureGroup("anubis_wolf", new float[]{1.0F, 1.0F, 0.90F, 0.80F}, new float[]{1.0F, 1.0F, 1.0F, 0.60F}, FormBodyType.NORMAL);
        addCreatureGroup("spider", new float[]{1.0F, 0.85F, 0.90F, 0.90F}, null, FormBodyType.NORMAL);

        add("allay_sp", "allay_form", 1, 1, FormBodyType.NORMAL, 0.55F, 0.55F, 1.0F,
                "no_instinct", "no_cursed_moon_effect", "special_form");
        add("feral_cat_sp", "feral_cat_form", 1, 1, FormBodyType.FERAL, 0.55F, 0.55F, 0.60F,
                "no_instinct", "no_cursed_moon_effect", "special_form");

        add("snow_fox_3_sub_marbled_polecat", "snow_fox_form", 4, 1, FormBodyType.FERAL, 0.55F, 0.55F, 0.60F,
                "sub_form");
        add("bat_3_sub_avali", "bat_form", 4, 1, FormBodyType.NORMAL, 0.65F, 0.65F, 1.0F,
                "sub_form");
    }

    private static void addCreatureGroup(String path, float[] widths, float[] eyes, FormBodyType finalBodyType) {
        String groupPath = path + "_form";
        for (int index = 0; index < widths.length; index++) {
            int tier = index + 1;
            Set<String> flags = new LinkedHashSet<>(switch (tier) {
                case 1 -> Set.of("starter_form");
                case 3 -> Set.of("inhibitor_resist", "lock_instinct", "cursed_moon_final_form", "catalyst_resist", "can_transform_to_final_form");
                case 4 -> Set.of("final_form", "inhibitor_immune", "no_instinct", "no_cursed_moon_effect");
                default -> Set.of("");
            });
            if (path.equals("bat") && tier >= 1) {
                flags.add("night_vision");
            }
            if (path.equals("bat") && tier >= 3) {
                flags.add("slow_fall");
            }
            if (path.equals("axolotl") && tier >= 1) {
                flags.add("water_breathing");
            }
            if (path.equals("spider") && tier >= 3) {
                flags.add("night_vision");
                flags.add("climb");
            }
            if (path.equals("spider") && tier <= 1) {
                flags.add("catalyst_immune");
            }
            if (path.equals("spider") && (tier == 1 || tier >= 3)) {
                flags.add("poison_immune");
            }
            FormBodyType bodyType = tier == 4 && (path.equals("ocelot") || path.contains("fox") || path.equals("anubis_wolf"))
                    ? finalBodyType == FormBodyType.NORMAL ? FormBodyType.FERAL : finalBodyType
                    : FormBodyType.NORMAL;
            float eyeScale = eyes == null ? 1.0F : eyes[index];
            float fallProtection = switch (path + "_" + tier) {
                case "bat_2" -> 2.5F;
                case "axolotl_2", "axolotl_3" -> 6.0F;
                case "snow_fox_3" -> 1.5F;
                case "snow_fox_4" -> 2.5F;
                case "spider_1", "spider_3", "spider_4" -> 4.0F;
                default -> 0.0F;
            };
            float jumpBoost = switch (path + "_" + tier) {
                case "bat_4" -> 0.2F;
                case "ocelot_4" -> 0.25F;
                default -> 0.0F;
            };
            add(path + "_" + index, groupPath, tier, 1, bodyType, widths[index], widths[index], eyeScale,
                    fallProtection, jumpBoost, flags.toArray(String[]::new));
        }
    }

    private static void add(String path, String groupPath, int tier, int weight, FormBodyType bodyType,
                             float widthScale, float heightScale, float eyeScale, String... flags) {
        add(path, groupPath, tier, weight, bodyType, widthScale, heightScale, eyeScale,
                0.0F, 0.0F, flags);
    }

    private static void add(String path, String groupPath, int tier, int weight, FormBodyType bodyType,
                             float widthScale, float heightScale, float eyeScale,
                             float fallProtectionDistance, float jumpVelocityAddition, String... flags) {
        ResourceLocation groupId = id(groupPath);
        FormDefinition definition = new FormDefinition(id(path), groupId, tier, weight, bodyType,
                widthScale, heightScale, eyeScale, Set.of(flags), fallProtectionDistance, jumpVelocityAddition);
        FORMS.put(definition.id(), definition);
        GROUPS.computeIfAbsent(groupId, FormGroup::new).add(definition);
    }

    /**
     * Replaces external {@code data/<namespace>/ssc_form/<form>.json} forms.
     *
     * <p>The external format deliberately follows the built-in convention: a form called
     * {@code namespace:catgirl_0} must live in {@code ssc_form/catgirl_0.json} and point to
     * {@code namespace:form_catgirl_0}. Keeping those ids deterministic makes the form,
     * its Origin data and its client model metadata resolve as one unit.</p>
     */
    public static void reloadDynamicForms(Map<ResourceLocation, JsonElement> json) {
        reloadDynamicForms(json, null);
    }

    /** Same as {@link #reloadDynamicForms(Map)}, also validates that every mapped Origin exists. */
    public static void reloadDynamicForms(Map<ResourceLocation, JsonElement> json, ResourceManager resourceManager) {
        bootstrap();
        javaFormsDirty = true;
        for (ResourceLocation formId : DYNAMIC_FORMS) {
            FormDefinition old = FORMS.remove(formId);
            if (old != null) {
                FormGroup group = GROUPS.get(old.groupId());
                if (group != null) {
                    group.remove(formId);
                    if (group.isEmpty() && DYNAMIC_GROUPS.contains(old.groupId())) {
                        GROUPS.remove(old.groupId());
                    }
                }
            }
        }
        DYNAMIC_FORMS.clear();
        DYNAMIC_GROUPS.clear();
        DYNAMIC_ORIGIN_IDS.clear();

        json.forEach((resourceId, element) -> {
            if (!element.isJsonObject()) {
                LOGGER.warn("Ignoring non-object dynamic form {}", resourceId);
                return;
            }
            JsonObject data = element.getAsJsonObject();
            // Deliberately fail the resource reload.  A partially registered form can leave
            // the player data, Origin powers and client model on different ids, which is much
            // harder to diagnose than a precise startup/reload error.
            DynamicFormIds ids = validateDynamicForm(resourceId, data, resourceManager);
            ResourceLocation formId = ids.formId();
            if (FORMS.containsKey(formId) && !DYNAMIC_FORMS.contains(formId)) {
                LOGGER.warn("Ignoring dynamic form {} because it would replace a built-in form", formId);
                return;
            }
            ResourceLocation groupId = resourceLocation(data, "group", formId);
            if (groupId == null) {
                LOGGER.warn("Ignoring dynamic form {} with invalid group", formId);
                return;
            }
            int tier = data.has("stage") ? integer(data, "stage", 1) : integer(data, "tier", 1);
            int weight = integer(data, "weight", data.has("group_weight") ? integer(data, "group_weight", 1) : 1);
            FormBodyType bodyType = bodyType(data);
            float width = number(data, "widthScale", number(data, "width_scale", 1.0F));
            float height = number(data, "heightScale", number(data, "height_scale", 1.0F));
            float eye = number(data, "eyeScale", number(data, "eye_scale", 1.0F));
            float fallProtection = number(data, "fallProtectionDistance",
                    number(data, "fall_protection_distance", 0.0F));
            float jumpAddition = number(data, "jumpVelocityAddition",
                    number(data, "jump_velocity_addition", 0.0F));
            boolean fullyCustomModel = booleanValue(data, "fullyCustomModel",
                    booleanValue(data, "fully_custom_model", false));
            Set<String> flags = flags(data);

            FormDefinition definition = new FormDefinition(formId, groupId, tier, weight, bodyType,
                    width, height, eye, flags, fallProtection, jumpAddition, fullyCustomModel);
            FormGroup group = GROUPS.get(groupId);
            if (group == null) {
                group = new FormGroup(groupId);
                GROUPS.put(groupId, group);
                DYNAMIC_GROUPS.add(groupId);
            }
            FormDefinition previous = FORMS.put(formId, definition);
            if (previous != null) {
                FormGroup previousGroup = GROUPS.get(previous.groupId());
                if (previousGroup != null) previousGroup.remove(formId);
            }
            group.add(definition);
            DYNAMIC_FORMS.add(formId);
            DYNAMIC_ORIGIN_IDS.put(formId, ids.originId());
            LOGGER.info("Loaded dynamic form {} from {}", formId, resourceId);
        });
        resolveJavaForms();
    }

    /**
     * Registers a Java form descriptor. Resolution is deferred until form data is first queried,
     * allowing an add-on to declare child forms before another add-on has registered its parent.
     */
    public static synchronized void registerJavaForm(SscForm form) {
        bootstrap();
        ResourceLocation id = form.id();
        if (JAVA_FORMS.containsKey(id) || (FORMS.containsKey(id) && !RESOLVED_JAVA_FORMS.contains(id))) {
            throw new IllegalStateException("Duplicate SSC Java form registration: '" + id + "'");
        }
        JAVA_FORMS.put(id, form);
        if (form.variantParentId() != null) {
            JAVA_VARIANT_PARENTS.put(id, form.variantParentId());
        }
        javaFormsDirty = true;
    }

    /**
     * Registers an explicit family evolution. Form declarations and this graph may be registered
     * in either order; validation is intentionally deferred until the complete Java form registry
     * is resolved.
     */
    public static synchronized void registerJavaEvolution(Evolution evolution) {
        bootstrap();
        JAVA_EVOLUTIONS.add(java.util.Objects.requireNonNull(evolution, "evolution"));
        javaFormsDirty = true;
    }

    private static synchronized void resolveJavaForms() {
        if (!javaFormsDirty) {
            return;
        }
        for (ResourceLocation id : RESOLVED_JAVA_FORMS) {
            FormDefinition old = FORMS.remove(id);
            if (old != null) {
                removeFromGroup(old);
            }
        }
        RESOLVED_JAVA_FORMS.clear();
        JAVA_GROUPS.removeIf(groupId -> {
            FormGroup group = GROUPS.get(groupId);
            if (group != null && group.isEmpty()) {
                GROUPS.remove(groupId);
                return true;
            }
            return group == null;
        });

        Set<ResourceLocation> resolving = new LinkedHashSet<>();
        for (ResourceLocation id : JAVA_FORMS.keySet()) {
            resolveJavaForm(id, resolving);
        }
        resolveJavaEvolutions();
        javaFormsDirty = false;
    }

    private static void resolveJavaEvolutions() {
        for (Evolution evolution : JAVA_EVOLUTIONS) {
            for (Evolution.Edge edge : evolution.edges()) {
                FormDefinition parent = FORMS.get(edge.from());
                FormDefinition child = FORMS.get(edge.to());
                if (parent == null || child == null) {
                    ResourceLocation missing = parent == null ? edge.from() : edge.to();
                    throw new IllegalStateException("SSC evolution references an unregistered form '" + missing + "'");
                }
                if (!child.groupId().equals(parent.groupId())) {
                    throw new IllegalStateException("SSC evolution edge '" + edge.from() + "' -> '" + edge.to()
                            + "' crosses groups ('" + parent.groupId() + "' to '" + child.groupId() + "')");
                }
                if (child.stage() != parent.stage() + 1) {
                    throw new IllegalStateException("SSC evolution edge '" + edge.from() + "' -> '" + edge.to()
                            + "' must advance exactly one stage (expected " + (parent.stage() + 1)
                            + ", got " + child.stage() + ")");
                }
            }
        }
        validateEvolutionCycles();
    }

    private static void validateEvolutionCycles() {
        Map<ResourceLocation, Set<ResourceLocation>> nextByForm = new LinkedHashMap<>();
        for (Evolution evolution : JAVA_EVOLUTIONS) {
            for (Evolution.Edge edge : evolution.edges()) {
                nextByForm.computeIfAbsent(edge.from(), ignored -> new LinkedHashSet<>()).add(edge.to());
            }
        }
        Set<ResourceLocation> visited = new LinkedHashSet<>();
        Set<ResourceLocation> visiting = new LinkedHashSet<>();
        for (ResourceLocation formId : nextByForm.keySet()) {
            validateEvolutionCycles(formId, nextByForm, visited, visiting);
        }
    }

    private static void validateEvolutionCycles(ResourceLocation formId,
                                                Map<ResourceLocation, Set<ResourceLocation>> nextByForm,
                                                Set<ResourceLocation> visited, Set<ResourceLocation> visiting) {
        if (visited.contains(formId)) return;
        if (!visiting.add(formId)) {
            throw new IllegalStateException("Circular SSC evolution involving '" + formId + "'");
        }
        for (ResourceLocation next : nextByForm.getOrDefault(formId, Set.of())) {
            validateEvolutionCycles(next, nextByForm, visited, visiting);
        }
        visiting.remove(formId);
        visited.add(formId);
    }

    private static FormDefinition resolveJavaForm(ResourceLocation id, Set<ResourceLocation> resolving) {
        FormDefinition resolved = FORMS.get(id);
        if (resolved != null) {
            return resolved;
        }
        SscForm form = JAVA_FORMS.get(id);
        if (form == null) {
            return null;
        }
        if (!resolving.add(id)) {
            throw new IllegalStateException("Circular SSC Java form inheritance involving '" + id + "'");
        }
        FormDefinition parent = null;
        if (form.inheritanceParentId() != null) {
            parent = JAVA_FORMS.containsKey(form.inheritanceParentId())
                    ? resolveJavaForm(form.inheritanceParentId(), resolving)
                    : FORMS.get(form.inheritanceParentId());
            if (parent == null) {
                throw new IllegalStateException("SSC Java form '" + id + "' inherits missing form '"
                        + form.inheritanceParentId() + "'");
            }
        }
        FormDefinition definition = form.resolve(parent);
        form.validateStage(definition);
        validateVariant(form, definition, parent);
        FormDefinition existing = FORMS.putIfAbsent(id, definition);
        if (existing != null) {
            throw new IllegalStateException("Duplicate SSC Java form registration: '" + id + "'");
        }
        FormGroup group = GROUPS.get(definition.groupId());
        if (group == null) {
            group = new FormGroup(definition.groupId());
            GROUPS.put(definition.groupId(), group);
            JAVA_GROUPS.add(definition.groupId());
        }
        group.add(definition);
        RESOLVED_JAVA_FORMS.add(id);
        resolving.remove(id);
        return definition;
    }

    private static void validateVariant(SscForm form, FormDefinition definition, FormDefinition parent) {
        ResourceLocation branchParentId = form.variantParentId();
        if (branchParentId == null) return;
        if (parent == null) {
            throw new IllegalStateException("SSC Java variant '" + form.id() + "' is missing branch parent '"
                    + branchParentId + "'");
        }
        if (!definition.groupId().equals(parent.groupId())) {
            throw new IllegalStateException("SSC Java variant '" + form.id() + "' must stay in group '"
                    + parent.groupId() + "', not '" + definition.groupId() + "'");
        }
        if (definition.stage() != parent.stage() + 1) {
            throw new IllegalStateException("SSC Java variant '" + form.id() + "' must be exactly one stage after '"
                    + branchParentId + "' (expected " + (parent.stage() + 1) + ", got " + definition.stage() + ")");
        }
        int branchMaximum = branchMaximumStage(branchParentId, parent);
        if (definition.stage() > branchMaximum) {
            throw new IllegalStateException("SSC Java variant '" + form.id() + "' is stage " + definition.stage()
                    + ", beyond branch maximum stage " + branchMaximum);
        }
    }

    private static int branchMaximumStage(ResourceLocation branchParentId, FormDefinition parent) {
        ResourceLocation root = branchParentId;
        Set<ResourceLocation> seen = new LinkedHashSet<>();
        while (JAVA_VARIANT_PARENTS.containsKey(root)) {
            if (!seen.add(root)) {
                throw new IllegalStateException("Circular SSC Java variant branch involving '" + root + "'");
            }
            root = JAVA_VARIANT_PARENTS.get(root);
        }
        SscForm rootForm = JAVA_FORMS.get(root);
        if (rootForm != null) {
            // validateStage already reports a malformed declaration; this value governs descendants.
            return Math.max(1, rootForm.maximumStageLimit());
        }
        FormGroup group = GROUPS.get(parent.groupId());
        return group == null ? parent.stage() : group.formsByStage().keySet().stream()
                .mapToInt(Integer::intValue).max().orElse(parent.stage());
    }

    private static void removeFromGroup(FormDefinition definition) {
        FormGroup group = GROUPS.get(definition.groupId());
        if (group == null) {
            return;
        }
        group.remove(definition.id());
        if (group.isEmpty() && JAVA_GROUPS.contains(definition.groupId())) {
            GROUPS.remove(definition.groupId());
        }
    }

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(FormRegistry.class);

    private static DynamicFormIds validateDynamicForm(ResourceLocation resourceId, JsonObject data,
                                                       ResourceManager resourceManager) {
        if (!data.has("FormID") || !data.get("FormID").isJsonPrimitive()) {
            throw new IllegalArgumentException("missing string FormID; expected '" + resourceId + "'");
        }
        ResourceLocation formId = ResourceLocation.tryParse(data.get("FormID").getAsString());
        if (formId == null) {
            throw new IllegalArgumentException("FormID is not a valid resource location");
        }
        if (!formId.equals(resourceId)) {
            throw new IllegalArgumentException("FormID must match its resource path; expected '"
                    + resourceId + "' but got '" + formId + "'");
        }
        if (formId.getPath().startsWith("form_")) {
            throw new IllegalArgumentException("FormID must name the form itself, without the 'form_' Origin prefix");
        }
        if (!data.has("originID") || !data.get("originID").isJsonPrimitive()) {
            throw new IllegalArgumentException("missing string originID; expected '"
                    + expectedOriginId(formId) + "'");
        }
        ResourceLocation originId = ResourceLocation.tryParse(data.get("originID").getAsString());
        ResourceLocation expectedOriginId = expectedOriginId(formId);
        if (!expectedOriginId.equals(originId)) {
            throw new IllegalArgumentException("originID must be '" + expectedOriginId
                    + "' for FormID '" + formId + "', but got '" + originId + "'");
        }
        if (resourceManager != null && resourceManager.getResource(originId).isEmpty()) {
            throw new IllegalArgumentException("missing Origin data file 'data/" + originId.getNamespace()
                    + "/origins/" + originId.getPath() + ".json'");
        }
        return new DynamicFormIds(formId, originId);
    }

    private static ResourceLocation expectedOriginId(ResourceLocation formId) {
        return ResourceLocation.fromNamespaceAndPath(formId.getNamespace(), "form_" + formId.getPath());
    }

    private record DynamicFormIds(ResourceLocation formId, ResourceLocation originId) {
    }

    private static ResourceLocation resourceLocation(JsonObject data, String key, ResourceLocation fallback) {
        if (!data.has(key) || !data.get(key).isJsonPrimitive()) return fallback;
        return ResourceLocation.tryParse(data.get(key).getAsString());
    }

    private static int integer(JsonObject data, String key, int fallback) {
        try {
            return data.has(key) ? data.get(key).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static float number(JsonObject data, String key, float fallback) {
        try {
            return data.has(key) ? data.get(key).getAsFloat() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean booleanValue(JsonObject data, String key, boolean fallback) {
        try {
            return data.has(key) ? data.get(key).getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static FormBodyType bodyType(JsonObject data) {
        String value = data.has("bodyType") ? data.get("bodyType").getAsString()
                : data.has("body_type") ? data.get("body_type").getAsString() : "NORMAL";
        try {
            return FormBodyType.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return FormBodyType.NORMAL;
        }
    }

    private static Set<String> flags(JsonObject data) {
        JsonElement element = data.has("flag") ? data.get("flag") : data.get("flags");
        if (element == null || !element.isJsonArray()) return Set.of();
        Set<String> result = new LinkedHashSet<>();
        for (JsonElement flag : element.getAsJsonArray()) {
            if (flag.isJsonPrimitive()) result.add(flag.getAsString());
        }
        return Set.copyOf(result);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, path);
    }

    public static FormDefinition get(ResourceLocation id) {
        bootstrap();
        resolveJavaForms();
        return FORMS.get(id);
    }

    public static FormDefinition get(String id) {
        return get(ResourceLocation.tryParse(id));
    }

    public static FormGroup getGroup(ResourceLocation id) {
        bootstrap();
        resolveJavaForms();
        return GROUPS.get(id);
    }

    /**
     * Returns the next form on the active branch. Evolution supplies candidates only; the registry
     * chooses the group's normal form when it is one of those candidates, otherwise the lowest
     * lexical form id. This selection policy intentionally lives outside {@link Evolution}.
     */
    public static FormDefinition nextInProgression(FormDefinition current) {
        if (current == null) return null;
        bootstrap();
        resolveJavaForms();
        int nextStage = current.stage() + 1;
        FormDefinition evolved = evolutionTarget(current, nextStage, true);
        if (evolved != null) return evolved;
        FormDefinition legacyVariant = childForParent(JAVA_VARIANT_PARENTS, current, nextStage);
        if (legacyVariant != null) return legacyVariant;
        FormGroup group = GROUPS.get(current.groupId());
        return group == null ? null : group.firstAtStage(nextStage);
    }

    private static FormDefinition childForParent(Map<ResourceLocation, ResourceLocation> parents,
                                                  FormDefinition current, int expectedStage) {
        for (Map.Entry<ResourceLocation, ResourceLocation> entry : parents.entrySet()) {
            if (!current.id().equals(entry.getValue())) continue;
            FormDefinition child = FORMS.get(entry.getKey());
            if (child != null && child.stage() == expectedStage && child.groupId().equals(current.groupId())) {
                return child;
            }
        }
        return null;
    }

    private static FormDefinition evolutionTarget(FormDefinition current, int expectedStage, boolean next) {
        Set<ResourceLocation> candidateIds = new LinkedHashSet<>();
        for (Evolution evolution : JAVA_EVOLUTIONS) {
            candidateIds.addAll(next ? evolution.next(current.id()) : evolution.previous(current.id()));
        }
        if (candidateIds.isEmpty()) return null;

        FormGroup group = GROUPS.get(current.groupId());
        FormDefinition normal = group == null ? null : group.firstAtStage(expectedStage);
        if (normal != null && candidateIds.contains(normal.id())) return normal;

        return candidateIds.stream()
                .sorted(java.util.Comparator.comparing(ResourceLocation::toString))
                .map(FORMS::get)
                .filter(candidate -> candidate != null && candidate.stage() == expectedStage
                        && candidate.groupId().equals(current.groupId()))
                .findFirst()
                .orElse(null);
    }

    /** Returns the preceding form in an evolution branch, otherwise the group's ordinary prior stage. */
    public static FormDefinition previousInProgression(FormDefinition current) {
        if (current == null) return null;
        bootstrap();
        resolveJavaForms();
        FormDefinition evolved = evolutionTarget(current, current.stage() - 1, false);
        if (evolved != null) return evolved;
        ResourceLocation branchParent = JAVA_VARIANT_PARENTS.get(current.id());
        if (branchParent != null) return FORMS.get(branchParent);
        FormGroup group = GROUPS.get(current.groupId());
        return group == null ? null : group.firstAtStage(current.stage() - 1);
    }

    /** Walks the current branch to a requested stage instead of jumping to an unrelated sibling. */
    public static FormDefinition formAtStageInProgression(FormDefinition current, int targetStage) {
        if (current == null || targetStage < 1) return null;
        FormDefinition cursor = current;
        while (cursor != null && cursor.stage() != targetStage) {
            cursor = cursor.stage() < targetStage ? nextInProgression(cursor) : previousInProgression(cursor);
        }
        return cursor;
    }

    public static ResourceLocation originIdFor(ResourceLocation formId) {
        bootstrap();
        return DYNAMIC_ORIGIN_IDS.get(formId);
    }

    /**
     * Current form followed by its Java-form ancestors. Built-in and data-defined forms have a
     * one-entry lineage. The order deliberately lets child-defined powers run before inherited
     * powers when both are active.
     */
    public static List<ResourceLocation> lineage(ResourceLocation formId) {
        bootstrap();
        resolveJavaForms();
        List<ResourceLocation> result = new ArrayList<>();
        Set<ResourceLocation> seen = new LinkedHashSet<>();
        ResourceLocation current = formId;
        while (current != null) {
            if (!seen.add(current)) {
                throw new IllegalStateException("Circular SSC Java form inheritance involving '" + current + "'");
            }
            result.add(current);
            SscForm form = JAVA_FORMS.get(current);
            current = form == null ? null : form.inheritanceParentId();
        }
        return List.copyOf(result);
    }

    /** Whether an id was successfully loaded from an external {@code ssc_form} data directory. */
    public static boolean isDynamicForm(ResourceLocation formId) {
        bootstrap();
        return DYNAMIC_FORMS.contains(formId);
    }

    public static Map<ResourceLocation, FormDefinition> forms() {
        bootstrap();
        resolveJavaForms();
        return Collections.unmodifiableMap(FORMS);
    }

    public static Map<ResourceLocation, FormGroup> groups() {
        bootstrap();
        resolveJavaForms();
        return Collections.unmodifiableMap(GROUPS);
    }
}
