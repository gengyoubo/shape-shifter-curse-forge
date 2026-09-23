package net.onixary.shapeShifterCurseForge.team;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;

/**
 * Forge port of Fabric's {@code PlayerTeamHandler}: keeps the sorcery team
 * membership in sync with the {@code pillager_friendly} power.
 */
public final class PlayerTeamHandler {
    private static final ResourceLocation PILLAGER_FRIENDLY = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "pillager_friendly");

    private PlayerTeamHandler() {
    }

    public static void updatePlayerTeam(ServerPlayer player) {
        if (player.level() == null) {
            return;
        }
        PlayerTeam team = MobTeamManager.registerTeam(player.serverLevel());
        String name = player.getScoreboardName();
        PlayerTeam current = player.getScoreboard().getPlayersTeam(name);
        if (FormPowerRegistry.has(player, PILLAGER_FRIENDLY)) {
            if (current == null || !MobTeamManager.SORCERY_TEAM_NAME.equals(current.getName())) {
                player.getScoreboard().addPlayerToTeam(name, team);
            }
        } else if (current != null && MobTeamManager.SORCERY_TEAM_NAME.equals(current.getName())) {
            player.getScoreboard().removePlayerFromTeam(name, team);
        }
    }
}