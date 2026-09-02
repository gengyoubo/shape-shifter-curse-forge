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

    private String formId = ORIGINAL_BEFORE_ENABLE_FORM;
    private String previousFormId = ORIGINAL_BEFORE_ENABLE_FORM;
    private String formGroupId = "shape-shifter-curse:base_form";
    private int formTier = -1;
    private boolean contentEnabled;

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

    @Override
    public void copyFrom(IPlayerFormData other) {
        setFormId(other.getFormId());
        setPreviousFormId(other.getPreviousFormId());
        setFormGroupId(other.getFormGroupId());
        setFormTier(other.getFormTier());
        setContentEnabled(other.isContentEnabled());
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(FORM_ID_KEY, formId);
        tag.putString(PREVIOUS_FORM_ID_KEY, previousFormId);
        tag.putString(FORM_GROUP_ID_KEY, formGroupId);
        tag.putInt(FORM_TIER_KEY, formTier);
        tag.putBoolean(CONTENT_ENABLED_KEY, contentEnabled);
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
    }
}
