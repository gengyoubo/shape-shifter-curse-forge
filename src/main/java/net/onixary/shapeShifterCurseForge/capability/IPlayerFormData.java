package net.onixary.shapeShifterCurseForge.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.api.SscApi;

public interface IPlayerFormData extends net.onixary.shapeShifterCurseForge.api.PlayerFormData {
    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    String getFormId();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setFormId(String formId);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    String getPreviousFormId();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setPreviousFormId(String formId);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    String getFormGroupId();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setFormGroupId(String formGroupId);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    int getFormTier();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setFormTier(int formTier);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    boolean isContentEnabled();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setContentEnabled(boolean contentEnabled);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    float getInstinctValue();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setInstinctValue(float instinctValue);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    float getInstinctRate();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setInstinctRate(float instinctRate);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    CompoundTag getInstinctEffects();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setInstinctEffects(CompoundTag effects);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    boolean isCursedMoonApplied();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setCursedMoonApplied(boolean applied);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    boolean wasLastTransformByCure();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setLastTransformByCure(boolean cured);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    String getBeforeCursedMoonAppliedForm();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setBeforeCursedMoonAppliedForm(String formId);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    String getAfterCursedMoonAppliedForm();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setAfterCursedMoonAppliedForm(String formId);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    String getTransformativeEffectFormId();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setTransformativeEffectFormId(String formId);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    int getTransformativeEffectTicks();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void setTransformativeEffectTicks(int ticks);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void copyFrom(IPlayerFormData other);

    @Override
    default void copyFrom(net.onixary.shapeShifterCurseForge.api.PlayerFormData other) {
        if (!(other instanceof IPlayerFormData legacy)) {
            throw new IllegalArgumentException("Cannot copy form data from an incompatible implementation");
        }
        copyFrom(legacy);
    }

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    CompoundTag serializeNBT();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated(forRemoval = false)
    void deserializeNBT(CompoundTag tag);
}
