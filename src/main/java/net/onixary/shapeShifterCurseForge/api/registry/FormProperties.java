package net.onixary.shapeShifterCurseForge.api.registry;

import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Mutable view of a form while a class-based {@link SscForm} is being resolved.
 *
 * <p>Use this only from {@link SscForm#configure(FormProperties)} for class-defined forms. The
 * object is transient and must not be stored: SSC discards it after resolving the immutable
 * {@link FormDefinition}. Builder forms intentionally have no configure hook.</p>
 */
public final class FormProperties {
    private ResourceLocation groupId;
    private int stage;
    private int weight;
    private FormBodyType bodyType;
    private float widthScale;
    private float heightScale;
    private float eyeScale;
    private float fallProtectionDistance;
    private float jumpVelocityAddition;
    private boolean fullyCustomModel;
    private final Set<String> flags;

    FormProperties(ResourceLocation groupId, int stage, int weight, FormBodyType bodyType,
                   float widthScale, float heightScale, float eyeScale, float fallProtectionDistance,
                   float jumpVelocityAddition, boolean fullyCustomModel, Set<String> flags) {
        this.groupId = Objects.requireNonNull(groupId, "groupId");
        this.stage = stage;
        this.weight = weight;
        this.bodyType = Objects.requireNonNull(bodyType, "bodyType");
        this.widthScale = widthScale;
        this.heightScale = heightScale;
        this.eyeScale = eyeScale;
        this.fallProtectionDistance = fallProtectionDistance;
        this.jumpVelocityAddition = jumpVelocityAddition;
        this.fullyCustomModel = fullyCustomModel;
        this.flags = new LinkedHashSet<>(flags);
    }

    public FormProperties group(ResourceLocation groupId) {
        this.groupId = Objects.requireNonNull(groupId, "groupId");
        return this;
    }

    public FormProperties stage(int stage) {
        if (stage < 1) throw new IllegalArgumentException("SSC form stage must be at least 1");
        this.stage = stage;
        return this;
    }

    public FormProperties weight(int weight) {
        if (weight < 1) throw new IllegalArgumentException("SSC form weight must be at least 1");
        this.weight = weight;
        return this;
    }

    public FormProperties bodyType(FormBodyType bodyType) {
        this.bodyType = Objects.requireNonNull(bodyType, "bodyType");
        return this;
    }

    public FormProperties scale(float width, float height, float eye) {
        this.widthScale = width;
        this.heightScale = height;
        this.eyeScale = eye;
        return this;
    }

    public FormProperties fallProtectionDistance(float distance) {
        this.fallProtectionDistance = distance;
        return this;
    }

    public FormProperties jumpVelocityAddition(float addition) {
        this.jumpVelocityAddition = addition;
        return this;
    }

    public FormProperties fullyCustomModel(boolean fullyCustomModel) {
        this.fullyCustomModel = fullyCustomModel;
        return this;
    }

    public FormProperties addFlags(String... values) {
        for (String value : values) addFlag(value);
        return this;
    }

    public FormProperties addFlag(String flag) {
        if (flag == null || flag.isBlank()) throw new IllegalArgumentException("SSC form flags must not be blank");
        flags.add(flag);
        return this;
    }

    public FormProperties removeFlag(String flag) {
        flags.remove(flag);
        return this;
    }

    /** Marks this as the final form in its branch. */
    public FormProperties finalForm() {
        return addFlags("final_form", "inhibitor_immune", "no_instinct", "no_cursed_moon_effect");
    }

    FormDefinition toDefinition(ResourceLocation id) {
        return new FormDefinition(id, groupId, stage, weight, bodyType, widthScale, heightScale, eyeScale,
                Set.copyOf(flags), fallProtectionDistance, jumpVelocityAddition, fullyCustomModel);
    }
}
