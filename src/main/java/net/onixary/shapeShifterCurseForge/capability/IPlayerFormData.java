package net.onixary.shapeShifterCurseForge.capability;

import net.minecraft.nbt.CompoundTag;

public interface IPlayerFormData extends net.onixary.shapeShifterCurseForge.api.PlayerFormData {
    @Deprecated(forRemoval = false)
    String getFormId();

    @Deprecated(forRemoval = false)
    void setFormId(String formId);

    @Deprecated(forRemoval = false)
    String getPreviousFormId();

    @Deprecated(forRemoval = false)
    void setPreviousFormId(String formId);

    @Deprecated(forRemoval = false)
    String getFormGroupId();

    @Deprecated(forRemoval = false)
    void setFormGroupId(String formGroupId);

    @Deprecated(forRemoval = false)
    int getFormTier();

    @Deprecated(forRemoval = false)
    void setFormTier(int formTier);

    @Deprecated(forRemoval = false)
    boolean isContentEnabled();

    @Deprecated(forRemoval = false)
    void setContentEnabled(boolean contentEnabled);

    @Deprecated(forRemoval = false)
    float getInstinctValue();

    @Deprecated(forRemoval = false)
    void setInstinctValue(float instinctValue);

    @Deprecated(forRemoval = false)
    float getInstinctRate();

    @Deprecated(forRemoval = false)
    void setInstinctRate(float instinctRate);

    @Deprecated(forRemoval = false)
    CompoundTag getInstinctEffects();

    @Deprecated(forRemoval = false)
    void setInstinctEffects(CompoundTag effects);

    @Deprecated(forRemoval = false)
    boolean isCursedMoonApplied();

    @Deprecated(forRemoval = false)
    void setCursedMoonApplied(boolean applied);

    @Deprecated(forRemoval = false)
    boolean wasLastTransformByCure();

    @Deprecated(forRemoval = false)
    void setLastTransformByCure(boolean cured);

    @Deprecated(forRemoval = false)
    String getBeforeCursedMoonAppliedForm();

    @Deprecated(forRemoval = false)
    void setBeforeCursedMoonAppliedForm(String formId);

    @Deprecated(forRemoval = false)
    String getAfterCursedMoonAppliedForm();

    @Deprecated(forRemoval = false)
    void setAfterCursedMoonAppliedForm(String formId);

    @Deprecated(forRemoval = false)
    String getTransformativeEffectFormId();

    @Deprecated(forRemoval = false)
    void setTransformativeEffectFormId(String formId);

    @Deprecated(forRemoval = false)
    int getTransformativeEffectTicks();

    @Deprecated(forRemoval = false)
    void setTransformativeEffectTicks(int ticks);

    @Deprecated(forRemoval = false)
    void copyFrom(IPlayerFormData other);

    @Override
    default void copyFrom(net.onixary.shapeShifterCurseForge.api.PlayerFormData other) {
        if (!(other instanceof IPlayerFormData legacy)) {
            throw new IllegalArgumentException("Cannot copy form data from an incompatible implementation");
        }
        copyFrom(legacy);
    }

    @Deprecated(forRemoval = false)
    CompoundTag serializeNBT();

    @Deprecated(forRemoval = false)
    void deserializeNBT(CompoundTag tag);
}
