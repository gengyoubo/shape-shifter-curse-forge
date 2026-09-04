package net.onixary.shapeShifterCurseForge.cursedmoon;

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
import net.onixary.shapeShifterCurseForge.capability.ModCapabilities;
import net.onixary.shapeShifterCurseForge.config.SscCommonConfig;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormGroup;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;

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
        ModNetwork.sendCursedMoonSync(player, isCursedMoonDay(player.getServer().overworld()));
    }

    public static void applyStartCursedMoonEffect(ServerPlayer player) {
        player.getCapability(ModCapabilities.PLAYER_FORM).ifPresent(data -> {
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
            }

            data.setLastTransformByCure(false);
            data.setBeforeCursedMoonAppliedForm(null);
            data.setAfterCursedMoonAppliedForm(null);

            if (!beforeEnable && SscCommonConfig.ENABLE_CURSED_MOON_TRANSFORM.get()
                    && !current.hasFlag("no_cursed_moon_effect")) {
                FormDefinition next = nextCursedMoonForm(current);
                if (next != null && !next.id().equals(current.id())) {
                    data.setBeforeCursedMoonAppliedForm(current.id().toString());
                    data.setAfterCursedMoonAppliedForm(next.id().toString());
                    FormManager.setForm(player, next.id(), true);
                }
            }
            data.setCursedMoonApplied(true);
            ModNetwork.sendFormSync(player);
        });
    }

    public static void applyEndCursedMoonEffect(ServerPlayer player) {
        player.getCapability(ModCapabilities.PLAYER_FORM).ifPresent(data -> {
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
            } else if (FormRegistry.ORIGINAL_SHIFTER.equals(current.id())) {
                sendMessage(player, "info.shape-shifter-curse.end_cursed_moon_special");
            } else if (beforeFormId != null && FormRegistry.get(beforeFormId) != null
                    && data.getAfterCursedMoonAppliedForm() != null
                    && data.getAfterCursedMoonAppliedForm().equals(current.id().toString())) {
                sendMessage(player, "info.shape-shifter-curse.end_cursed_moon");
                ResourceLocation beforeId = ResourceLocation.tryParse(beforeFormId);
                if (beforeId != null && FormRegistry.get(beforeId) != null) {
                    FormManager.setForm(player, beforeId, true);
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

    private static FormDefinition nextCursedMoonForm(FormDefinition current) {
        FormGroup group = FormRegistry.getGroup(current.groupId());
        if (group == null) {
            return null;
        }
        return group.firstAtTier(current.tier() + 1);
    }

    private static void sendMessage(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
