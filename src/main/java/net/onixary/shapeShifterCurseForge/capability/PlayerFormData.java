package net.onixary.shapeShifterCurseForge.capability;

import net.minecraft.nbt.CompoundTag;

public final class PlayerFormData implements IPlayerFormData {
    public static final String ORIGINAL_BEFORE_ENABLE_FORM = "shape-shifter-curse:original_before_enable";
    public static final String ORIGINAL_SHIFTER_FORM = "shape-shifter-curse:original_shifter";
    private static final String FORM_ID_KEY = "FormId";
    private static final String PREVIOUS_FORM_ID_KEY = "PreviousFormId";
    private static final String FORM_GROUP_ID_KEY = "FormGroupId";
    private static final String FORM_TIER_KEY = "FormTier";
    private static final String CONTENT_ENABLED_KEY = "ContentEnabled";
    private static final String INSTINCT_VALUE_KEY = "InstinctValue";
    private static final String INSTINCT_RATE_KEY = "InstinctRate";
    private static final String INSTINCT_EFFECTS_KEY = "InstinctEffects";
    private static final String CURSED_MOON_APPLIED_KEY = "CursedMoonApplied";
    private static final String LAST_TRANSFORM_BY_CURE_KEY = "LastTransformByCure";
    private static final String BEFORE_CURSED_MOON_FORM_KEY = "BeforeCursedMoonAppliedForm";
    private static final String AFTER_CURSED_MOON_FORM_KEY = "AfterCursedMoonAppliedForm";

    private String formId = ORIGINAL_BEFORE_ENABLE_FORM;
    private String previousFormId = ORIGINAL_BEFORE_ENABLE_FORM;
    private String formGroupId = "shape-shifter-curse:base_form";
    private int formTier = -1;
    private boolean contentEnabled;
    private float instinctValue;
    private float instinctRate;
    private CompoundTag instinctEffects = new CompoundTag();
    private boolean cursedMoonApplied;
    private boolean lastTransformByCure;
    private String beforeCursedMoonAppliedForm;
    private String afterCursedMoonAppliedForm;

    @Override
    public String getFormId() {
        return formId;
    }

    @Override
    public void setFormId(String formId) {
        this.formId = formId == null || formId.isBlank() ? ORIGINAL_BEFORE_ENABLE_FORM : formId;
    }

    @Override
    public String getPreviousFormId() {
        return previousFormId;
    }

    @Override
    public void setPreviousFormId(String formId) {
        this.previousFormId = formId == null || formId.isBlank() ? ORIGINAL_BEFORE_ENABLE_FORM : formId;
    }

    @Override
    public String getFormGroupId() {
        return formGroupId;
    }

    public void setFormGroupId(String formGroupId) {
        this.formGroupId = formGroupId == null || formGroupId.isBlank()
                ? "shape-shifter-curse:base_form" : formGroupId;
    }

    @Override
    public int getFormTier() {
        return formTier;
    }

    public void setFormTier(int formTier) {
        this.formTier = formTier;
    }

    @Override
    public boolean isContentEnabled() {
        return contentEnabled;
    }

    public void setContentEnabled(boolean contentEnabled) {
        this.contentEnabled = contentEnabled;
    }

    @Override public float getInstinctValue() { return instinctValue; }

    @Override public void setInstinctValue(float instinctValue) { this.instinctValue = Math.max(0.0F, instinctValue); }

    @Override public float getInstinctRate() { return instinctRate; }

    @Override public void setInstinctRate(float instinctRate) { this.instinctRate = instinctRate; }

    @Override public CompoundTag getInstinctEffects() { return instinctEffects.copy(); }

    @Override public void setInstinctEffects(CompoundTag effects) { instinctEffects = effects == null ? new CompoundTag() : effects.copy(); }

    @Override public boolean isCursedMoonApplied() { return cursedMoonApplied; }

    @Override public void setCursedMoonApplied(boolean applied) { cursedMoonApplied = applied; }

    @Override public boolean wasLastTransformByCure() { return lastTransformByCure; }

    @Override public void setLastTransformByCure(boolean cured) { lastTransformByCure = cured; }

    @Override public String getBeforeCursedMoonAppliedForm() { return beforeCursedMoonAppliedForm; }

