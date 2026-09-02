package net.onixary.shapeShifterCurseForge.form;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FormGroup {
    private final ResourceLocation id;
    private final Map<Integer, List<FormDefinition>> formsByTier = new LinkedHashMap<>();

    public FormGroup(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation id() {
        return id;
    }

    public void add(FormDefinition form) {
        formsByTier.computeIfAbsent(form.tier(), ignored -> new ArrayList<>()).add(form);
    }

    public List<FormDefinition> formsAtTier(int tier) {
        return Collections.unmodifiableList(formsByTier.getOrDefault(tier, List.of()));
    }

    public FormDefinition firstAtTier(int tier) {
        return formsByTier.getOrDefault(tier, List.of()).stream().findFirst().orElse(null);
    }

    public Map<Integer, List<FormDefinition>> formsByTier() {
        return Collections.unmodifiableMap(formsByTier);
    }
}
