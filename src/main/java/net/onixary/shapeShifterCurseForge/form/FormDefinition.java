package net.onixary.shapeShifterCurseForge.form;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Immutable resolved form definition. {@link #stage()} is the public progression term.
 * Player-specific state belongs to {@code PlayerFormData}, never to this shared object.
 */
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
        float jumpVelocityAddition,
        boolean fullyCustomModel
) {
    /** Primary progression accessor. */
    public int stage() {
        return tier;
    }

    /** @deprecated Use {@link #stage()}; tier is the retained storage/compatibility term. */
    @Deprecated(forRemoval = false)
    public int tier() {
        return tier;
    }

    public FormDefinition(ResourceLocation id, ResourceLocation groupId, int tier, int weight,
                           FormBodyType bodyType, float widthScale, float heightScale, float eyeScale,
                           Set<String> flags) {
        this(id, groupId, tier, weight, bodyType, widthScale, heightScale, eyeScale, flags,
                0.0F, 0.0F, false);
    }

    public FormDefinition(ResourceLocation id, ResourceLocation groupId, int tier, int weight,
                           FormBodyType bodyType, float widthScale, float heightScale, float eyeScale,
                           Set<String> flags, float fallProtectionDistance, float jumpVelocityAddition) {
        this(id, groupId, tier, weight, bodyType, widthScale, heightScale, eyeScale, flags,
                fallProtectionDistance, jumpVelocityAddition, false);
    }

    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }
}
