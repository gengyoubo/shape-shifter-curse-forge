package net.onixary.shapeShifterCurseForge.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;

public final class PlayerSkinData implements IPlayerSkinData, INBTSerializable<CompoundTag> {
    private boolean keepOriginalSkin;
    private boolean enableFormColor;
    private FormTextureUtils.ColorSetting formColor = new FormTextureUtils.ColorSetting(
            0x00FFFFFF, 0x00FFFFFF, 0x00FFFFFF, 0x00000000, 0x00000000, false, false, false);
    private boolean enableFormRandomSound = true;

    @Override
    public boolean isKeepOriginalSkin() {
        return keepOriginalSkin;
    }

    @Override
    public void setKeepOriginalSkin(boolean keepOriginalSkin) {
        this.keepOriginalSkin = keepOriginalSkin;
    }

    @Override
    public boolean isEnableFormColor() {
        return enableFormColor;
    }

    @Override
    public void setEnableFormColor(boolean enableFormColor) {
        this.enableFormColor = enableFormColor;
    }

    @Override
    public FormTextureUtils.ColorSetting getFormColor() {
        return formColor;
    }

    @Override
    public void setFormColor(FormTextureUtils.ColorSetting formColor) {
        if (formColor != null) {
            this.formColor = formColor;
        }
    }

    @Override
    public boolean isEnableFormRandomSound() {
        return enableFormRandomSound;
    }

    @Override
    public void setEnableFormRandomSound(boolean enableFormRandomSound) {
        this.enableFormRandomSound = enableFormRandomSound;
    }

    @Override
    public void copyFrom(IPlayerSkinData other) {
        this.keepOriginalSkin = other.isKeepOriginalSkin();
        this.enableFormColor = other.isEnableFormColor();
        this.formColor = other.getFormColor();
        this.enableFormRandomSound = other.isEnableFormRandomSound();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("KeepOriginalSkin", keepOriginalSkin);
        tag.putBoolean("EnableFormColor", enableFormColor);
        tag.putInt("PrimaryColor", formColor.primaryColor());
        tag.putInt("AccentColor1", formColor.accentColor1());
        tag.putInt("AccentColor2", formColor.accentColor2());
        tag.putInt("EyeColorA", formColor.eyeColorA());
        tag.putInt("EyeColorB", formColor.eyeColorB());
        tag.putBoolean("PrimaryGreyReverse", formColor.primaryGreyReverse());
        tag.putBoolean("Accent1GreyReverse", formColor.accent1GreyReverse());
        tag.putBoolean("Accent2GreyReverse", formColor.accent2GreyReverse());
        tag.putBoolean("EnableFormRandomSound", enableFormRandomSound);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        keepOriginalSkin = tag.getBoolean("KeepOriginalSkin");
        enableFormColor = tag.getBoolean("EnableFormColor");
        formColor = new FormTextureUtils.ColorSetting(
                tag.getInt("PrimaryColor"), tag.getInt("AccentColor1"), tag.getInt("AccentColor2"),
                tag.getInt("EyeColorA"), tag.getInt("EyeColorB"),
                tag.getBoolean("PrimaryGreyReverse"), tag.getBoolean("Accent1GreyReverse"),
                tag.getBoolean("Accent2GreyReverse"));
        enableFormRandomSound = !tag.contains("EnableFormRandomSound") || tag.getBoolean("EnableFormRandomSound");
    }
}
