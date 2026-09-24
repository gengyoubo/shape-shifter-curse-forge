package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormManager;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Keeps the previous climbing result for each climbing_ex power, as Fabric does. */
public final class ClimbingExService {
    private static final Map<Player, State> STATES = new WeakHashMap<>();

    private static final class State {
        private ResourceLocation formId;
        private final Map<ResourceLocation, Boolean> wasClimbing = new HashMap<>();

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
        }
        if (!FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return false;

        boolean wasClimbing = state.wasClimbing.getOrDefault(powerId, false);
        JsonObject condition = power.getAsJsonObject(wasClimbing
                ? "continue_climb_condition" : "start_climb_condition");
        boolean active = FormPowerRuntime.test(player, player, condition);
        state.wasClimbing.put(powerId, active);
        return active;
    }
}
