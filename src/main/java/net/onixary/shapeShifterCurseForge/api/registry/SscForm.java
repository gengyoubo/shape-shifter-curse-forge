package net.onixary.shapeShifterCurseForge.api.registry;

import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Java-defined SSC form descriptor.
 *
 * <p>A descriptor may inherit any omitted physical and progression property from another form.
 * Flags are inherited, then {@link Builder#addFlags(String...)} and
 * {@link Builder#removeFlags(String...)} are applied. A root form defaults to the NORMAL body,
 * tier/weight 1, scale 1, and a {@code <path>_form} group when those values are omitted.</p>
 */
public class SscForm {
    private final ResourceLocation id;
    private final ResourceLocation parentId;
    private final ResourceLocation groupId;
    private final Integer tier;
    private final Integer weight;
    private final FormBodyType bodyType;
    private final Float widthScale;
    private final Float heightScale;
    private final Float eyeScale;
    private final Float fallProtectionDistance;
    private final Float jumpVelocityAddition;
    private final Set<String> addedFlags;
    private final Set<String> removedFlags;

    /**
     * Creates a Java form from its properties.
     *
     * <p>Protected so extensions can expose a concrete form class, in the same style as a
     * custom {@code Item}: {@code final class MyForm extends SscForm}.</p>
     */
    protected SscForm(Builder builder) {
        this.id = builder.id;
        this.parentId = builder.parentId;
        this.groupId = builder.groupId;
        this.tier = builder.tier;
        this.weight = builder.weight;
        this.bodyType = builder.bodyType;
        this.widthScale = builder.widthScale;
        this.heightScale = builder.heightScale;
        this.eyeScale = builder.eyeScale;
        this.fallProtectionDistance = builder.fallProtectionDistance;
        this.jumpVelocityAddition = builder.jumpVelocityAddition;
        this.addedFlags = Set.copyOf(builder.addedFlags);
        this.removedFlags = Set.copyOf(builder.removedFlags);
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public ResourceLocation id() {
        return id;
    }

    public ResourceLocation parentId() {
        return parentId;
    }

    /** Resolves this descriptor against its optional parent definition. */
    public FormDefinition resolve(FormDefinition parent) {
        if (parentId != null && parent == null) {
            throw new IllegalStateException("SSC Java form '" + id + "' inherits missing form '" + parentId + "'");
        }
        ResourceLocation resolvedGroup = groupId != null ? groupId
                : parent != null ? parent.groupId()
                : ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_form");
        int resolvedTier = tier != null ? tier : parent != null ? parent.tier() : 1;
        int resolvedWeight = weight != null ? weight : parent != null ? parent.weight() : 1;
        FormBodyType resolvedBody = bodyType != null ? bodyType : parent != null ? parent.bodyType() : FormBodyType.NORMAL;
        float resolvedWidth = widthScale != null ? widthScale : parent != null ? parent.widthScale() : 1.0F;
        float resolvedHeight = heightScale != null ? heightScale : parent != null ? parent.heightScale() : 1.0F;
        float resolvedEye = eyeScale != null ? eyeScale : parent != null ? parent.eyeScale() : 1.0F;
        float resolvedFallProtection = fallProtectionDistance != null ? fallProtectionDistance
                : parent != null ? parent.fallProtectionDistance() : 0.0F;
        float resolvedJumpAddition = jumpVelocityAddition != null ? jumpVelocityAddition
                : parent != null ? parent.jumpVelocityAddition() : 0.0F;
        Set<String> resolvedFlags = new LinkedHashSet<>();
        if (parent != null) {
            resolvedFlags.addAll(parent.flags());
        }
        resolvedFlags.addAll(addedFlags);
        resolvedFlags.removeAll(removedFlags);
        return new FormDefinition(id, resolvedGroup, resolvedTier, resolvedWeight, resolvedBody,
                resolvedWidth, resolvedHeight, resolvedEye, Set.copyOf(resolvedFlags),
                resolvedFallProtection, resolvedJumpAddition);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private ResourceLocation parentId;
        private ResourceLocation groupId;
        private Integer tier;
        private Integer weight;
        private FormBodyType bodyType;
        private Float widthScale;
        private Float heightScale;
        private Float eyeScale;
        private Float fallProtectionDistance;
        private Float jumpVelocityAddition;
        private final Set<String> addedFlags = new LinkedHashSet<>();
        private final Set<String> removedFlags = new LinkedHashSet<>();

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder inherits(ResourceLocation parentId) {
            this.parentId = Objects.requireNonNull(parentId, "parentId");
            return this;
        }

        public Builder group(ResourceLocation groupId) {
            this.groupId = Objects.requireNonNull(groupId, "groupId");
            return this;
        }

        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }

        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public Builder bodyType(FormBodyType bodyType) {
            this.bodyType = Objects.requireNonNull(bodyType, "bodyType");
            return this;
        }

        public Builder widthScale(float widthScale) {
            this.widthScale = widthScale;
            return this;
        }

        public Builder heightScale(float heightScale) {
            this.heightScale = heightScale;
            return this;
        }

        public Builder eyeScale(float eyeScale) {
            this.eyeScale = eyeScale;
            return this;
        }

        public Builder fallProtectionDistance(float distance) {
            this.fallProtectionDistance = distance;
            return this;
        }

        public Builder jumpVelocityAddition(float addition) {
            this.jumpVelocityAddition = addition;
            return this;
        }

        public Builder addFlags(String... flags) {
            addAll(addedFlags, flags);
            return this;
        }

        public Builder removeFlags(String... flags) {
            addAll(removedFlags, flags);
            return this;
        }

        public SscForm build() {
            if (parentId != null && parentId.equals(id)) {
                throw new IllegalArgumentException("SSC Java form '" + id + "' cannot inherit itself");
            }
            return new SscForm(this);
        }

        private static void addAll(Set<String> target, String... flags) {
            if (flags == null) return;
            for (String flag : flags) {
                if (flag == null || flag.isBlank()) {
                    throw new IllegalArgumentException("SSC form flags must not be blank");
                }
                target.add(flag);
            }
        }
    }
}
