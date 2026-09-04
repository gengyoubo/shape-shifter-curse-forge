package net.onixary.shapeShifterCurseForge.client.codex;

import net.minecraft.world.level.Level;

/**
 * Cursed-moon day/night readout for the Codex, mirroring Fabric's mapping onto
 * vanilla moon phases. Fabric syncs a server-computed flag because its phase list is
 * configurable per side; the phase list here matches its default ({@code {1, 5}}) and
 * the client reads its own world directly, which is deterministic either way.
 */
public final class CursedMoonData {
    // TODO: move to a common config once one exists (Fabric default: {1, 5}).
    private static final int[] CURSE_MOON_PHASES = {1, 5};

    private CursedMoonData() {
    }

    public static boolean isCursedMoonByPhase(int moonPhase) {
        for (int phase : CURSE_MOON_PHASES) {
            if (phase == moonPhase) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCursedMoonDay(Level level) {
        return level != null && isCursedMoonByPhase(level.getMoonPhase());
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
}
