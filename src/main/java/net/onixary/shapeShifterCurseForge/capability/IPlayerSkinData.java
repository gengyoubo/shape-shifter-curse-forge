package net.onixary.shapeShifterCurseForge.capability;

import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.api.SscApi;

/** Server-authoritative per-player form skin (color) settings. Colors are ABGR. */
public interface IPlayerSkinData extends net.onixary.shapeShifterCurseForge.api.PlayerSkinData {
    /** @deprecated Use {@link SscApi#currentSkin(Player)} instead. */
    @Deprecated(forRemoval = false)
    boolean isKeepOriginalSkin();

    /** @deprecated Use {@link SscApi#currentSkin(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setKeepOriginalSkin(boolean keepOriginalSkin);

    /** @deprecated Use {@link SscApi#currentSkin(Player)} instead. */
    @Deprecated(forRemoval = false)
    boolean isEnableFormColor();

    /** @deprecated Use {@link SscApi#currentSkin(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setEnableFormColor(boolean enableFormColor);

    /** @deprecated Use {@link SscApi#currentSkin(Player)} instead. */
    @Deprecated(forRemoval = false)
    FormTextureUtils.ColorSetting getFormColor();

    /** @deprecated Use {@link SscApi#currentSkin(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setFormColor(FormTextureUtils.ColorSetting formColor);

    /** @deprecated Use {@link SscApi#currentSkin(Player)} instead. */
    @Deprecated(forRemoval = false)
    boolean isEnableFormRandomSound();

    /** @deprecated Use {@link SscApi#currentSkin(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setEnableFormRandomSound(boolean enableFormRandomSound);

    /** @deprecated Use {@link SscApi#currentSkin(Player)} instead. */
    @Deprecated(forRemoval = false)
    void copyFrom(IPlayerSkinData other);

    @Override
    default void copyFrom(net.onixary.shapeShifterCurseForge.api.PlayerSkinData other) {
        if (!(other instanceof IPlayerSkinData legacy)) {
            throw new IllegalArgumentException("Cannot copy skin data from an incompatible implementation");
        }
        copyFrom(legacy);
    }
}
