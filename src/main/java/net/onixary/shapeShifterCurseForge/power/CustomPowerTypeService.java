package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;
import net.onixary.shapeShifterCurseForge.api.registry.SscPowerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Drives add-on {@link SscPowerType} implementations for the data-defined powers on each player's
 * current form. Server state is touched only on the server thread.
 *
 * <p>An instance is one power JSON object of a registered type. Children of {@code apoli:multiple}
 * are separate instances that share the parent's power id. Instances are compared by power id and
 * JSON object identity, so a data reload replaces them (remove, then add) as Apoli did.</p>
 */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class CustomPowerTypeService {
    private record Instance(ResourceLocation powerId, SscPowerType type, JsonObject data) {
        boolean sameAs(Instance other) {
            return powerId.equals(other.powerId) && data == other.data;
        }
    }

    @FunctionalInterface
    private interface Callback {
        void run(SscPowerType type, ResourceLocation powerId, JsonObject data);
    }

    private static final Map<UUID, List<Instance>> ACTIVE = new HashMap<>();

    private CustomPowerTypeService() {
    }

    private static List<Instance> scan(Player player) {
        if (!SscJavaRegistries.hasPowerTypes()) return List.of();
        List<Instance> found = new ArrayList<>();
        FormPowerRegistry.visitActive(player, (id, power) -> {
            ResourceLocation type = ResourceLocation.tryParse(FormPowerRegistry.typeOf(power));
            if (type != null) SscJavaRegistries.powerType(type).ifPresent(t -> found.add(new Instance(id, t, power)));
        });
        return found;
    }

    /** Server player tick: reconcile added/removed powers, then tick every active instance. */
    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        List<Instance> now = scan(player);
        List<Instance> before = ACTIVE.getOrDefault(id, List.of());
        if (now.isEmpty() && before.isEmpty()) return;
        for (Instance old : before) {
            if (now.stream().noneMatch(old::sameAs)) call(old, (t, p, d) -> t.onRemoved(player, p, d));
        }
        for (Instance current : now) {
            if (before.stream().noneMatch(current::sameAs)) call(current, (t, p, d) -> t.onAdded(player, p, d));
        }
        if (now.isEmpty()) ACTIVE.remove(id); else ACTIVE.put(id, now);
        for (Instance current : now) call(current, (t, p, d) -> t.tick(player, p, d));
    }

    /** Logical-client player tick. */
    public static void clientTick(Player player) {
        for (Instance current : scan(player)) call(current, (t, p, d) -> t.clientTick(player, p, d));
    }

    public static void keyPressed(ServerPlayer player, String key) {
        forKey(player, key, (t, p, d) -> t.onKeyPressed(player, p, d));
    }

    public static void keyHeld(ServerPlayer player, String key) {
        forKey(player, key, (t, p, d) -> t.whileKeyHeld(player, p, d));
    }

    public static void keyReleased(ServerPlayer player, String key) {
        forKey(player, key, (t, p, d) -> t.onKeyReleased(player, p, d));
    }

    /** Logout: every active instance is removed. */
    public static void forget(ServerPlayer player) {
        List<Instance> before = ACTIVE.remove(player.getUUID());
        if (before != null) for (Instance old : before) call(old, (t, p, d) -> t.onRemoved(player, p, d));
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }

    private static void forKey(ServerPlayer player, String key, Callback callback) {
        List<Instance> active = ACTIVE.get(player.getUUID());
        if (active == null) return;
        for (Instance instance : List.copyOf(active)) {
            JsonObject data = instance.data();
            if (data.has("key") && data.get("key").isJsonObject()
                    && key.equals(FormPowerRuntime.stringValue(data.getAsJsonObject("key"), "key", ""))) {
                call(instance, callback);
            }
        }
    }

    /** Add-on code must not be able to stop SSC's own tick; failures are logged and skipped. */
    private static void call(Instance instance, Callback callback) {
        try {
            callback.run(instance.type(), instance.powerId(), instance.data());
        } catch (RuntimeException exception) {
            ShapeShifterCurseForge.LOGGER.error("SSC power type callback failed for power {}", instance.powerId(), exception);
        }
    }
}
