package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.advancement.SscAdvancementTriggers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Accumulates web-bullet binding time and fires the full-entanglement advancement. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class WebEntanglementService {
    private static final int FULL_THRESHOLD = 20 * 5 * 5;
    private static final Map<UUID, State> STATES = new HashMap<>();

    private WebEntanglementService() {
    }

    public static void apply(Entity owner, LivingEntity target, int duration) {
        if (target.level().isClientSide || duration <= 0) {
            return;
        }
        State state = STATES.get(target.getUUID());
        if (state != null && state.fullTicks > 0) {
            return;
        }
        if (state == null || state.expiresIn <= 0) {
            state = new State(0, 0);
        }
        state.duration = Math.min(FULL_THRESHOLD, state.duration + duration);
        state.expiresIn = Math.min(Integer.MAX_VALUE - duration, state.expiresIn) + duration;
        if (state.duration >= FULL_THRESHOLD) {
            state.duration = 0;
            state.fullTicks = target instanceof net.minecraft.world.entity.player.Player ? 20 * 5 : 20 * 15;
            if (owner instanceof ServerPlayer player) {
                ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
                SscAdvancementTriggers.ON_WEB_ENTITY.triggerEntity(player, entityId);
            }
        }
        STATES.put(target.getUUID(), state);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        State state = STATES.get(target.getUUID());
        if (state == null) {
            return;
        }
        state.expiresIn--;
        if (state.fullTicks > 0) {
            state.fullTicks--;
        }
        if (state.expiresIn <= 0 && state.fullTicks <= 0) {
            STATES.remove(target.getUUID());
        }
    }

    private static final class State {
        private int duration;
        private int expiresIn;
        private int fullTicks;

        private State(int duration, int expiresIn) {
            this.duration = duration;
            this.expiresIn = expiresIn;
        }
    }
}
