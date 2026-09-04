package net.onixary.shapeShifterCurseForge.client.codex;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.config.SscCommonConfig;

/**
 * Client readout for the Codex. The server packet is authoritative while connected;
 * the common phase list is only used as a safe fallback before the first packet.
 */
public final class CursedMoonData {
    private static volatile Boolean serverCursedMoonDay;
    private static boolean middayMessageSent;

    private CursedMoonData() {
    }

    public static boolean isCursedMoonByPhase(int moonPhase) {
        for (int phase : SscCommonConfig.cursedMoonPhases()) {
            if (phase == moonPhase) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCursedMoonDay(Level level) {
        if (level == null) {
            return false;
        }
        if (level.isClientSide && serverCursedMoonDay != null) {
            return serverCursedMoonDay;
        }
        return isCursedMoonByPhase(level.getMoonPhase());
    }

    public static boolean isNight(Level level) {
        if (level == null) {
            return false;
        }
        long timeOfDay = level.getDayTime() % 24000L;
        return timeOfDay > 12000L && timeOfDay < 23000L;
    }

    public static boolean isInCursedMoon(Level level) {
        return isCursedMoonDay(level) && isNight(level);
    }

    public static void setServerCursedMoonDay(boolean cursedMoonDay) {
        serverCursedMoonDay = cursedMoonDay;
        middayMessageSent = false;
    }

    public static void clientTick(Level level) {
        if (level == null || !isCursedMoonDay(level)) {
            return;
        }
        long timeOfDay = level.getDayTime() % 24000L;
        if (timeOfDay >= 6000L && timeOfDay < 12500L && !middayMessageSent) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable(
                        level.dimension() == Level.NETHER
                                ? "info.shape-shifter-curse.before_cursed_moon_nether"
                                : "info.shape-shifter-curse.before_cursed_moon"), false);
                middayMessageSent = true;
            }
        }
    }
}
