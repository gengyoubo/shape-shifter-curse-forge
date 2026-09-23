package net.onixary.shapeShifterCurseForge.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.api.SscApi;

public interface IPlayerFormData extends net.onixary.shapeShifterCurseForge.api.PlayerFormData {
    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    String getFormId();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void setFormId(String formId);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    String getPreviousFormId();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void setPreviousFormId(String formId);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    String getFormGroupId();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void setFormGroupId(String formGroupId);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    int getFormTier();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void setFormTier(int formTier);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    boolean isContentEnabled();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void setContentEnabled(boolean contentEnabled);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    float getInstinctValue();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void setInstinctValue(float instinctValue);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    float getInstinctRate();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void setInstinctRate(float instinctRate);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    CompoundTag getInstinctEffects();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void setInstinctEffects(CompoundTag effects);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    boolean isCursedMoonApplied();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void setCursedMoonApplied(boolean applied);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    boolean wasLastTransformByCure();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void setLastTransformByCure(boolean cured);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    String getBeforeCursedMoonAppliedForm();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void setBeforeCursedMoonAppliedForm(String formId);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    String getAfterCursedMoonAppliedForm();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void setAfterCursedMoonAppliedForm(String formId);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    String getTransformativeEffectFormId();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void setTransformativeEffectFormId(String formId);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    int getTransformativeEffectTicks();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void setTransformativeEffectTicks(int ticks);

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void copyFrom(IPlayerFormData other);

    @Override
    default void copyFrom(net.onixary.shapeShifterCurseForge.api.PlayerFormData other) {
        if (!(other instanceof IPlayerFormData legacy)) {
            throw new IllegalArgumentException("Cannot copy form data from an incompatible implementation");
        }
        copyFrom(legacy);
    }

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    CompoundTag serializeNBT();

    /** @deprecated Use {@link SscApi#currentForm(Player)} instead. */
    @Deprecated()
    void deserializeNBT(CompoundTag tag);
}
