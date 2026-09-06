package net.onixary.shapeShifterCurseForge.api.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Small DeferredRegister-style staging area for one SSC add-on namespace.
 *
 * <p>Declare handles as static fields, then call {@link #init()} exactly once from the owning
 * mod's entry point. Suppliers are evaluated only at init, so forms become stable registry
 * definitions instead of incidental temporary objects constructed while class-loading.</p>
 */
public final class SscRegistrar {
    private final String namespace;
    private final List<Runnable> deferredEntries = new ArrayList<>();
    private State state = State.MUTABLE;

    SscRegistrar(String namespace) {
        if (namespace == null || namespace.isBlank()) throw new IllegalArgumentException("SSC registry namespace must not be blank");
        this.namespace = namespace;
    }

    /** Defers construction and registration of a stable form definition. */
    public synchronized <T extends SscForm> SscRegistryObject<T> form(Supplier<T> factory) {
        requireMutable();
        Objects.requireNonNull(factory, "factory");
        SscRegistryObject<T> handle = new SscRegistryObject<>();
        deferredEntries.add(() -> {
            T form = Objects.requireNonNull(factory.get(), "form factory result");
            if (!namespace.equals(form.id().getNamespace())) {
                throw new IllegalArgumentException("SSC registrar for namespace '" + namespace
                        + "' cannot register form '" + form.id() + "' from namespace '"
                        + form.id().getNamespace() + "'");
            }
            SscJavaRegistries.registerForm(form);
            handle.complete(form.id(), form);
        });
        return handle;
    }

    /** Defers registration of a family-owned evolution graph. */
    public synchronized void evolution(Supplier<Evolution> factory) {
        requireMutable();
        Objects.requireNonNull(factory, "factory");
        deferredEntries.add(() -> SscJavaRegistries.registerEvolution(
                Objects.requireNonNull(factory.get(), "evolution factory result")));
    }

    /** Defers registration of a Java Power under this registrar's namespace. */
    public synchronized SscRegistryObject<SscPower> power(String path, Supplier<SscPower> factory) {
        return keyed(path, factory, SscJavaRegistries::registerPower);
    }

    /** Defers registration of a Java condition under this registrar's namespace. */
    public synchronized SscRegistryObject<SscCondition> condition(String path, Supplier<SscCondition> factory) {
        return keyed(path, factory, SscJavaRegistries::registerCondition);
    }

    /** Defers registration of a Java action under this registrar's namespace. */
    public synchronized SscRegistryObject<SscAction> action(String path, Supplier<SscAction> factory) {
        return keyed(path, factory, SscJavaRegistries::registerAction);
    }

    /**
     * Performs all declarations in the order they were made.
     *
     * <p>Repeated calls after success are no-ops. SSC registries do not support rollback, so an
     * exception leaves this registrar in a terminal failed state: it must not be retried, because
     * earlier entries may already have reached their global registries.</p>
     */
    public synchronized void init() {
        if (state == State.INITIALIZED) return;
        if (state == State.INITIALIZING) {
            throw new IllegalStateException("SSC registrar initialization is already in progress");
        }
        if (state == State.FAILED) {
            throw new IllegalStateException("SSC registrar initialization previously failed and cannot be retried");
        }

        state = State.INITIALIZING;
        try {
            deferredEntries.forEach(Runnable::run);
            state = State.INITIALIZED;
        } catch (RuntimeException | Error exception) {
            state = State.FAILED;
            throw exception;
        }
    }

    private <T> SscRegistryObject<T> keyed(String path, Supplier<T> factory, EntryRegistrar<T> registrar) {
        requireMutable();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, validatePath(path));
        Objects.requireNonNull(factory, "factory");
        SscRegistryObject<T> handle = new SscRegistryObject<>();
        deferredEntries.add(() -> {
            T value = Objects.requireNonNull(factory.get(), "registry factory result");
            registrar.register(id, value);
            handle.complete(id, value);
        });
        return handle;
    }

    private void requireMutable() {
        if (state != State.MUTABLE) {
            throw new IllegalStateException("Cannot add SSC registrations after initialization has started (state: "
                    + state + ")");
        }
    }

    private static String validatePath(String path) {
        if (path == null || path.isBlank() || ResourceLocation.tryParse("ssc:" + path) == null) {
            throw new IllegalArgumentException("Invalid SSC registry path: '" + path + "'");
        }
        return path;
    }

    @FunctionalInterface
    private interface EntryRegistrar<T> {
        void register(ResourceLocation id, T value);
    }

    private enum State {
        MUTABLE,
        INITIALIZING,
        INITIALIZED,
        FAILED
    }
}
