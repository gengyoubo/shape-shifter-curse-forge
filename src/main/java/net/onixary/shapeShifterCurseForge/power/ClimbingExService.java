package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.other.config.SscCommonConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Keeps the previous climbing result for each climbing_ex power, as Fabric does. */
public final class ClimbingExService {
    private static final Map<Player, State> STATES = new WeakHashMap<>();

    private static final class State {
        private ResourceLocation formId;
        private final Map<ResourceLocation, Boolean> wasClimbing = new HashMap<>();
        private final Map<ResourceLocation, Integer> lastDebugTick = new HashMap<>();
        private final Map<ResourceLocation, Boolean> lastDebugResult = new HashMap<>();

        private State(ResourceLocation formId) {
            this.formId = formId;
        }
    }

    private ClimbingExService() { }

    public static synchronized boolean isActive(Player player, ResourceLocation powerId, JsonObject power) {
        ResourceLocation formId = FormManager.current(player).id();
        State state = STATES.computeIfAbsent(player, ignored -> new State(formId));
        if (!formId.equals(state.formId)) {
            state.formId = formId;
            state.wasClimbing.clear();
            state.lastDebugTick.clear();
            state.lastDebugResult.clear();
        }
        if (!FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
            return false;
        }

        boolean wasClimbing = state.wasClimbing.getOrDefault(powerId, false);
        JsonObject condition = power.getAsJsonObject(wasClimbing
                ? "continue_climb_condition" : "start_climb_condition");
        boolean active = FormPowerRuntime.test(player, player, condition);
        Integer previousDebugTick = state.lastDebugTick.get(powerId);
        Boolean previousDebugResult = state.lastDebugResult.get(powerId);
        if (SscCommonConfig.ENABLE_MOVEMENT_DEBUG_LOGGING.get()
                && formId.getPath().startsWith("ocelot_")
                && (previousDebugTick == null || previousDebugTick != player.tickCount
                || previousDebugResult == null || previousDebugResult != active)) {
            state.lastDebugTick.put(powerId, player.tickCount);
            state.lastDebugResult.put(powerId, active);
            boolean startEligible = FormPowerRuntime.test(player, player,
                    power.getAsJsonObject("start_climb_condition"));
            boolean continueEligible = FormPowerRuntime.test(player, player,
                    power.getAsJsonObject("continue_climb_condition"));
            ShapeShifterCurseForge.LOGGER.info(
                    "[SSC-CLIMB-DEBUG] side={} tick={} form={} power={} phase={} start={} continue={} active={} horizontalCollision={} onGround={} position={} velocity={} box={}",
                    player.level().isClientSide ? "client" : "server", player.tickCount, formId, powerId,
                    wasClimbing ? "continue" : "start", startEligible, continueEligible, active,
                    player.horizontalCollision,
                    player.onGround(), player.position(), player.getDeltaMovement(), player.getBoundingBox());
        }
        state.wasClimbing.put(powerId, active);
        return active;
    }

    /** Fabric's ClimbingEXPower.canHold: disabled, custom condition, or Shift by default. */
    public static boolean canHold(Player player, JsonObject power) {
        if (!FormPowerRuntime.booleanValue(power, "allow_holding", true)) return false;
        JsonObject condition = power.getAsJsonObject("holding_condition");
        return condition == null ? player.isShiftKeyDown()
                : FormPowerRuntime.test(player, player, condition);
    }
}