    @Override public void setBeforeCursedMoonAppliedForm(String formId) { beforeCursedMoonAppliedForm = formId; }

    @Override public String getAfterCursedMoonAppliedForm() { return afterCursedMoonAppliedForm; }

    @Override public void setAfterCursedMoonAppliedForm(String formId) { afterCursedMoonAppliedForm = formId; }

    @Override
    public void copyFrom(IPlayerFormData other) {
        setFormId(other.getFormId());
        setPreviousFormId(other.getPreviousFormId());
        setFormGroupId(other.getFormGroupId());
        setFormTier(other.getFormTier());
        setContentEnabled(other.isContentEnabled());
        setInstinctValue(other.getInstinctValue());
        setInstinctRate(other.getInstinctRate());
        setInstinctEffects(other.getInstinctEffects());
        setCursedMoonApplied(other.isCursedMoonApplied());
        setLastTransformByCure(other.wasLastTransformByCure());
        setBeforeCursedMoonAppliedForm(other.getBeforeCursedMoonAppliedForm());
        setAfterCursedMoonAppliedForm(other.getAfterCursedMoonAppliedForm());
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(FORM_ID_KEY, formId);
        tag.putString(PREVIOUS_FORM_ID_KEY, previousFormId);
        tag.putString(FORM_GROUP_ID_KEY, formGroupId);
        tag.putInt(FORM_TIER_KEY, formTier);
        tag.putBoolean(CONTENT_ENABLED_KEY, contentEnabled);
        tag.putFloat(INSTINCT_VALUE_KEY, instinctValue);
        tag.putFloat(INSTINCT_RATE_KEY, instinctRate);
        tag.put(INSTINCT_EFFECTS_KEY, instinctEffects.copy());
        tag.putBoolean(CURSED_MOON_APPLIED_KEY, cursedMoonApplied);
        tag.putBoolean(LAST_TRANSFORM_BY_CURE_KEY, lastTransformByCure);
        if (beforeCursedMoonAppliedForm != null) {
            tag.putString(BEFORE_CURSED_MOON_FORM_KEY, beforeCursedMoonAppliedForm);
        }
        if (afterCursedMoonAppliedForm != null) {
            tag.putString(AFTER_CURSED_MOON_FORM_KEY, afterCursedMoonAppliedForm);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains(FORM_ID_KEY)) {
            setFormId(tag.getString(FORM_ID_KEY));
        }
        if (tag.contains(PREVIOUS_FORM_ID_KEY)) {
            setPreviousFormId(tag.getString(PREVIOUS_FORM_ID_KEY));
        }
        if (tag.contains(FORM_GROUP_ID_KEY)) {
            setFormGroupId(tag.getString(FORM_GROUP_ID_KEY));
        }
        if (tag.contains(FORM_TIER_KEY)) {
            setFormTier(tag.getInt(FORM_TIER_KEY));
        }
        if (tag.contains(CONTENT_ENABLED_KEY)) {
            setContentEnabled(tag.getBoolean(CONTENT_ENABLED_KEY));
        }
        if (tag.contains(INSTINCT_VALUE_KEY)) setInstinctValue(tag.getFloat(INSTINCT_VALUE_KEY));
        if (tag.contains(INSTINCT_RATE_KEY)) setInstinctRate(tag.getFloat(INSTINCT_RATE_KEY));
        if (tag.contains(INSTINCT_EFFECTS_KEY)) setInstinctEffects(tag.getCompound(INSTINCT_EFFECTS_KEY));
        if (tag.contains(CURSED_MOON_APPLIED_KEY)) setCursedMoonApplied(tag.getBoolean(CURSED_MOON_APPLIED_KEY));
        if (tag.contains(LAST_TRANSFORM_BY_CURE_KEY)) setLastTransformByCure(tag.getBoolean(LAST_TRANSFORM_BY_CURE_KEY));
        beforeCursedMoonAppliedForm = tag.contains(BEFORE_CURSED_MOON_FORM_KEY)
                ? tag.getString(BEFORE_CURSED_MOON_FORM_KEY) : null;
        afterCursedMoonAppliedForm = tag.contains(AFTER_CURSED_MOON_FORM_KEY)
                ? tag.getString(AFTER_CURSED_MOON_FORM_KEY) : null;
    }
}
