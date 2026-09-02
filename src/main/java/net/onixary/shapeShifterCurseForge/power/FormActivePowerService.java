package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative active, toggle, cooldown, charge and mana state for form powers. */
public final class FormActivePowerService {
    private static final float DEFAULT_MANA = 20.0F;
    private static final Map<UUID, Map<String, Boolean>> PRESSED_KEYS = new HashMap<>();
    private static final Map<UUID, Map<ResourceLocation, Integer>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Map<ResourceLocation, Integer>> CHARGES = new HashMap<>();
    private static final Map<UUID, Map<String, Float>> MANA = new HashMap<>();
    private static final Map<UUID, Boolean> SPRINTING = new HashMap<>();

    private FormActivePowerService() {
    }

    public static void setKeyPressed(ServerPlayer player, String key, boolean pressed) {
        if (!key.startsWith("key.shape-shifter-curse.")) {
            return;
        }
        Map<String, Boolean> keys = PRESSED_KEYS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        boolean wasPressed = keys.getOrDefault(key, false);
        keys.put(key, pressed);
        if (pressed && !wasPressed) {
            triggerActive(player, key);
        } else if (!pressed && wasPressed) {
            releaseCharge(player, key);
        }
    }

    public static void tick(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        tickCooldowns(player.getUUID());
        boolean wasSprinting = SPRINTING.getOrDefault(player.getUUID(), false);
        SPRINTING.put(player.getUUID(), player.isSprinting());
        if (player.isSprinting() && !wasSprinting && player instanceof ServerPlayer serverPlayer) {
            triggerActive(serverPlayer, "key.sprint");
        }
        Map<String, Boolean> keys = PRESSED_KEYS.get(player.getUUID());
        if (keys == null) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        keys.forEach((key, pressed) -> {
            if (pressed) {
                charge(serverPlayer, key);
            }
        });
    }

    public static boolean hasMana(Player player, float amount) {
        return mana(player) >= amount;
    }

    public static void triggerVanillaKey(Player player, String key) {
        if (player instanceof ServerPlayer serverPlayer) {
            triggerActive(serverPlayer, key);
        }
    }

    public static boolean consumeMana(Player player, float amount) {
        if (!hasMana(player, amount)) {
            return false;
        }
        setMana(player, mana(player) - amount);
        return true;
    }

    public static float mana(Player player) {
        String type = manaType(player);
        return MANA.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).computeIfAbsent(type,
                ignored -> DEFAULT_MANA);
    }

    private static void setMana(Player player, float value) {
        MANA.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).put(manaType(player),
                Math.max(0.0F, Math.min(DEFAULT_MANA, value)));
    }

    private static String manaType(Player player) {
        final String[] type = {"shape-shifter-curse:generic_mana"};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:mana_type_power".equals(FormPowerRegistry.typeOf(power)) && power.has("mana_type")) {
                type[0] = power.get("mana_type").getAsString();
            }
        });
        return type[0];
    }

    private static void triggerActive(ServerPlayer player, String key) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:active_self".equals(FormPowerRegistry.typeOf(power)) || !usesKey(power, key)
                    || isOnCooldown(player, id)) {
                return;
            }
            JsonObject condition = power.has("condition") ? power.getAsJsonObject("condition") : power.getAsJsonObject("entity_condition");
            if (!FormPowerRuntime.test(player, player, condition)) {
                return;
            }
            FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
            startCooldown(player, id, FormPowerRuntime.intValue(power, "cooldown", 0));
        });
    }

    private static void charge(ServerPlayer player, String key) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:charge_action".equals(FormPowerRegistry.typeOf(power)) || !usesKey(power, key)
                    || isOnCooldown(player, id)) {
                return;
            }
            int tier = jsonTier(player);
            String prefix = "tier" + tier + "_";
            if (!power.has(prefix + "enable") || !power.get(prefix + "enable").getAsBoolean()) {
                return;
            }
            if (power.has(prefix + "can_charge_condition")
                    && !FormPowerRuntime.test(player, player, power.getAsJsonObject(prefix + "can_charge_condition"))) {
                return;
            }

            int charge = CHARGES.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                    .merge(id, 1, Integer::sum);
            FormPowerRuntime.execute(player, player, power.getAsJsonObject(prefix + "tick_action"));
            FormPowerRuntime.execute(player, player, power.getAsJsonObject(prefix + "charge_tick_action"));
            if (charge == Math.max(1, FormPowerRuntime.intValue(power, prefix + "charge_time", 0))) {
                FormPowerRuntime.execute(player, player, power.getAsJsonObject(prefix + "charge_complete_action"));
            }
        });
    }

    private static void releaseCharge(ServerPlayer player, String key) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:charge_action".equals(FormPowerRegistry.typeOf(power)) || !usesKey(power, key)) {
                return;
            }
            int tier = jsonTier(player);
            String prefix = "tier" + tier + "_";
            int charge = CHARGES.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).remove(id);
            int required = Math.max(0, FormPowerRuntime.intValue(power, prefix + "charge_time", 0));
            if (charge >= required && !isOnCooldown(player, id)) {
                FormPowerRuntime.execute(player, player, power.getAsJsonObject(prefix + "use_action"));
                startCooldown(player, id, FormPowerRuntime.intValue(power, prefix + "cooldown", 0));
            }
        });
    }

    private static int jsonTier(Player player) {
        String path = player.getCapability(net.onixary.shapeShifterCurseForge.capability.ModCapabilities.PLAYER_FORM)
                .map(data -> data.getFormId()).orElse("");
        int underscore = path.lastIndexOf('_');
        if (underscore >= 0) {
            try {
                return Integer.parseInt(path.substring(underscore + 1));
            } catch (NumberFormatException ignored) {
                // Special forms do not use tiered charge JSON.
            }
        }
        return 0;
    }

    private static boolean usesKey(JsonObject power, String key) {
        return power.has("key") && power.get("key").isJsonObject()
                && key.equals(FormPowerRuntime.stringValue(power.getAsJsonObject("key"), "key", ""));
    }

    private static boolean isOnCooldown(Player player, ResourceLocation id) {
        return COOLDOWNS.getOrDefault(player.getUUID(), Map.of()).getOrDefault(id, 0) > 0;
    }

    private static void startCooldown(Player player, ResourceLocation id, int ticks) {
        if (ticks > 0) {
            COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).put(id, ticks);
        }
    }

    private static void tickCooldowns(UUID playerId) {
        Map<ResourceLocation, Integer> cooldowns = COOLDOWNS.get(playerId);
        if (cooldowns != null) {
            cooldowns.replaceAll((id, ticks) -> ticks - 1);
            cooldowns.entrySet().removeIf(entry -> entry.getValue() <= 0);
        }
    }
}
