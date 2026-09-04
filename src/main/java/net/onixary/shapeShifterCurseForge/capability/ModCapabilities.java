package net.onixary.shapeShifterCurseForge.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class ModCapabilities {
    public static final Capability<IPlayerFormData> PLAYER_FORM = CapabilityManager.get(
            new CapabilityToken<>() {
            }
    );
    public static final Capability<IPlayerSkinData> PLAYER_SKIN = CapabilityManager.get(
            new CapabilityToken<>() {
            }
    );

    private ModCapabilities() {
    }
}
