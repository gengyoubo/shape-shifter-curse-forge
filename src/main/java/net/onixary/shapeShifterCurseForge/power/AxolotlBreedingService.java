package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.form.FormManager;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Gives nearby adult axolotls a rare chance to start vanilla breeding for an axolotl-form player. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class AxolotlBreedingService {
    private static final long RETRY_INTERVAL_TICKS = 10L * 20L;
    private static final long SUCCESS_COOLDOWN_TICKS = 40L * 60L * 20L;
    private static final double SEARCH_RADIUS = 8.0D;
    private static final float ATTEMPT_CHANCE = 0.01F;

    private static final Map<UUID, Long> NEXT_ATTEMPT = new HashMap<>();
    private static final Map<UUID, Long> SUCCESS_COOLDOWN_UNTIL = new HashMap<>();
    private static final Map<UUID, UUID> ATTEMPTING_PLAYER = new HashMap<>();

    private AxolotlBreedingService() {
    }

    @SubscribeEvent
    public static void playerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || !isAxolotlForm(player)) {
            return;
        }

        long now = player.level().getGameTime();
        for (Axolotl axolotl : player.level().getEntitiesOfClass(Axolotl.class,
                        player.getBoundingBox().inflate(SEARCH_RADIUS), candidate ->
                                candidate.isAlive() && !candidate.isBaby()
                                        && candidate.canFallInLove() && !candidate.isInLove())
                .stream()
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .toList()) {
            UUID axolotlId = axolotl.getUUID();
            if (now < SUCCESS_COOLDOWN_UNTIL.getOrDefault(axolotlId, 0L)
                    || now < NEXT_ATTEMPT.getOrDefault(axolotlId, Long.MIN_VALUE)) {
                continue;
            }
            NEXT_ATTEMPT.put(axolotlId, now + RETRY_INTERVAL_TICKS);
            if (player.getRandom().nextFloat() < ATTEMPT_CHANCE) {
                // This uses the vanilla love/breeding path. A successful child spawn
                // is handled below and starts this axolotl's long cooldown.
                ATTEMPTING_PLAYER.put(axolotlId, player.getUUID());
                axolotl.setInLove(player);
            }
        }
    }

    @SubscribeEvent
    public static void breedingSucceeded(BabyEntitySpawnEvent event) {
        Player player = event.getCausedByPlayer();
        if (!(event.getChild() instanceof Axolotl)
                || player == null
                || !isAxolotlForm(player)) {
            return;
        }

        long cooldownUntil = player.level().getGameTime() + SUCCESS_COOLDOWN_TICKS;
        markCooldown(event.getParentA(), player, cooldownUntil);
        markCooldown(event.getParentB(), player, cooldownUntil);
    }

    private static void markCooldown(net.minecraft.world.entity.Mob parent,
                                     Player player, long cooldownUntil) {
        if (!(parent instanceof Axolotl axolotl)) {
            return;
        }
        UUID axolotlId = axolotl.getUUID();
        UUID attemptingPlayer = ATTEMPTING_PLAYER.remove(axolotlId);
        if (player.getUUID().equals(attemptingPlayer)
                || player.equals(axolotl.getLoveCause())) {
            SUCCESS_COOLDOWN_UNTIL.put(axolotlId, cooldownUntil);
            NEXT_ATTEMPT.put(axolotlId, cooldownUntil);
        }
    }

    private static boolean isAxolotlForm(Player player) {
        return "axolotl_form".equals(FormManager.current(player).groupId().getPath());
    }
}
