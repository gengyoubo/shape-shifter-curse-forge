package net.onixary.shapeShifterCurseForge.form;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FormGroup {
    private final ResourceLocation id;
    private final Map<Integer, List<FormDefinition>> formsByStage = new LinkedHashMap<>();

    public FormGroup(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation id() {
        return id;
    }

    public void add(FormDefinition form) {
        formsByStage.computeIfAbsent(form.stage(), ignored -> new ArrayList<>()).add(form);
    }

    public boolean remove(ResourceLocation formId) {
        boolean removed = false;
        for (List<FormDefinition> forms : formsByStage.values()) {
            removed |= forms.removeIf(form -> form.id().equals(formId));
        }
        formsByStage.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return removed;
    }

    public boolean isEmpty() {
        return formsByStage.isEmpty();
    }

    /** Returns the forms registered at one progression stage. */
    public List<FormDefinition> formsAtStage(int stage) {
        return Collections.unmodifiableList(formsByStage.getOrDefault(stage, List.of()));
    }

    /** Returns the default form at one progression stage. */
    public FormDefinition firstAtStage(int stage) {
        return formsByStage.getOrDefault(stage, List.of()).stream().findFirst().orElse(null);
    }

    /** @deprecated Use {@link #formsAtStage(int)}. */
    @Deprecated(forRemoval = false)
    public List<FormDefinition> formsAtTier(int tier) {
        return formsAtStage(tier);
    }

    /** @deprecated Use {@link #firstAtStage(int)}. */
    @Deprecated(forRemoval = false)
    public FormDefinition firstAtTier(int tier) {
        return firstAtStage(tier);
    }

    /** Immutable stage-to-forms view used by progression code. */
    public Map<Integer, List<FormDefinition>> formsByStage() {
        return Collections.unmodifiableMap(formsByStage);
    }

    /** @deprecated Use {@link #formsByStage()}. */
    @Deprecated(forRemoval = false)
    public Map<Integer, List<FormDefinition>> formsByTier() {
        return formsByStage();
    }
}
