package net.onixary.shapeShifterCurseForge.capability;

import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;

/** Server-authoritative per-player form skin (color) settings. Colors are ABGR. */
public interface IPlayerSkinData {
    boolean isKeepOriginalSkin();

    void setKeepOriginalSkin(boolean keepOriginalSkin);

    boolean isEnableFormColor();

    void setEnableFormColor(boolean enableFormColor);

    FormTextureUtils.ColorSetting getFormColor();

    void setFormColor(FormTextureUtils.ColorSetting formColor);

    boolean isEnableFormRandomSound();

    void setEnableFormRandomSound(boolean enableFormRandomSound);

    void copyFrom(IPlayerSkinData other);
}
