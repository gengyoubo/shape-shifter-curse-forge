package net.onixary.shapeShifterCurseForge.capability;

import net.minecraft.nbt.CompoundTag;

public interface IPlayerFormData {
    String getFormId();

    void setFormId(String formId);

    String getPreviousFormId();

    void setPreviousFormId(String formId);

    String getFormGroupId();

    void setFormGroupId(String formGroupId);

    int getFormTier();

    void setFormTier(int formTier);

    boolean isContentEnabled();

    void setContentEnabled(boolean contentEnabled);

    float getInstinctValue();

    void setInstinctValue(float instinctValue);

    float getInstinctRate();

    void setInstinctRate(float instinctRate);

    CompoundTag getInstinctEffects();

    void setInstinctEffects(CompoundTag effects);

    void copyFrom(IPlayerFormData other);

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag tag);
}
