package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.client.render.FormAnimationSystem;
import net.onixary.shapeShifterCurseForge.form.TransformManager;
import net.onixary.shapeShifterCurseForge.power.LivingEntityJumpState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Client mirror of the server transform lifecycle: animation start, overlay strengths, movement lock. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class TransformClientState {
    private static final Set<UUID> TRANSFORMING_PLAYERS = new HashSet<>();
    private static int transformTimer = -1;
    private static boolean transforming = false;
    private static float nauseaStrength = 0.0F;
    private static float blackStrength = 0.0F;

    private TransformClientState() {
    }

    public static void apply(int entityId, boolean isTransforming, String startFormId, String endFormId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(entityId);
        if (!(entity instanceof Player player)) {
            return;
        }
        if (isTransforming) {
            TRANSFORMING_PLAYERS.add(player.getUUID());
            FormAnimationSystem.startTransition(player, startFormId, endFormId);
        } else {
            TRANSFORMING_PLAYERS.remove(player.getUUID());
        }
        // The overlay and movement lock only ever apply to the local player.
        if (minecraft.player != null && entityId == minecraft.player.getId()) {
            if (isTransforming) {
                transformTimer = 0;
                transforming = true;
                lockMovement(player, TransformManager.TRANSFORM_FX_DURATION_IN);
            } else {
                transforming = false;
                transformTimer = -1;
                nauseaStrength = 0.0F;
                blackStrength = 0.0F;
                lockMovement(player, 0);
            }
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (transformTimer < 0) {
            return;
        }
        float nausea = 0.0F;
        float black = 0.0F;
        if (transformTimer < TransformManager.TRANSFORM_FX_DURATION_IN) {
            nausea = transformTimer / (float) TransformManager.TRANSFORM_FX_DURATION_IN;
            black = Math.max(nausea - 0.8F, 0.0F) * 5.0F;
        } else if (transformTimer < TransformManager.TRANSFORM_TOTAL_TICKS) {
            nausea = 1.0F - (transformTimer - TransformManager.TRANSFORM_FX_DURATION_IN)
                    / (float) TransformManager.TRANSFORM_FX_DURATION_IN;
            black = Math.min(1.0F, nausea / 0.6F);
        } else {
            transformTimer = -1;
            transforming = false;
        }
        nauseaStrength = nausea;
        blackStrength = black;
        if (transformTimer >= 0) {
            transformTimer++;
        }
    }

    public static boolean isTransforming() {
        return transforming;
    }

    /** Per-player transforming flag, used by the animation system for observers too. */
    public static boolean isTransforming(Player player) {
        return TRANSFORMING_PLAYERS.contains(player.getUUID());
    }

    public static float nauseaStrength() {
        return nauseaStrength;
    }

    public static float blackStrength() {
        return blackStrength;
    }

    public static void applyMovementLock(int noMoveTicks, int noJumpTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player instanceof LivingEntityJumpState state) {
            state.ssc$setNoMoveTick(noMoveTicks);
            state.ssc$setNoJumpTick(noJumpTicks);
        }
    }

    private static void lockMovement(Player player, int ticks) {
        if (player instanceof LivingEntityJumpState state) {
            state.ssc$setNoMoveTick(ticks);
            state.ssc$setNoJumpTick(ticks);
        }
    }
}
