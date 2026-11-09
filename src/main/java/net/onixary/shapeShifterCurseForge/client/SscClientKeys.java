package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.client.KeyMapping;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client half of add-on power keys. SSC polls every registered {@link KeyMapping} each client tick
 * and sends its name and pressed state to the server, exactly as it does for its own active-skill
 * keys. The name must also be registered on both sides with
 * {@link net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries#registerPowerKey}.
 *
 * <p>The mapping only needs {@link KeyMapping#getName()} and {@link KeyMapping#isDown()}. It need not
 * be registered with {@code RegisterKeyMappingsEvent}: a "virtual" mapping whose {@code isDown()}
 * forwards to another key (for example SSC's own active-skill key) is allowed.</p>
 */
public final class SscClientKeys {
    private static final List<KeyMapping> KEYS = new CopyOnWriteArrayList<>();

    private SscClientKeys() {
    }

    /** Call from client setup. */
    public static void register(KeyMapping mapping) {
        Objects.requireNonNull(mapping, "key mapping");
        if (KEYS.stream().anyMatch(existing -> existing.getName().equals(mapping.getName()))) {
            throw new IllegalStateException("Duplicate SSC client power key: '" + mapping.getName() + "'");
        }
        KEYS.add(mapping);
    }

    static List<KeyMapping> keys() {
        return KEYS;
    }
}
