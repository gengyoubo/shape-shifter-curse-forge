package net.onixary.shapeShifterCurseForge.api.registry;

import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Java-defined SSC form descriptor, usable with either a builder or an inheritable class.
 *
 * <p>This distinction is intentional: {@link #builder(ResourceLocation)} creates a plain final
 * descriptor and never invokes subclass hooks. A subclass must call
 * {@link #SscForm(ResourceLocation)}, which is the only subclass-accessible constructor; then
 * {@link #stage()}, {@link #configure(FormProperties)}, and {@link #powers(PowerRegistrar)} are
 * evaluated by SSC while the registry resolves. Progression between those forms belongs in the
 * owning family's {@link Evolution}, not in the Stage class.</p>
 */
public class SscForm {
    /** A class-based form with no stage override is a stage-four form. */
    public static final int DEFAULT_STAGE = 4;

    /** Exact form id for builder forms. Class forms derive theirs from {@link #familyId}. */
    private final ResourceLocation id;
    /** Namespace and internal family id supplied by a class form. */
    private final ResourceLocation familyId;
    private final ResourceLocation inheritanceParentId;
    private final ResourceLocation variantParentId;
    private final ResourceLocation groupId;
    private final Integer configuredStage;
    private final Integer configuredMaximumStage;
    private final Integer weight;
    private final FormBodyType bodyType;
    private final Float widthScale, heightScale, eyeScale, configuredFallProtectionDistance, jumpVelocityAddition;
    private final Boolean fullyCustomModel;
    private final Set<String> addedFlags, removedFlags;
    private final boolean classHooks;

    /** Creates a builder-defined form. Only {@link Builder#build()} can create this form kind. */
    private SscForm(Builder builder) {
        this(builder, false, null);
    }

    /**
     * Creates a class-defined stage from its family internal id. A stage-one class using
     * {@code super(id("catgirl"))} therefore registers {@code <namespace>:catgirl_0} and defaults
     * to group {@code <namespace>:catgirl_form}; subsequent stages receive suffixes 1, 2, and so
     * on. Named branches must use {@link #branch(ResourceLocation, String)} instead.
     */
    protected SscForm(ResourceLocation familyId) {
        this(new Builder(Objects.requireNonNull(familyId, "familyId")), true, familyId);
    }

    private SscForm(Builder builder, boolean classHooks, ResourceLocation familyId) {
        id = builder.id;
        this.familyId = familyId;
        inheritanceParentId = builder.inheritanceParentId;
        variantParentId = builder.variantParentId;
        groupId = builder.groupId;
        configuredStage = builder.stage;
        configuredMaximumStage = builder.maximumStage;
        weight = builder.weight;
        bodyType = builder.bodyType;
        widthScale = builder.widthScale;
        heightScale = builder.heightScale;
        eyeScale = builder.eyeScale;
        configuredFallProtectionDistance = builder.fallProtectionDistance;
        jumpVelocityAddition = builder.jumpVelocityAddition;
        fullyCustomModel = builder.fullyCustomModel;
        addedFlags = Set.copyOf(builder.addedFlags);
        removedFlags = Set.copyOf(builder.removedFlags);
        this.classHooks = classHooks;
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    /**
     * Creates a named branch form in {@code familyId}'s normal group. Unlike regular stages, a
     * branch must be named explicitly and receives id {@code <family>_<branchName>}.
     */
    public static Builder branch(ResourceLocation familyId, String branchName) {
        Objects.requireNonNull(familyId, "familyId");
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("SSC form branch name must not be blank");
        }
        ResourceLocation branchId = ResourceLocation.fromNamespaceAndPath(familyId.getNamespace(),
                familyId.getPath() + "_" + branchName);
        return new Builder(branchId).group(groupId(familyId));
    }

    /** Returns the generated id for a regular stage; useful when declaring an {@link Evolution}. */
    public static ResourceLocation stageId(ResourceLocation familyId, int stage) {
        Objects.requireNonNull(familyId, "familyId");
        if (stage < 1) throw new IllegalArgumentException("SSC form stage must be at least 1");
        return ResourceLocation.fromNamespaceAndPath(familyId.getNamespace(), familyId.getPath() + "_" + (stage - 1));
    }

    /** @deprecated Put the route in {@link Evolution} and register it explicitly. */
    @Deprecated(forRemoval = false)
    public static Builder variantBuilder(ResourceLocation id, ResourceLocation branchTip) {
        return builder(id).variantOf(branchTip);
    }

    public ResourceLocation id() {
        return classHooks ? stageId(familyId, stage()) : id;
    }

    /** Explicit inheritance parent, or the preceding branch form for a variant. */
    public ResourceLocation inheritanceParentId() { return inheritanceParentId; }

    /** The preceding form in this branch; {@code null} means this is a regular form. */
    public ResourceLocation variantParentId() { return variantParentId; }

    /** Override in class-based forms. The default is the final stage, four. */
    protected int stage() { return DEFAULT_STAGE; }

    /** Override on a shared family base to cap every variant branch in that family. */
    protected int maximumStage() { return configuredMaximumStage != null ? configuredMaximumStage : DEFAULT_STAGE; }

    /** The declared branch cap, exposed for SSC's progression validator. */
    public final int maximumStageLimit() { return maximumStage(); }

    /**
     * Override for concise property changes. This runs while definitions are resolved, not per
     * player; use {@link SscPower} for gameplay that needs to react at runtime.
     */
    protected void configure(FormProperties properties) { }

    /** Adds data-defined or Java power ids. Subclasses should call {@code super.powers(powers)}. */
    protected void powers(PowerRegistrar powers) { }

    final Set<ResourceLocation> declaredPowers() {
        PowerRegistrar registrar = new PowerRegistrar();
        powers(registrar);
        return registrar.ids();
    }

    /** Resolves builder data, then class hooks, against the optional inheritance parent. */
    public FormDefinition resolve(FormDefinition parent) {
        ResourceLocation inheritanceParent = inheritanceParentId();
        if (inheritanceParent != null && parent == null) {
            throw new IllegalStateException("SSC Java form '" + id() + "' inherits missing form '" + inheritanceParent + "'");
        }
        ResourceLocation resolvedId = id();
        ResourceLocation resolvedGroup = groupId != null ? groupId : parent != null ? parent.groupId()
                : classHooks ? groupId(familyId)
                : ResourceLocation.fromNamespaceAndPath(resolvedId.getNamespace(), resolvedId.getPath() + "_form");
        int resolvedStage = configuredStage != null ? configuredStage
                : classHooks ? stage() : parent != null ? parent.stage() : 1;
        int resolvedWeight = weight != null ? weight : parent != null ? parent.weight() : 1;
        FormBodyType resolvedBody = bodyType != null ? bodyType : parent != null ? parent.bodyType() : FormBodyType.NORMAL;
        float resolvedWidth = widthScale != null ? widthScale : parent != null ? parent.widthScale() : 1.0F;
        float resolvedHeight = heightScale != null ? heightScale : parent != null ? parent.heightScale() : 1.0F;
        float resolvedEye = eyeScale != null ? eyeScale : parent != null ? parent.eyeScale() : 1.0F;
        float resolvedFall = configuredFallProtectionDistance != null ? configuredFallProtectionDistance
                : parent != null ? parent.fallProtectionDistance() : 0.0F;
        float resolvedJump = jumpVelocityAddition != null ? jumpVelocityAddition : parent != null ? parent.jumpVelocityAddition() : 0.0F;
        boolean resolvedCustom = fullyCustomModel != null ? fullyCustomModel : parent != null && parent.fullyCustomModel();
        Set<String> resolvedFlags = new LinkedHashSet<>();
        if (parent != null) resolvedFlags.addAll(parent.flags());
        resolvedFlags.addAll(addedFlags);
        resolvedFlags.removeAll(removedFlags);

        FormProperties properties = new FormProperties(resolvedGroup, resolvedStage, resolvedWeight, resolvedBody,
                resolvedWidth, resolvedHeight, resolvedEye, resolvedFall, resolvedJump, resolvedCustom, resolvedFlags);
        if (classHooks) configure(properties);
        return properties.toDefinition(resolvedId);
    }

    public void validateStage(FormDefinition definition) {
        int maximum = maximumStage();
        if (maximum < 1 || definition.stage() < 1 || definition.stage() > maximum) {
            throw new IllegalStateException("SSC Java form '" + id() + "' resolves to stage " + definition.stage()
                    + ", outside its allowed 1-" + maximum + " range");
        }
    }

    private static ResourceLocation groupId(ResourceLocation familyId) {
        return ResourceLocation.fromNamespaceAndPath(familyId.getNamespace(), familyId.getPath() + "_form");
    }

    public static final class Builder {
        private final ResourceLocation id;
        private ResourceLocation inheritanceParentId, variantParentId, groupId;
        private Integer stage, maximumStage, weight;
        private FormBodyType bodyType;
        private Float widthScale, heightScale, eyeScale, fallProtectionDistance, jumpVelocityAddition;
        private Boolean fullyCustomModel;
        private final Set<String> addedFlags = new LinkedHashSet<>();
        private final Set<String> removedFlags = new LinkedHashSet<>();

        private Builder(ResourceLocation id) { this.id = Objects.requireNonNull(id, "id"); }

        public Builder inherits(ResourceLocation parentId) {
            inheritanceParentId = Objects.requireNonNull(parentId, "parentId");
            return this;
        }

        /**
         * @deprecated Put branch ownership in {@link Evolution}. This legacy method also makes
         * the branch point the inheritance parent when {@link #inherits(ResourceLocation)} was
         * not explicitly called.
         */
        @Deprecated(forRemoval = false)
        public Builder variantOf(ResourceLocation parentId) {
            ResourceLocation checked = Objects.requireNonNull(parentId, "parentId");
            variantParentId = checked;
            if (inheritanceParentId == null) inheritanceParentId = checked;
            return this;
        }

        public Builder group(ResourceLocation groupId) { this.groupId = Objects.requireNonNull(groupId, "groupId"); return this; }
        /** Sets the form stage. Stages are numbered 1 through the family's maximum stage. */
        public Builder stage(int stage) { this.stage = stage; return this; }

        /** @deprecated Use {@link #stage(int)}. Tier is retained only for source compatibility. */
        @Deprecated(forRemoval = false)
        public Builder tier(int tier) { return stage(tier); }
        public Builder maximumStage(int maximumStage) { this.maximumStage = maximumStage; return this; }
        public Builder weight(int weight) { this.weight = weight; return this; }
        public Builder bodyType(FormBodyType bodyType) { this.bodyType = Objects.requireNonNull(bodyType, "bodyType"); return this; }
        public Builder widthScale(float widthScale) { this.widthScale = widthScale; return this; }
        public Builder heightScale(float heightScale) { this.heightScale = heightScale; return this; }
        public Builder eyeScale(float eyeScale) { this.eyeScale = eyeScale; return this; }
        public Builder fallProtectionDistance(float distance) { fallProtectionDistance = distance; return this; }
        public Builder jumpVelocityAddition(float addition) { jumpVelocityAddition = addition; return this; }
        public Builder fullyCustomModel(boolean fullyCustomModel) { this.fullyCustomModel = fullyCustomModel; return this; }
        public Builder addFlags(String... flags) { addAll(addedFlags, flags); return this; }
        public Builder removeFlags(String... flags) { addAll(removedFlags, flags); return this; }

        public SscForm build() {
            if (id.equals(inheritanceParentId) || id.equals(variantParentId)) {
                throw new IllegalArgumentException("SSC Java form '" + id + "' cannot inherit or branch from itself");
            }
            return new SscForm(this);
        }

        private static void addAll(Set<String> target, String... flags) {
            if (flags == null) return;
            for (String flag : flags) {
                if (flag == null || flag.isBlank()) throw new IllegalArgumentException("SSC form flags must not be blank");
                target.add(flag);
            }
        }
    }
}
