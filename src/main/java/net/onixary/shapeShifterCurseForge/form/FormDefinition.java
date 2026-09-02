package net.onixary.shapeShifterCurseForge.form;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public record FormDefinition(
        ResourceLocation id,
        ResourceLocation groupId,
        int tier,
        int weight,
        FormBodyType bodyType,
        float widthScale,
        float heightScale,
        float eyeScale,
        Set<String> flags,
        float fallProtectionDistance,
        float jumpVelocityAddition
) {
    public FormDefinition(ResourceLocation id, ResourceLocation groupId, int tier, int weight,
                           FormBodyType bodyType, float widthScale, float heightScale, float eyeScale,
                           Set<String> flags) {
        this(id, groupId, tier, weight, bodyType, widthScale, heightScale, eyeScale, flags, 0.0F, 0.0F);
    }

    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }
}
