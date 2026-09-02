package net.onixary.shapeShifterCurseForge.capability;

import net.minecraft.nbt.CompoundTag;

public interface IPlayerFormData {
    String getFormId();

    void setFormId(String formId);

    void copyFrom(IPlayerFormData other);

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag tag);
}
