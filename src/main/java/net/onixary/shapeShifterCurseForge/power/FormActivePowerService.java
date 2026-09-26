package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.other.config.SscCommonConfig;
import net.onixary.shapeShifterCurseForge.form.FormManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative active, toggle, cooldown, charge and mana state for form powers. */
@SuppressWarnings("deprecation")
public final class FormActivePowerService {
    private static final float DEFAULT_MANA = 20.0F;
    private static final String FAMILIAR_FOX_MANA = "shape-shifter-curse:familiar_fox_mana";
    private static final String WEB_RESOURCE = "shape-shifter-curse:web_resource";
    private static final float FAMILIAR_FOX_MAX_MANA = 100.0F;
    private static final Map<UUID, Map<String, Boolean>> PRESSED_KEYS = new HashMap<>();
    private static final Map<UUID, Map<ResourceLocation, Integer>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Map<ResourceLocation, ChargeState>> CHARGES = new HashMap<>();
    private static final class ChargeState {
        int ticks;
        int tier;
        int lastUseTick = Integer.MIN_VALUE;
    }
    private static final Map<UUID, Map<ResourceLocation, Double>> RESOURCES = new HashMap<>();
    private static final Map<UUID, Map<ResourceLocation, Boolean>> TOGGLES = new HashMap<>();
    private static final Map<UUID, Boolean> SPRINTING = new HashMap<>();
    private static final Map<UUID, Boolean> SPRINT_TO_SNEAK_TRIGGERED = new HashMap<>();
    private static final Map<UUID, Integer> LEVITATE_TICKS = new HashMap<>();
    /** One travel tick of protection for a launch issued while still touching water. */
    private static final Map<UUID, Boolean> WATER_LAUNCH_GRACE = new HashMap<>();
    private static final ResourceLocation JUMP_OUT_WATER = ResourceLocation.fromNamespaceAndPath(
            "shape-shifter-curse", "jump_out_water");

    private FormActivePowerService() {
    }

    /** Drops transient input state when a player leaves, so held/charged skills cannot survive a reconnect. */
    public static void clearTransientInput(Player player) {
        UUID id = player.getUUID();
        PRESSED_KEYS.remove(id);
        CHARGES.remove(id);
        WATER_LAUNCH_GRACE.remove(id);
    }

    /** An Origin replacement removes its power instances; discard the matching runtime state. */
    public static void onFormChanged(Player player) {
        UUID id = player.getUUID();
        clearTransientInput(player);
        COOLDOWNS.remove(id);
        RESOURCES.remove(id);
        TOGGLES.remove(id);
        SPRINTING.remove(id);
        SPRINT_TO_SNEAK_TRIGGERED.remove(id);
        LEVITATE_TICKS.remove(id);
        // Fabric's ManaTypePower fills the active pool when the form gains it.
        if (WEB_RESOURCE.equals(manaType(player))) setMana(player, maximumMana(player));
    }

    public static void setKeyPressed(ServerPlayer player, String key, boolean pressed) {
        if (!key.startsWith("key.shape-shifter-curse.") && !"key.jump".equals(key)
                && !"key.sprint".equals(key)) {
            return;
        }
        Map<String, Boolean> keys = PRESSED_KEYS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        boolean wasPressed = keys.getOrDefault(key, false);
        keys.put(key, pressed);
        if ("key.jump".equals(key) && SscCommonConfig.ENABLE_MOVEMENT_DEBUG_LOGGING.get()) {
            ShapeShifterCurseForge.LOGGER.info(
                    "[SSC-JUMP-DEBUG] input player={} pressed={} wasPressed={} form={} fluidHeight={} eyeInWater={} velocity={}",
                    player.getGameProfile().getName(), pressed, wasPressed,
                    FormManager.current(player).id(), player.getFluidHeight(FluidTags.WATER),
                    player.isEyeInFluid(FluidTags.WATER), player.getDeltaMovement());
        }
        if (pressed && !wasPressed) {
            if (toggle(player, key)) {
                return;
            }
            if ("key.shape-shifter-curse.make_sound".equals(key) && triggerHiss(player)) {
                return;
            }
            // A surface-water active_self power (jump_out_water) must win over the
            // generic air-jump branch. Water-surface players are not onGround(), so
            // checking air jump first made the original SSC launch unreachable.
            if ("key.jump".equals(key) && triggerActive(player, key)) {
                return;
            }
            triggerActive(player, key);
        }
    }

