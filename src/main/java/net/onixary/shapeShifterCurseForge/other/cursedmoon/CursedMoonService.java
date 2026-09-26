package net.onixary.shapeShifterCurseForge.other.cursedmoon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.other.advancement.SscAdvancementTriggers;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.other.config.SscCommonConfig;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.form.TransformManager;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.api.PlayerFormData;
import net.onixary.shapeShifterCurseForge.power.TransformativeEffectService;

import java.util.List;
import java.util.Objects;

/** Server-side Cursed Moon state machine, ported from Fabric's world tick flow. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CursedMoonService {
    private static long lastProcessedDay = Long.MIN_VALUE;

    private CursedMoonService() {
    }

    public static boolean isCursedMoonByPhase(int moonPhase) {
        for (int configuredPhase : SscCommonConfig.cursedMoonPhases()) {
            if (configuredPhase == moonPhase) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCursedMoonDay(net.minecraft.world.level.Level level) {
        return level != null && isCursedMoonByPhase(level.getMoonPhase());
    }

    public static boolean isNight(net.minecraft.world.level.Level level) {
        if (level == null) {
            return false;
        }
        long timeOfDay = Math.floorMod(level.getDayTime(), 24000L);
        return timeOfDay > 12000L && timeOfDay < 23000L;
    }

    public static boolean isInCursedMoon(net.minecraft.world.level.Level level) {
        return isCursedMoonDay(level) && isNight(level);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            serverTick(server);
        }
    }

    public static void serverTick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        long day = Math.floorDiv(overworld.getDayTime(), 24000L);
        boolean cursedMoonDay = isCursedMoonDay(overworld);
        if (day != lastProcessedDay) {
            lastProcessedDay = day;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ModNetwork.sendCursedMoonSync(player, cursedMoonDay);
            }
        }

        if (isInCursedMoon(overworld)) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!SscCommonConfig.ALLOW_SLEEP_IN_CURSED_MOON.get() && player.isSleeping()) {
                    player.stopSleeping();
                }
                applyStartCursedMoonEffect(player);
            }
        } else {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                applyEndCursedMoonEffect(player);
            }
        }
    }

    public static void sendDaySync(ServerPlayer player) {
        ModNetwork.sendCursedMoonSync(player, isCursedMoonDay(Objects.requireNonNull(player.getServer()).overworld()));
    }

    public static void applyStartCursedMoonEffect(ServerPlayer player) {
        SscApi.currentForm(player).ifPresent(data -> {
            if (data.isCursedMoonApplied()) {
                return;
            }

            FormDefinition current = FormManager.current(player);
            boolean beforeEnable = FormRegistry.ORIGINAL_BEFORE_ENABLE.equals(current.id());
            if (beforeEnable) {
                if (player.level().dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                    sendMessage(player, "info.shape-shifter-curse.on_cursed_moon_before_enable");
                }
            } else {
                sendMessage(player, player.level().dimension() == net.minecraft.world.level.Level.OVERWORLD
                        ? "info.shape-shifter-curse.on_cursed_moon"
                        : "info.shape-shifter-curse.on_cursed_moon_nether");
                SscAdvancementTriggers.ON_TRIGGER_CURSED_MOON.trigger(player);
            }

            data.setLastTransformByCure(false);
            data.setBeforeCursedMoonAppliedForm(null);
            data.setAfterCursedMoonAppliedForm(null);

            if (!beforeEnable && SscCommonConfig.ENABLE_CURSED_MOON_TRANSFORM.get()
                    && !current.hasFlag("no_cursed_moon_effect")) {
                FormDefinition next = nextCursedMoonForm(player, current);
                if (next != null && !next.id().equals(current.id())) {
                    data.setBeforeCursedMoonAppliedForm(current.id().toString());
                    data.setAfterCursedMoonAppliedForm(next.id().toString());
                    TransformManager.forceTransform(player, next.id(), false);
                    if (current.hasFlag("cursed_moon_final_form")) {
                        SscAdvancementTriggers.ON_TRIGGER_CURSED_MOON_FORM_2.trigger(player);
                    }
                }
            }
            data.setCursedMoonApplied(true);
            ModNetwork.sendFormSync(player);
        });
    }

    public static void applyEndCursedMoonEffect(ServerPlayer player) {
        SscApi.currentForm(player).ifPresent(data -> {
            if (!data.isCursedMoonApplied()) {
                return;
            }

            String beforeFormId = data.getBeforeCursedMoonAppliedForm();
            FormDefinition current = FormManager.current(player);
            if (FormRegistry.ORIGINAL_BEFORE_ENABLE.equals(current.id())) {
                if (player.level().dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                    sendMessage(player, "info.shape-shifter-curse.end_cursed_moon_before_enable");
                }
            } else if (data.wasLastTransformByCure()) {
                sendMessage(player, "info.shape-shifter-curse.end_cursed_moon_by_cure");
                SscAdvancementTriggers.ON_END_CURSED_MOON_CURED.trigger(player);
                String afterFormId = data.getAfterCursedMoonAppliedForm();
                FormDefinition afterForm = afterFormId == null ? null : FormRegistry.get(afterFormId);
                if (afterForm != null && afterForm.stage() == 1) {
                    SscAdvancementTriggers.ON_END_CURSED_MOON_CURED_FORM_2.trigger(player);
                }
            } else if (FormRegistry.ORIGINAL_SHIFTER.equals(current.id())) {
                sendMessage(player, "info.shape-shifter-curse.end_cursed_moon_special");
            } else if (beforeFormId != null && FormRegistry.get(beforeFormId) != null
                    && data.getAfterCursedMoonAppliedForm() != null
                    && data.getAfterCursedMoonAppliedForm().equals(current.id().toString())) {
                sendMessage(player, "info.shape-shifter-curse.end_cursed_moon");
                SscAdvancementTriggers.ON_END_CURSED_MOON.trigger(player);
                ResourceLocation beforeId = ResourceLocation.tryParse(beforeFormId);
                if (beforeId != null && FormRegistry.get(beforeId) != null) {
                    TransformManager.forceTransform(player, beforeId, false);
                }
            }

            data.setCursedMoonApplied(false);
            data.setLastTransformByCure(false);
            data.setBeforeCursedMoonAppliedForm(null);
            data.setAfterCursedMoonAppliedForm(null);
            ModNetwork.sendFormSync(player);
        });
    }

    public static void forceTriggerCursedMoon(ServerLevel commandLevel) {
        ServerLevel overworld = commandLevel.getServer().overworld();
        long currentDay = Math.floorDiv(overworld.getDayTime(), 24000L);
        int currentPhase = overworld.getMoonPhase();
        int nextPhase = getNextCursedMoonPhase(currentPhase);
        long daysUntil = Math.floorMod(nextPhase - currentPhase, 8);
        if (daysUntil == 0) {
            daysUntil = 8;
        }
        long nextDay = currentDay + daysUntil;
        overworld.setDayTime(nextDay * 24000L + Math.floorMod(overworld.getDayTime(), 24000L));
        for (ServerPlayer player : overworld.getServer().getPlayerList().getPlayers()) {
            sendMessage(player, "info.shape-shifter-curse.cursed_moon_forced");
        }
        lastProcessedDay = Long.MIN_VALUE;
    }

    public static int getNextCursedMoonPhase(int currentPhase) {
        for (int offset = 0; offset < 8; offset++) {
            int phase = Math.floorMod(currentPhase + offset, 8);
            if (isCursedMoonByPhase(phase)) {
                return phase;
            }
        }
        return currentPhase;
    }

    private static FormDefinition nextCursedMoonForm(ServerPlayer player, FormDefinition current) {
        if (FormRegistry.ORIGINAL_SHIFTER.equals(current.id())) {
            ResourceLocation transformativeTarget = SscApi.currentForm(player)
                    .filter(ignored -> TransformativeEffectService.has(player))
                    .map(PlayerFormData::getTransformativeEffectFormId)
                    .map(ResourceLocation::tryParse)
                    .orElse(null);
            if (transformativeTarget != null) {
                FormDefinition target = FormRegistry.get(transformativeTarget);
                if (target != null) {
                    return target;
                }
            }
            List<FormDefinition> starters = FormRegistry.forms().values().stream()
                    .filter(form -> form.hasFlag("starter_form"))
                    .toList();
            return starters.isEmpty() ? current : starters.get(player.getRandom().nextInt(starters.size()));
        }

        int targetStage = current.hasFlag("cursed_moon_final_form") ? 1 : current.stage() + 1;
        var group = FormRegistry.getGroup(current.groupId());
        if (group == null) {
            return current;
        }
        List<FormDefinition> candidates = group.formsAtStage(targetStage).stream()
                .filter(form -> !form.hasFlag("no_cursed_moon_target"))
                .toList();
        return candidates.isEmpty() ? current : candidates.get(player.getRandom().nextInt(candidates.size()));
    }

    private static void sendMessage(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
