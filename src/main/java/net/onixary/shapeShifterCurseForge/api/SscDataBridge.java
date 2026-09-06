package net.onixary.shapeShifterCurseForge.api;

import java.util.Optional;

import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.capability.ModCapabilities;

/**
 * Stable access point for SSCF player data.
 *
 * <p>SSCF currently resolves these values through Forge Capabilities. That is
 * an implementation detail and must remain isolated in this class so the
 * backend can later be replaced by a dual-loader adapter.</p>
 */
@SuppressWarnings("deprecation")
final class SscDataBridge {
    private SscDataBridge() {
    }

    static Optional<PlayerFormData> getFormData(Player player) {
        return player.getCapability(ModCapabilities.PLAYER_FORM).map(data -> data);
    }

    static Optional<PlayerSkinData> getSkinData(Player player) {
        return player.getCapability(ModCapabilities.PLAYER_SKIN).map(data -> data);
    }

    static void copyPlayerData(Player original, Player replacement) {
        getFormData(original).ifPresent(oldData ->
                getFormData(replacement).ifPresent(newData -> newData.copyFrom(oldData)));
        getSkinData(original).ifPresent(oldData ->
                getSkinData(replacement).ifPresent(newData -> newData.copyFrom(oldData)));
    }
}
