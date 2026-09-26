package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import java.util.Map;
import java.util.WeakHashMap;

/** Fabric LevitatePower: client ascent, server gravity state, and one tick of key release grace. */
public final class LevitatePowerService {
    private static final Map<Player, State> STATES = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final class State {
        int progress;
        boolean previousKey;
        boolean active;
        boolean ownsGravity;
    }
    private LevitatePowerService() { }

    public static boolean isActive(Player player) {
        State state = STATES.get(player);
        return state != null && state.active;
    }

    public static boolean isAvailable(Player player) {
        return STATES.containsKey(player);
    }

    public static void tick(Player player, boolean jumpPressed) {
        final com.google.gson.JsonObject[] definition = {null};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:levitate".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) definition[0] = power;
        });
        State state = STATES.computeIfAbsent(player, ignored -> new State());
        if (definition[0] == null || player.getFluidHeight(FluidTags.WATER) > 0
                || player.getFluidHeight(FluidTags.LAVA) > 0) {
            if (state.ownsGravity) player.setNoGravity(false);
            STATES.remove(player);
            return;
        }
        boolean active = jumpPressed || state.previousKey;
        state.active = active;
        player.setNoGravity(active);
        state.ownsGravity = active;
        if (!player.level().isClientSide) player.fallDistance = 0.0F;
        if (player.level().isClientSide && active) {
            int duration = FormPowerRuntime.intValue(definition[0], "max_ascend_duration", 40);
            double velocity = 0;
            if (state.progress < duration) {
                double remaining = 1.0D - (double) state.progress / duration;
                velocity = FormPowerRuntime.doubleValue(definition[0], "ascent_speed", 0.5D)
                        * remaining * remaining;
                state.progress++;
            }
            var current = player.getDeltaMovement();
            player.setDeltaMovement(current.x, velocity, current.z);
        }
        state.previousKey = jumpPressed;
        if (player.onGround()) {
            state.progress = 0;
            state.active = false;
        }
    }
}
