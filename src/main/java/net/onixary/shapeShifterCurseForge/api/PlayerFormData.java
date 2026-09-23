package net.onixary.shapeShifterCurseForge.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.Map;

/**
 * Stable public access contract for one player's mutable form runtime state.
 *
 * <p>The current implementation is backed by a Forge Capability, but callers
 * must not depend on that storage mechanism. This is deliberately separate from
 * {@code SscForm}, {@code FormDefinition}, and {@code Evolution}: those classes are immutable
 * shared definitions and must never contain a player's instinct, transformation progress,
 * cooldown, or other mutable state.</p>
 */
public interface PlayerFormData {
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

    boolean isCursedMoonApplied();

    void setCursedMoonApplied(boolean applied);

    boolean wasLastTransformByCure();

    void setLastTransformByCure(boolean cured);

    String getBeforeCursedMoonAppliedForm();

    void setBeforeCursedMoonAppliedForm(String formId);

    String getAfterCursedMoonAppliedForm();

    void setAfterCursedMoonAppliedForm(String formId);

    String getTransformativeEffectFormId();

    void setTransformativeEffectFormId(String formId);

    int getTransformativeEffectTicks();

    void setTransformativeEffectTicks(int ticks);

    /** Immutable ids of all perks this player has purchased, independent of their current form. */
    Set<ResourceLocation> getUnlockedPerks();

    boolean hasUnlockedPerk(ResourceLocation perkId);

    /** Server-side only; UI clients must request purchases through the server. */
    void unlockPerk(ResourceLocation perkId);

    /** Server-side administrative/data-repair operation. */
    void revokePerk(ResourceLocation perkId);

    /** Persistent mana pools keyed by the data-defined mana type. */
    Map<String, Float> getManaPools();

    void setManaPool(String manaType, float amount);

    void copyFrom(PlayerFormData other);

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag tag);
}
