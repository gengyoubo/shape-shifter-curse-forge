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
    private static final Map<UUID, Map<ResourceLocation, Double>> RESOURCES = new HashMap<>();
    private static final Map<UUID, Map<String, Float>> MANA = new HashMap<>();
    private static final Map<UUID, Boolean> SPRINTING = new HashMap<>();
    private static final Map<UUID, Boolean> CROUCHING = new HashMap<>();
    private static final Map<UUID, Integer> JUMPS = new HashMap<>();
    private static final Map<UUID, Integer> GROUND_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> LEVITATE_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> JUMP_INPUT_GRACE = new HashMap<>();

    private FormActivePowerService() {
    }

    public static void setKeyPressed(ServerPlayer player, String key, boolean pressed) {
        if (!key.startsWith("key.shape-shifter-curse.") && !"key.jump".equals(key)) {
            return;
        }
        Map<String, Boolean> keys = PRESSED_KEYS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        boolean wasPressed = keys.getOrDefault(key, false);
        keys.put(key, pressed);
        if (pressed && !wasPressed) {
            if ("key.jump".equals(key) && !player.onGround()) {
                triggerAirJump(player);
            } else {
                triggerActive(player, key);
            }
        } else if (!pressed && wasPressed) {
            releaseCharge(player, key);
        }
    }

    public static void tick(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        tickCooldowns(player.getUUID());
        JUMP_INPUT_GRACE.computeIfPresent(player.getUUID(), (id, ticks) -> ticks <= 1 ? null : ticks - 1);
        if (player.onGround()) {
            GROUND_TICKS.merge(player.getUUID(), 1, Integer::sum);
            if (GROUND_TICKS.get(player.getUUID()) >= 8) JUMPS.remove(player.getUUID());
            LEVITATE_TICKS.remove(player.getUUID());
        } else {
            GROUND_TICKS.put(player.getUUID(), 0);
        }
        boolean wasSprinting = SPRINTING.getOrDefault(player.getUUID(), false);
        SPRINTING.put(player.getUUID(), player.isSprinting());
        boolean wasCrouching = CROUCHING.getOrDefault(player.getUUID(), false);
        CROUCHING.put(player.getUUID(), player.isCrouching());
        if (player.isSprinting() && !wasSprinting && player instanceof ServerPlayer serverPlayer) {
            triggerActive(serverPlayer, "key.sprint");
        }
        if (wasSprinting && player.isCrouching() && !wasCrouching && player instanceof ServerPlayer serverPlayer) {
            FormPowerRegistry.visitActive(serverPlayer, (id, power) -> {
                if ("shape-shifter-curse:action_on_sprinting_to_sneaking".equals(FormPowerRegistry.typeOf(power))
                        && FormPowerRuntime.test(serverPlayer, serverPlayer, power.getAsJsonObject("entity_condition"))) {
                    FormPowerRuntime.execute(serverPlayer, serverPlayer, power.getAsJsonObject("entity_action"));
                }
            });
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
        if (keys.getOrDefault("key.jump", false)) {
            tickLevitation(serverPlayer);
        }
    }

    public static boolean hasMana(Player player, float amount) {
        return mana(player) >= amount;
    }

    public static void triggerVanillaKey(Player player, String key) {
        if (player instanceof ServerPlayer serverPlayer) {
            triggerActive(serverPlayer, key);
        }
    }

    public static void registerGroundJump(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        JUMPS.put(player.getUUID(), 1);
        JUMP_INPUT_GRACE.put(player.getUUID(), 3);
        applyTripleJump(serverPlayer, 1, true);
    }

    public static boolean consumeMana(Player player, float amount) {
        if (!hasMana(player, amount)) {
            return false;
        }
        setMana(player, mana(player) - amount);
        return true;
    }

    /** Adds mana to the currently selected mana pool, clamped to that pool's maximum. */
    public static void gainMana(Player player, float amount) {
        setMana(player, mana(player) + amount);
    }

    /** Returns the current Apoli resource value. Cooldowns are exposed as 0/1, as in the source data. */
    public static double resource(Player player, ResourceLocation id) {
        FormPowerDefinition definition = FormPowerRegistry.all().get(id);
        if (definition != null && "apoli:cooldown".equals(FormPowerRegistry.typeOf(definition.data()))) {
            return isOnCooldown(player, id) ? 1.0D : 0.0D;
        }
        return RESOURCES.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                .computeIfAbsent(id, ignored -> defaultResource(definition));
    }

    public static void modifyResource(Player player, ResourceLocation id, JsonObject modifier) {
        FormPowerDefinition definition = FormPowerRegistry.all().get(id);
        if (definition != null && "apoli:cooldown".equals(FormPowerRegistry.typeOf(definition.data()))) {
            if (FormPowerRuntime.doubleValue(modifier, "value", 0.0D) >= 1.0D) {
                triggerCooldown(player, id);
            }
            return;
        }
        double current = resource(player, id);
        double value = switch (FormPowerRuntime.stringValue(modifier, "operation", "addition")) {
            case "set_total" -> FormPowerRuntime.doubleValue(modifier, "value", current);
            default -> FormPowerRuntime.applyModifier(current, modifier);
        };
        double min = definition == null ? Double.NEGATIVE_INFINITY : FormPowerRuntime.doubleValue(definition.data(), "min", 0.0D);
        double max = definition == null ? Double.POSITIVE_INFINITY : FormPowerRuntime.doubleValue(definition.data(), "max", Double.MAX_VALUE);
        RESOURCES.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).put(id, Math.max(min, Math.min(max, value)));
    }

    public static void triggerCooldown(Player player, ResourceLocation id) {
        FormPowerDefinition definition = FormPowerRegistry.all().get(id);
        if (definition != null) {
            startCooldown(player, id, FormPowerRuntime.intValue(definition.data(), "cooldown", 0));
        }
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

    private static void triggerAirJump(ServerPlayer player) {
        if (JUMP_INPUT_GRACE.containsKey(player.getUUID())) return;
        int jump = JUMPS.getOrDefault(player.getUUID(), 1) + 1;
        if (jump > 3) return;
        JUMPS.put(player.getUUID(), jump);
        applyTripleJump(player, jump, false);
    }

    private static void applyTripleJump(ServerPlayer player, int jump, boolean fromGround) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:triple_jump".equals(FormPowerRegistry.typeOf(power))) return;
            String ordinal = jump == 1 ? "first" : jump == 2 ? "second" : "third";
            float multiplier = FormPowerRuntime.floatValue(power, ordinal + "_jump_multiplier", 1.0F);
            double y = fromGround ? player.getDeltaMovement().y * multiplier : 0.42D * multiplier;
            player.setDeltaMovement(player.getDeltaMovement().x, y, player.getDeltaMovement().z);
            FormPowerRuntime.execute(player, player, power.getAsJsonObject(ordinal + "_jump_action"));
        });
    }

    private static void tickLevitation(ServerPlayer player) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:levitate".equals(FormPowerRegistry.typeOf(power)) || player.onGround()) return;
            int ticks = LEVITATE_TICKS.merge(player.getUUID(), 1, Integer::sum);
            if (ticks <= FormPowerRuntime.intValue(power, "max_ascend_duration", 0)) {
                player.setDeltaMovement(player.getDeltaMovement().x,
                        Math.max(player.getDeltaMovement().y, FormPowerRuntime.doubleValue(power, "ascent_speed", 0.3D)),
                        player.getDeltaMovement().z);
                player.fallDistance = 0.0F;
            }
        });
        if (player.onGround()) LEVITATE_TICKS.remove(player.getUUID());
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

    private static double defaultResource(FormPowerDefinition definition) {
        return definition == null ? 0.0D : FormPowerRuntime.doubleValue(definition.data(), "start_value",
                FormPowerRuntime.doubleValue(definition.data(), "min", 0.0D));
    }

    private static void tickCooldowns(UUID playerId) {
        Map<ResourceLocation, Integer> cooldowns = COOLDOWNS.get(playerId);
        if (cooldowns != null) {
            cooldowns.replaceAll((id, ticks) -> ticks - 1);
            cooldowns.entrySet().removeIf(entry -> entry.getValue() <= 0);
        }
    }
}
