package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.client.Minecraft;

/** Client-only HUD state used by dist-agnostic power conditions. */
public final class ClientHudState {
    private ClientHudState() {
    }

    /** Mirrors Fabric's {@code ClientUtils.CanDisplayGUI}: the HUD is not hidden. */
    public static boolean canDisplayGui() {
        return !Minecraft.getInstance().options.hideGui;
    }
}