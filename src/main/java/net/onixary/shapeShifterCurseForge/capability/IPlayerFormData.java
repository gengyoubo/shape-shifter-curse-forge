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

    void copyFrom(IPlayerFormData other);

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag tag);
}
