package net.onixary.shapeShifterCurseForge.form;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.power.LivingEntityJumpState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Forge counterpart of the Fabric {@code TransformManager}: a delayed, phase-based
 * transform instead of the previous "switch immediately, then play an animation".
 *
 * <p>Timeline (server ticks):</p>
 * <pre>
 * t = 0   startPlayerTransform  -> transform state broadcast, instinct lock, BLINDNESS, lock move/jump
 * t = 60  middlePlayerTransform -> actual setForm() switch, NAUSEA, re-lock move/jump
 * t = 160 endPlayerTransform    -> transform state clear, unlock instinct, final move lock
 * </pre>
 */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class TransformManager {
    public static final int TRANSFORM_FX_DURATION_IN = 3 * 20;   // 60
    public static final int TRANSFORM_FX_DURATION_OUT = 5 * 20;  // 100
    public static final int TRANSFORM_TOTAL_TICKS = TRANSFORM_FX_DURATION_IN + TRANSFORM_FX_DURATION_OUT;

    private static final Map<UUID, PlayerTransformData> DATA = new HashMap<>();
    private static final Set<UUID> INSTINCT_LOCKED = new HashSet<>();

    private TransformManager() {
    }

    /** Entry point mirroring Fabric's {@code forceTransform(target, form, immediately)}. */
    public static boolean forceTransform(ServerPlayer player, ResourceLocation targetId, boolean immediately) {
        if (immediately) {
            return immediatelyTransform(player, targetId);
        }
        return startTransform(player, targetId);
    }

    public static boolean startTransform(ServerPlayer player, ResourceLocation targetId) {
        FormDefinition target = FormRegistry.get(targetId);
        if (target == null) {
            return false;
        }
        PlayerTransformData data = getPlayerData(player);
        data.transformTimer = 0;
        data.transformStartForm = FormManager.current(player).id();
        data.transformEndForm = targetId;
        return true;
    }

    public static boolean immediatelyTransform(ServerPlayer player, ResourceLocation targetId) {
        return FormManager.setForm(player, targetId, false);
    }

    /** Whether a player is currently inside the transforming window. */
    public static boolean isTransforming(Player player) {
        PlayerTransformData data = DATA.get(player.getUUID());
        return data != null && data.transformTimer >= 0;
    }

    /** Fabric locks Instinct for the whole transforming window. */
    public static boolean isInstinctLocked(Player player) {
        return INSTINCT_LOCKED.contains(player.getUUID());
    }

    /** Cancels an in-progress transform without completing the form switch. */
    public static void cancel(ServerPlayer player) {
        PlayerTransformData data = DATA.remove(player.getUUID());
        if (data == null) {
            return;
        }
        INSTINCT_LOCKED.remove(player.getUUID());
        setNoMoveTicks(player, 0);
        setNoJumpTicks(player, 0);
        ModNetwork.sendMovementLock(player, 0, 0);
        ModNetwork.sendTransformState(player, false, data.transformStartForm, data.transformEndForm);
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server == null || server.getPlayerCount() == 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerTransformData data = DATA.get(player.getUUID());
            if (data == null || data.transformTimer < 0) {
                continue;
            }
            int timer = data.transformTimer;
            if (timer == 0) {
                startPlayerTransform(player);
            } else if (timer == TRANSFORM_FX_DURATION_IN) {
                middlePlayerTransform(player);
            } else if (timer == TRANSFORM_TOTAL_TICKS) {
                endPlayerTransform(player);
                data.transformTimer = -1;
            }
            if (data.transformTimer >= 0) {
                data.transformTimer++;
            }
        }
    }

    private static void startPlayerTransform(ServerPlayer player) {
        PlayerTransformData data = getPlayerData(player);
        ModNetwork.sendTransformState(player, true, data.transformStartForm, data.transformEndForm);
        INSTINCT_LOCKED.add(player.getUUID());
        applyStartTransformEffect(player, TRANSFORM_FX_DURATION_IN);
    }

    private static void middlePlayerTransform(ServerPlayer player) {
        PlayerTransformData data = getPlayerData(player);
        if (data.transformEndForm != null) {
            FormManager.setForm(player, data.transformEndForm, false);
        }
        applyEndTransformEffect(player, TRANSFORM_FX_DURATION_OUT);
    }

    private static void endPlayerTransform(ServerPlayer player) {
        PlayerTransformData data = getPlayerData(player);
        ModNetwork.sendTransformState(player, false, data.transformStartForm, data.transformEndForm);
        INSTINCT_LOCKED.remove(player.getUUID());
        applyFinaleTransformEffect(player, 5);
    }

    private static void applyStartTransformEffect(ServerPlayer player, int ticks) {
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks));
        lockMovement(player, ticks);
    }

    private static void applyEndTransformEffect(ServerPlayer player, int ticks) {
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks));
        lockMovement(player, ticks);
    }

    private static void applyFinaleTransformEffect(ServerPlayer player, int ticks) {
        setNoMoveTicks(player, ticks);
        ModNetwork.sendMovementLock(player, ticks, 0);
    }

    private static void lockMovement(ServerPlayer player, int ticks) {
        setNoMoveTicks(player, ticks);
        setNoJumpTicks(player, ticks);
        ModNetwork.sendMovementLock(player, ticks, ticks);
    }

    private static void setNoMoveTicks(ServerPlayer player, int ticks) {
        if (player instanceof LivingEntityJumpState state) {
            state.ssc$setNoMoveTick(ticks);
        }
    }

    private static void setNoJumpTicks(ServerPlayer player, int ticks) {
        if (player instanceof LivingEntityJumpState state) {
            state.ssc$setNoJumpTick(ticks);
        }
    }

    private static PlayerTransformData getPlayerData(ServerPlayer player) {
        return DATA.computeIfAbsent(player.getUUID(), ignored -> new PlayerTransformData());
    }

    private static final class PlayerTransformData {
        private int transformTimer = -1;
        private ResourceLocation transformStartForm;
        private ResourceLocation transformEndForm;
    }
}
