package net.onixary.shapeShifterCurseForge.team;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Forge port of Fabric's {@code MobTeamManager}. A single scoreboard team marks
 * players who are friendly to pillagers (sorcery allies). Friendly fire is
 * disabled so allies cannot hurt each other.
 */
public final class MobTeamManager {
    public static final String SORCERY_TEAM_NAME = "sorcery_team";

    private MobTeamManager() {
    }

    public static PlayerTeam registerTeam(ServerLevel level) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(SORCERY_TEAM_NAME);
        if (team == null) {
            team = scoreboard.addPlayerTeam(SORCERY_TEAM_NAME);
            team.setAllowFriendlyFire(false);
            team.setSeeFriendlyInvisibles(true);
        }
        return team;
    }
}