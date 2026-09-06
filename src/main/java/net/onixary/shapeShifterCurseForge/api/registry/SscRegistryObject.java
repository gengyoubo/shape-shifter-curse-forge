package net.onixary.shapeShifterCurseForge.api.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Deferred, stable handle to one SSC registry object.
 *
 * <p>Like Forge's {@code RegistryObject}, this handle is safe to keep in a static field but its
 * value cannot be read until the owning {@link SscRegistrar} has been {@link SscRegistrar#init()
 * initialized}. The registry keeps that one object as the definition; it is never a player
 * runtime instance.</p>
 */
public final class SscRegistryObject<T> implements Supplier<T> {
    private volatile ResourceLocation id;
    private volatile T value;

    SscRegistryObject() {
    }

    void complete(ResourceLocation id, T value) {
        if (this.value != null) throw new IllegalStateException("SSC registry object was initialized twice");
        this.id = Objects.requireNonNull(id, "id");
        this.value = Objects.requireNonNull(value, "value");
    }

    public ResourceLocation id() {
        ensureInitialized();
        return id;
    }

    public boolean isPresent() {
        return value != null;
    }

    @Override
    public T get() {
        ensureInitialized();
        return value;
    }

    private void ensureInitialized() {
        if (value == null) throw new IllegalStateException("SSC registry object has not been initialized yet");
    }
}
