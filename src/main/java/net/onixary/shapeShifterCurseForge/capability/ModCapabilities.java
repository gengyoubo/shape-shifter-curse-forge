package net.onixary.shapeShifterCurseForge.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.api.SscApi;

public final class ModCapabilities {
    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    public static final Capability<IPlayerFormData> PLAYER_FORM = CapabilityManager.get(
            new CapabilityToken<>() {
            }
    );
    /** @deprecated Use {@link SscApi#currentSkin(Player)} instead. */
    @Deprecated(forRemoval = false)
    public static final Capability<IPlayerSkinData> PLAYER_SKIN = CapabilityManager.get(
            new CapabilityToken<>() {
            }
    );

    private ModCapabilities() {
    }
}