    public static void tick(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        if (FAMILIAR_FOX_MANA.equals(manaType(player))
                && net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService
                .isInCursedMoon(player.level()) && mana(player) < maximumMana(player)) {
            gainMana(player, 0.02F);
        }
        tickCooldowns(player.getUUID());
        tickChargeReleases(player);
        if (player.onGround()) LEVITATE_TICKS.remove(player.getUUID());
        Map<ResourceLocation, Boolean> toggles = TOGGLES.get(player.getUUID());
        if (toggles != null) {
            toggles.keySet().removeIf(toggleId -> !FormPowerRegistry.has(player, toggleId));
        }
        boolean wasSprinting = SPRINTING.getOrDefault(player.getUUID(), false);
        SPRINTING.put(player.getUUID(), player.isSprinting());
        if (!wasSprinting && player.isSprinting()) {
            SPRINT_TO_SNEAK_TRIGGERED.put(player.getUUID(), false);
        }
        if (player.isSprinting() && !wasSprinting && player instanceof ServerPlayer serverPlayer) {
            triggerActive(serverPlayer, "key.sprint");
        }
        // Fabric's SprintingStateTracker checks the prior sprint tick against
        // the current Shift input, and fires once per sprint episode. Crouch
        // pose is not equivalent to Shift while an axolotl is crawling.
        if (wasSprinting && player.isShiftKeyDown()
                && !SPRINT_TO_SNEAK_TRIGGERED.getOrDefault(player.getUUID(), false)
                && player instanceof ServerPlayer serverPlayer) {
            SPRINT_TO_SNEAK_TRIGGERED.put(player.getUUID(), true);
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
                triggerContinuousActive(serverPlayer, key);
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

    public static boolean hasFamiliarFoxMana(Player player) {
        return FAMILIAR_FOX_MANA.equals(manaType(player));
    }

    public static boolean isToggleActive(Player player, ResourceLocation id) {
        FormPowerDefinition definition = FormPowerRegistry.all().get(id);
        boolean defaultValue = definition != null
                && FormPowerRuntime.booleanValue(definition.data(), "active_by_default", false);
        return TOGGLES.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                .getOrDefault(id, defaultValue);
    }

    private static boolean toggle(ServerPlayer player, String key) {
        final boolean[] changed = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"origins:toggle".equals(FormPowerRegistry.typeOf(power))
                    || !key.equals(FormPowerRuntime.stringValue(power.getAsJsonObject("key"), "key", ""))) return;
            boolean active = isToggleActive(player, id);
            TOGGLES.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).put(id, !active);
            changed[0] = true;
        });
        return changed[0];
    }

    private static boolean triggerHiss(ServerPlayer player) {
        final boolean[] triggered = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:hiss_phantom_power".equals(FormPowerRegistry.typeOf(power))) return;
            FormPowerRuntime.execute(player, player, power.getAsJsonObject("on_hiss_phantom_action"));
            triggered[0] = true;
        });
        return triggered[0];
    }

    public static void triggerVanillaKey(Player player, String key) {
        if (player instanceof ServerPlayer serverPlayer) {
            triggerActive(serverPlayer, key);
        }
    }

    /**
     * Runs the surface jump after vanilla has completed LivingEntity.travel().
     * The normal LivingTickEvent is before travel, so a launch applied there is
     * still treated as water motion and its Y component is damped by vanilla.
     */
    public static void postTravelTick(Player player) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        UUID id = player.getUUID();
        boolean touchingWater = player.getFluidHeight(FluidTags.WATER) > 0.0D;
        if (!touchingWater) {
            WATER_LAUNCH_GRACE.remove(id);
        }

        Map<String, Boolean> keys = PRESSED_KEYS.get(id);
        if (keys == null || !keys.getOrDefault("key.jump", false)) {
            return;
        }

        triggerContinuousJumpOutWater(serverPlayer);
        logJumpState(serverPlayer);
    }

    private static void logJumpState(ServerPlayer player) {
        if (!SscCommonConfig.ENABLE_MOVEMENT_DEBUG_LOGGING.get()) {
            return;
        }
        ShapeShifterCurseForge.LOGGER.info(
                "[SSC-JUMP-DEBUG] state stage={} player={} y={} yOld={} deltaY={} velocity={} onGround={} verticalCollision={} horizontalCollision={} fluidHeight={} eyeInWater={}",
                "post-travel-state", player.getGameProfile().getName(), player.getY(), player.yOld,
                player.getY() - player.yOld, player.getDeltaMovement(), player.onGround(),
                player.verticalCollision, player.horizontalCollision,
                player.getFluidHeight(FluidTags.WATER), player.isEyeInFluid(FluidTags.WATER));
    }

    /** Called by LivingEntity.travel() to preserve a just-issued surface launch. */
    public static void armWaterLaunchGrace(Player player) {
        if (player.getFluidHeight(FluidTags.WATER) > 0.0D) {
            WATER_LAUNCH_GRACE.put(player.getUUID(), Boolean.TRUE);
        }
    }

    /** Consumes the one-tick vertical water-damping bypass. */
    public static boolean consumeWaterLaunchGrace(Player player) {
        if (player.level().isClientSide) {
            return false;
        }
        return WATER_LAUNCH_GRACE.remove(player.getUUID()) != null;
    }

    public static void consumeMana(Player player, float amount) {
        if (!hasMana(player, amount)) {
            return;
        }
        setMana(player, mana(player) - amount);
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
        float initial = FAMILIAR_FOX_MANA.equals(type) ? 0.0F
                : WEB_RESOURCE.equals(type) ? 100.0F : DEFAULT_MANA;
        return SscApi.currentForm(player).map(data -> data.getManaPools().getOrDefault(type, initial))
                .orElse(initial);
    }

    /** The retained SSC data expresses mana thresholds as a 0..1 fraction. */
    public static float manaPercent(Player player) {
        return mana(player) / maximumMana(player);
    }

    private static float maximumMana(Player player) {
        String type = manaType(player);
        return FAMILIAR_FOX_MANA.equals(type) ? FAMILIAR_FOX_MAX_MANA
                : WEB_RESOURCE.equals(type) ? 100.0F : DEFAULT_MANA;
    }

    private static void setMana(Player player, float value) {
        String type = manaType(player);
        float maximum = maximumMana(player);
        float clamped = Math.max(0.0F, Math.min(maximum, value));
        SscApi.currentForm(player).ifPresent(data -> data.setManaPool(type, clamped));
        if (player instanceof ServerPlayer serverPlayer) {
            net.onixary.shapeShifterCurseForge.network.ModNetwork.sendManaSync(serverPlayer, type, clamped, maximum);
        }
    }

    /** Pushes the active pool after login, respawn, dimension changes and form swaps. */
    public static void synchronizeMana(ServerPlayer player) {
        if (!hasActiveManaType(player)) {
            net.onixary.shapeShifterCurseForge.network.ModNetwork.sendManaSync(player, "", 0.0F, DEFAULT_MANA);
            return;
        }
        String type = manaType(player);
        net.onixary.shapeShifterCurseForge.network.ModNetwork.sendManaSync(player, type, mana(player), maximumMana(player));
    }

    private static boolean hasActiveManaType(Player player) {
        final boolean[] found = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:mana_type_power".equals(FormPowerRegistry.typeOf(power)) && power.has("mana_type")) {
                found[0] = true;
            }
        });
        return found[0];
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

    private static boolean triggerActive(ServerPlayer player, String key) {
        final boolean[] triggered = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:active_self".equals(FormPowerRegistry.typeOf(power)) || usesKey(power, key)
                    || isOnCooldown(player, id)) {
                return;
            }
            JsonObject condition = power.has("condition") ? power.getAsJsonObject("condition") : power.getAsJsonObject("entity_condition");
            boolean jumpOutWater = JUMP_OUT_WATER.equals(id);
            boolean conditionMet = FormPowerRuntime.test(player, player, condition);
            if (jumpOutWater && SscCommonConfig.ENABLE_MOVEMENT_DEBUG_LOGGING.get()) {
                ShapeShifterCurseForge.LOGGER.info(
                        "[SSC-JUMP-DEBUG] press-candidate player={} condition={} fluidHeight={} eyeInWater={} velocityBefore={}",
                        player.getGameProfile().getName(), conditionMet,
                        player.getFluidHeight(FluidTags.WATER), player.isEyeInFluid(FluidTags.WATER),
                        player.getDeltaMovement());
            }
            if (!conditionMet) {
                return;
            }
            // Surface launch is deliberately deferred until PlayerTickEvent.END,
            // after vanilla travel has finished applying water damping.
            if (jumpOutWater) {
                return;
            }
            Vec3 before = player.getDeltaMovement();
            FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
            if (!before.equals(player.getDeltaMovement())) {
                player.hurtMarked = true;
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }
            startCooldown(player, id, FormPowerRuntime.intValue(power, "cooldown", 0));
            triggered[0] = true;
        });
        return triggered[0];
    }

    /** Runs only active_self powers whose key explicitly has continuous=true. */
    private static void triggerContinuousActive(ServerPlayer player, String key) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:active_self".equals(FormPowerRegistry.typeOf(power))
                    || usesKey(power, key)
                    || !power.getAsJsonObject("key").has("continuous")
                    || !power.getAsJsonObject("key").get("continuous").getAsBoolean()
                    || isOnCooldown(player, id)) {
                return;
            }
            JsonObject condition = power.has("condition")
                    ? power.getAsJsonObject("condition") : power.getAsJsonObject("entity_condition");
            boolean jumpOutWater = JUMP_OUT_WATER.equals(id);
            if (jumpOutWater) {
                return;
            }
            boolean conditionMet = FormPowerRuntime.test(player, player, condition);
            if (!conditionMet) {
                return;
            }
            FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
            startCooldown(player, id, FormPowerRuntime.intValue(power, "cooldown", 0));
        });
    }

    /** Executes only jump_out_water after the entity has finished vanilla travel. */
    private static void triggerContinuousJumpOutWater(ServerPlayer player) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!JUMP_OUT_WATER.equals(id)
                    || !"apoli:active_self".equals(FormPowerRegistry.typeOf(power))
                    || usesKey(power, "key.jump")
                    || !power.getAsJsonObject("key").has("continuous")
                    || !power.getAsJsonObject("key").get("continuous").getAsBoolean()
                    || isOnCooldown(player, id)) {
                return;
            }
            JsonObject condition = power.has("condition")
                    ? power.getAsJsonObject("condition") : power.getAsJsonObject("entity_condition");
            boolean conditionMet = FormPowerRuntime.test(player, player, condition);
            if (!conditionMet) {
                return;
            }
            Vec3 before = player.getDeltaMovement();
            if (SscCommonConfig.ENABLE_MOVEMENT_DEBUG_LOGGING.get()) {
                ShapeShifterCurseForge.LOGGER.info(
                        "[SSC-JUMP-DEBUG] post-travel-candidate player={} fluidHeight={} velocityBefore={}",
                        player.getGameProfile().getName(), player.getFluidHeight(FluidTags.WATER), before);
            }
            FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
            double maxY = FormPowerRuntime.doubleValue(power, "max_y_velocity", -1.0D);
            if (power.has("max_y_velocity") && maxY >= 0.0D && player.getDeltaMovement().y > maxY) {
                Vec3 capped = player.getDeltaMovement();
                player.setDeltaMovement(capped.x, maxY, capped.z);
            }
            armWaterLaunchGrace(player);
            syncLaunchVelocity(player);
            startCooldown(player, id, FormPowerRuntime.intValue(power, "cooldown", 0));
            if (SscCommonConfig.ENABLE_MOVEMENT_DEBUG_LOGGING.get()) {
                ShapeShifterCurseForge.LOGGER.info(
                        "[SSC-JUMP-DEBUG] post-travel-fired player={} velocityAfter={} deltaY={}",
                        player.getGameProfile().getName(), player.getDeltaMovement(),
                        player.getDeltaMovement().y - before.y);
            }
        });
    }

    /**
     * The server changes a player velocity, but the local client also predicts
     * player movement. Send the launch explicitly so its next movement tick
     * cannot continue with the ordinary swim trajectory.
     */
    private static void syncLaunchVelocity(ServerPlayer player) {
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }

    private static void charge(ServerPlayer player, String key) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:charge_action".equals(FormPowerRegistry.typeOf(power)) || usesKey(power, key)
                    || isOnCooldown(player, id)) {
                return;
            }
            ChargeState state = CHARGES.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                    .computeIfAbsent(id, ignored -> new ChargeState());
            state.lastUseTick = player.tickCount;
            state.ticks++;
            for (int tier = 0; tier < 10; tier++) {
                String prefix = "tier" + tier + "_";
                if (!FormPowerRuntime.booleanValue(power, prefix + "enable", tier == 0)) break;
                boolean justCompleted = false;
                if (state.tier + 1 == tier) {
                    JsonObject canCharge = power.getAsJsonObject(prefix + "can_charge_condition");
                    if (canCharge != null && !FormPowerRuntime.test(player, player, canCharge)) {
                        state.ticks--;
                    } else {
                        FormPowerRuntime.execute(player, player, power.getAsJsonObject(prefix + "charge_tick_action"));
                        int required = FormPowerRuntime.intValue(power, prefix + "charge_time", -1);
                        if (state.ticks >= required) {
                            JsonObject condition = power.getAsJsonObject(prefix + "condition");
                            if (condition != null && !FormPowerRuntime.test(player, player, condition)) {
                                state.ticks = required - 1;
                            } else {
                                FormPowerRuntime.execute(player, player, power.getAsJsonObject(prefix + "charge_complete_action"));
                                state.tier = tier;
                                justCompleted = true;
                            }
                        }
                    }
                }
                if (state.tier == tier) {
                    FormPowerRuntime.execute(player, player, power.getAsJsonObject(prefix + "tick_action"));
                }
                if (state.tier >= tier) {
                    FormPowerRuntime.execute(player, player, power.getAsJsonObject(prefix + "charge_complete_tick_action"));
                }
                JsonObject autoFire = power.getAsJsonObject(prefix + "auto_fire_condition");
                if (justCompleted && autoFire != null && FormPowerRuntime.test(player, player, autoFire)) {
                    fireCharge(player, id, power, state);
                    break;
                }
            }
        });
    }

    /** Fabric fires once no onUse call has arrived for more than two power ticks. */
    private static void tickChargeReleases(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        Map<ResourceLocation, ChargeState> states = CHARGES.get(player.getUUID());
        if (states == null || states.isEmpty()) return;
        Map<String, Boolean> keys = PRESSED_KEYS.getOrDefault(player.getUUID(), Map.of());
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:charge_action".equals(FormPowerRegistry.typeOf(power))) return;
            ChargeState state = states.get(id);
            if (state == null) return;
            String key = FormPowerRuntime.stringValue(power.getAsJsonObject("key"), "key", "");
            if (!keys.getOrDefault(key, false) && player.tickCount - state.lastUseTick > 2) {
                if (!isOnCooldown(player, id)) fireCharge(serverPlayer, id, power, state);
                states.remove(id);
            }
        });
    }

    private static void fireCharge(ServerPlayer player, ResourceLocation id, JsonObject power, ChargeState state) {
        if (state.ticks <= 0) return;
        for (int tier = 0; tier < 10; tier++) {
            String prefix = "tier" + tier + "_";
            if (!FormPowerRuntime.booleanValue(power, prefix + "enable", tier == 0)) break;
            if (state.tier == tier) {
                FormPowerRuntime.execute(player, player, power.getAsJsonObject(prefix + "use_action"));
                startCooldown(player, id, FormPowerRuntime.intValue(power, prefix + "cooldown", 0));
            }
            if (state.tier >= tier) {
                FormPowerRuntime.execute(player, player, power.getAsJsonObject(prefix + "charge_complete_use_action"));
            }
        }
        state.tier = 0;
        state.ticks = 0;
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

    private static boolean usesKey(JsonObject power, String key) {
        return !power.has("key") || !power.get("key").isJsonObject()
                || !key.equals(FormPowerRuntime.stringValue(power.getAsJsonObject("key"), "key", ""));
    }

    private static boolean isOnCooldown(Player player, ResourceLocation id) {
        return COOLDOWNS.getOrDefault(player.getUUID(), Map.of()).getOrDefault(id, 0) > 0;
    }

    /** Claims an Apoli cooldown after every condition has passed. */
    public static boolean usePowerCooldown(Player player, ResourceLocation id, int ticks) {
        if (isOnCooldown(player, id)) return false;
        startCooldown(player, id, ticks);
        return true;
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
