package net.onixary.shapeShifterCurseForge.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormGroup;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Public read-only power lookup for SSC add-ons.
 *
 * <p>Stages are one-based: {@code axolotl_0} is stage 1 and
 * {@code axolotl_3} is stage 4. A returned list includes powers retained
 * from earlier stages when the selected form still assigns them. Runtime
 * conditions are not evaluated, since they depend on a player and can change
 * every tick.</p>
 */
public final class SscPowerApi {
    private SscPowerApi() {
    }

    /** The player's exact form id, including its stage or branch suffix. */
    public static ResourceLocation currentFormId(Player player) {
        return FormManager.current(Objects.requireNonNull(player, "player")).id();
    }

    /**
     * Resolve a form or form-group id at a one-based stage. An exact form id
     * follows its own progression branch; a group id selects its default form.
     * For built-in groups, {@code shape-shifter-curse:axolotl} is accepted as
     * shorthand for {@code shape-shifter-curse:axolotl_form}.
     */
    public static Optional<ResourceLocation> formIdAtStage(ResourceLocation formOrGroupId, int stage) {
        Objects.requireNonNull(formOrGroupId, "formOrGroupId");
        FormDefinition form = FormRegistry.get(formOrGroupId);
        if (form != null) {
            FormDefinition selected = FormRegistry.formAtStageInProgression(form, stage);
            return Optional.ofNullable(selected).map(FormDefinition::id);
        }

        FormGroup group = FormRegistry.getGroup(formOrGroupId);
        if (group == null && !formOrGroupId.getPath().endsWith("_form")) {
            ResourceLocation groupId = ResourceLocation.fromNamespaceAndPath(
                    formOrGroupId.getNamespace(), formOrGroupId.getPath() + "_form");
            group = FormRegistry.getGroup(groupId);
        }
        return Optional.ofNullable(group == null ? null : group.firstAtStage(stage))
                .map(FormDefinition::id);
    }

    /**
     * Powers assigned to the selected form at {@code stage}, including carried
     * powers. Returns an empty list for an unknown form or stage. The result
     * excludes player-specific accessory changes.
     */
    public static List<ResourceLocation> powersFor(ResourceLocation formOrGroupId, int stage) {
        return formIdAtStage(formOrGroupId, stage)
                .map(FormPowerRegistry::idsForForm)
                .orElseGet(List::of);
    }

    /** Powers assigned to one exact form id, without player-specific changes. */
    public static List<ResourceLocation> powersFor(ResourceLocation exactFormId) {
        return FormPowerRegistry.idsForForm(Objects.requireNonNull(exactFormId, "exactFormId"));
    }

    /** Powers currently available to a player, including accessory changes. */
    public static List<ResourceLocation> powersFor(Player player) {
        return FormPowerRegistry.idsFor(Objects.requireNonNull(player, "player"));
    }
}
