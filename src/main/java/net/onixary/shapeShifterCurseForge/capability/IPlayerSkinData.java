package net.onixary.shapeShifterCurseForge.capability;

import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;

/** Server-authoritative per-player form skin (color) settings. Colors are ABGR. */
public interface IPlayerSkinData {
    @Deprecated(forRemoval = false)
    boolean isKeepOriginalSkin();

    @Deprecated(forRemoval = false)
    void setKeepOriginalSkin(boolean keepOriginalSkin);

    @Deprecated(forRemoval = false)
    boolean isEnableFormColor();

    @Deprecated(forRemoval = false)
    void setEnableFormColor(boolean enableFormColor);

    @Deprecated(forRemoval = false)
    FormTextureUtils.ColorSetting getFormColor();

    @Deprecated(forRemoval = false)
    void setFormColor(FormTextureUtils.ColorSetting formColor);

    @Deprecated(forRemoval = false)
    boolean isEnableFormRandomSound();

    @Deprecated(forRemoval = false)
    void setEnableFormRandomSound(boolean enableFormRandomSound);

    @Deprecated(forRemoval = false)
    void copyFrom(IPlayerSkinData other);
}
