package net.onixary.shapeShifterCurseForge.capability;

import net.minecraft.nbt.CompoundTag;

public final class PlayerFormData implements IPlayerFormData {
    private static final String FORM_ID_KEY = "FormId";
    private String formId = "minecraft:player";

    @Override
    public String getFormId() {
        return formId;
    }

    @Override
    public void setFormId(String formId) {
        this.formId = formId == null || formId.isBlank() ? "minecraft:player" : formId;
    }

    @Override
    public void copyFrom(IPlayerFormData other) {
        setFormId(other.getFormId());
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(FORM_ID_KEY, formId);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains(FORM_ID_KEY)) {
            setFormId(tag.getString(FORM_ID_KEY));
        }
    }
}
