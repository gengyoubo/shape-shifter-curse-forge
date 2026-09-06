package net.onixary.shapeShifterCurseForge.api;

import java.util.Optional;

import net.minecraft.world.entity.player.Player;

/** Stable public API for accessing SSCF player data. */
public final class SscApi {
    private SscApi() {
    }

    public static Optional<PlayerFormData> currentForm(Player player) {
        return SscDataBridge.getFormData(player);
    }

    public static Optional<PlayerSkinData> currentSkin(Player player) {
        return SscDataBridge.getSkinData(player);
    }

    public static void copyPlayerData(Player original, Player replacement) {
        SscDataBridge.copyPlayerData(original, replacement);
    }
}
