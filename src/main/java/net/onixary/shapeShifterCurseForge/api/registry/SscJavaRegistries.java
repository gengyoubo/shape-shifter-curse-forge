package net.onixary.shapeShifterCurseForge.api.registry;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;

import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stable Java registration API for SSC extensions.
 *
 * <p>Register from the add-on mod constructor (before common setup), just like Forge registry
 * entries. IDs are global and duplicate registrations throw immediately. The class intentionally
 * has no Forge-only registration event, so the same API shape can be retained by the future
 * Fabric implementation.</p>
 *
 * <p>Registration is deliberately explicit: SSC does not scan static fields, subclasses, or
 * annotations. Register every form with {@link #registerForm(SscForm)}, then explicitly register
 * its owning family's {@link Evolution} with {@link #registerEvolution(Evolution)}. This keeps
 * load order deterministic and lets invalid cross-mod references fail with a useful error.</p>
 *
 * <pre>{@code
 * SscJavaRegistries.registerCondition(id("is_raining"),
 *     (player, target, json) -> player.level().isRaining());
 * SscJavaRegistries.registerAction(id("jump"),
 *     (player, target, json) -> target.push(0.0, 0.5, 0.0));
 * SscJavaRegistries.registerPower(id("aura"), player -> { ... });
 * SscJavaRegistries.attachPower(id("my_form"), id("aura"));
 * }</pre>
 */
public final class SscJavaRegistries {
    private static final Map<ResourceLocation, SscCondition> CONDITIONS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, SscAction> ACTIONS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, SscPower> POWERS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, Set<ResourceLocation>> FORM_POWERS = new ConcurrentHashMap<>();

    private SscJavaRegistries() {
    }

    public static void registerCondition(ResourceLocation id, SscCondition condition) {
        register(CONDITIONS, id, condition, "condition");
    }

    public static void registerAction(ResourceLocation id, SscAction action) {
        register(ACTIONS, id, action, "action");
    }

    public static void registerPower(ResourceLocation id, SscPower power) {
        register(POWERS, id, power, "power");
    }

    /**
     * Registers one Java form. Omitted properties inherit only from
     * {@link SscForm#inheritanceParentId()}; stage progression is intentionally not inferred
     * from that inheritance relationship and belongs in {@link Evolution}.
     *
     * <p>{@link SscForm#powers(PowerRegistrar)} is evaluated once during this call. Register
     * dynamic behaviour as an {@link SscPower}; do not expect this declaration hook to run per
     * player or per tick.</p>
     */
    public static void registerForm(SscForm form) {
        SscForm checked = Objects.requireNonNull(form, "form");
        Set<ResourceLocation> declaredPowers = checked.declaredPowers();
        FormRegistry.registerJavaForm(checked);
        declaredPowers.forEach(powerId -> attachPowerReference(checked.id(), powerId));
    }

    /**
     * Registers the complete evolution graph owned by one form family. The referenced forms may
     * be registered before or after this call, but all must exist before SSC first resolves forms.
     */
    public static void registerEvolution(Evolution evolution) {
        FormRegistry.registerJavaEvolution(Objects.requireNonNull(evolution, "evolution"));
    }

    /**
     * @deprecated Declare all form progression, including variants, through one family-owned
     * {@link Evolution}; Stage classes must not own progression edges themselves.
     */
    @Deprecated(forRemoval = false)
    public static void registerVariant(SscForm variant) {
        SscForm checked = Objects.requireNonNull(variant, "variant");
        if (checked.variantParentId() == null) {
            throw new IllegalArgumentException("SSC variant '" + checked.id() + "' must declare variantOf(...)");
        }
        registerForm(checked);
    }

    /** Attaches an already registered Java power to a form id. The form may load later from data. */
    public static void attachPower(ResourceLocation formId, ResourceLocation powerId) {
        Objects.requireNonNull(formId, "formId");
        Objects.requireNonNull(powerId, "powerId");
        if (!POWERS.containsKey(powerId)) {
            throw new IllegalArgumentException("Cannot attach unknown SSC Java power '" + powerId + "'");
        }
        attachPowerReference(formId, powerId);
    }

    /**
     * Attaches a data-defined or Java power id to a Java form. Unlike {@link #attachPower}, this
     * method intentionally permits data-defined power ids which are loaded later by /reload.
     */
    public static void attachPowerReference(ResourceLocation formId, ResourceLocation powerId) {
        Objects.requireNonNull(formId, "formId");
        Objects.requireNonNull(powerId, "powerId");
        FORM_POWERS.computeIfAbsent(formId, ignored -> ConcurrentHashMap.newKeySet()).add(powerId);
    }

    public static Optional<SscCondition> condition(ResourceLocation id) {
        return Optional.ofNullable(CONDITIONS.get(id));
    }

    public static Optional<SscAction> action(ResourceLocation id) {
        return Optional.ofNullable(ACTIONS.get(id));
    }

    public static Optional<SscPower> power(ResourceLocation id) {
        return Optional.ofNullable(POWERS.get(id));
    }

    /** Immutable snapshot of Java powers attached to one form. */
    public static Set<ResourceLocation> powersForForm(ResourceLocation formId) {
        if (formId == null) {
            return Set.of();
        }
        Set<ResourceLocation> result = new LinkedHashSet<>();
        for (ResourceLocation inheritedForm : FormRegistry.lineage(formId)) {
            Set<ResourceLocation> powers = FORM_POWERS.get(inheritedForm);
            if (powers != null) {
                result.addAll(powers);
            }
        }
        return Set.copyOf(result);
    }

    /** True only when this Java power is attached to the player's current form. */
    public static boolean isPowerActive(Player player, ResourceLocation powerId) {
        return player != null && powerId != null
                && powersForForm(FormManager.current(player).id()).contains(powerId);
    }

    /** Runs every Java power attached to the player's active form. Called by SSC's server tick. */
    public static void tickActivePowers(Player player) {
        for (ResourceLocation powerId : powersForForm(FormManager.current(player).id())) {
            SscPower power = POWERS.get(powerId);
            if (power != null) {
                power.tick(player);
            }
        }
    }

    /** Invokes a registered entity condition, returning {@code null} when the id is not Java-registered. */
    public static Boolean testCondition(ResourceLocation id, Player actor, Entity target, JsonObject data) {
        SscCondition condition = CONDITIONS.get(id);
        return condition == null ? null : condition.test(actor, target, data);
    }

    /** Invokes a registered entity action and reports whether an implementation was found. */
    public static boolean executeAction(ResourceLocation id, Player actor, LivingEntity target, JsonObject data) {
        SscAction action = ACTIONS.get(id);
        if (action == null) {
            return false;
        }
        action.execute(actor, target, data);
        return true;
    }

    private static <T> void register(Map<ResourceLocation, T> registry, ResourceLocation id, T entry, String kind) {
        Objects.requireNonNull(id, kind + " id");
        Objects.requireNonNull(entry, kind);
        if (registry.putIfAbsent(id, entry) != null) {
            throw new IllegalStateException("Duplicate SSC Java " + kind + " registration: '" + id + "'");
        }
    }
}
