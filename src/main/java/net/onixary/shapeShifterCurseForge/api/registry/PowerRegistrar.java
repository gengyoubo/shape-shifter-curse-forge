package net.onixary.shapeShifterCurseForge.api.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Receives immutable power references declared by a class-based form.
 *
 * <p>This registrar only describes what a form has. It does not create a per-player Power
 * instance or hold cooldown/state; Java powers must store such state in an appropriate
 * per-player capability/attachment.</p>
 */
public final class PowerRegistrar {
    private final Set<ResourceLocation> powerIds = new LinkedHashSet<>();

    /**
     * Adds either an SSC Java power or a data-defined power id. Data-defined ids are resolved by
     * the normal power reload pipeline, so an add-on need not duplicate JSON powers in Java.
     */
    public void add(ResourceLocation powerId) {
        powerIds.add(Objects.requireNonNull(powerId, "powerId"));
    }

    public void add(String powerId) {
        ResourceLocation parsed = ResourceLocation.tryParse(powerId);
        if (parsed == null) throw new IllegalArgumentException("Invalid SSC power id: '" + powerId + "'");
        add(parsed);
    }

    Set<ResourceLocation> ids() {
        return Set.copyOf(powerIds);
    }
}
