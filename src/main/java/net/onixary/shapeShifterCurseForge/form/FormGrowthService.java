package net.onixary.shapeShifterCurseForge.form;

import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.other.advancement.SscAdvancementTriggers;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService;
import net.onixary.shapeShifterCurseForge.power.TransformativeEffectService;

/** Growth and regression policy migrated from the Fabric catalyst/inhibitor transform reasons. */
public final class FormGrowthService {
    public enum Mode {
        CATALYST,
        POWERFUL_CATALYST,
        INHIBITOR,
        POWERFUL_INHIBITOR
    }

    private FormGrowthService() {
    }

    public static boolean apply(ServerPlayer player, Mode mode) {
        if ((mode == Mode.CATALYST || mode == Mode.POWERFUL_CATALYST)
                && CursedMoonService.isInCursedMoon(player.getServer().overworld())) {
            return false;
        }
        FormDefinition current = FormManager.current(player);
        boolean changed = switch (mode) {
            case CATALYST -> activateTransformativeEffect(player, current);
            case POWERFUL_CATALYST -> advance(player, current, true);
            case INHIBITOR -> regress(player, current, false);
            case POWERFUL_INHIBITOR -> regress(player, current, true);
        };
        if (changed) {
            if (mode == Mode.INHIBITOR || mode == Mode.POWERFUL_INHIBITOR) {
                SscApi.currentForm(player).ifPresent(data -> data.setLastTransformByCure(true));
            }
            switch (mode) {
                case CATALYST -> SscAdvancementTriggers.ON_TRANSFORM_BY_CATALYST.trigger(player);
                case INHIBITOR -> SscAdvancementTriggers.ON_TRANSFORM_BY_CURE.trigger(player);
                case POWERFUL_INHIBITOR -> SscAdvancementTriggers.ON_TRANSFORM_BY_CURE_FINAL.trigger(player);
                case POWERFUL_CATALYST -> { }
            }
        }
        return changed;
    }

    /** A regular catalyst activates a pending curse effect; it does not advance the form by itself. */
    private static boolean activateTransformativeEffect(ServerPlayer player, FormDefinition current) {
        if (!(current.hasFlag("can_have_transform_effect") || current.hasFlag("original_shifter"))
                || !TransformativeEffectService.has(player)) {
            return false;
        }
        var before = current.id();
        TransformativeEffectService.activate(player);
        return !FormManager.current(player).id().equals(before);
    }

    /**
     * Instinct reaches its threshold independently of catalyst resistance.
     */
    public static void advanceByInstinct(ServerPlayer player) {
        FormDefinition current = FormManager.current(player);
        if (current.hasFlag("no_instinct") || current.hasFlag("lock_instinct") || current.hasFlag("special_form")) {
            return;
        }
        FormDefinition target = FormRegistry.nextInProgression(current);
        if (target != null) {
            TransformManager.forceTransform(player, target.id(), false);
        }
    }

    private static boolean advance(ServerPlayer player, FormDefinition current, boolean powerful) {
        if (current.hasFlag("special_form") || current.hasFlag("catalyst_immune")
                || (!powerful && current.hasFlag("catalyst_resist"))) {
            return false;
        }

        FormDefinition target = FormRegistry.nextInProgression(current);
        if (target == null) {
            return false;
        }
        if (powerful && (!current.hasFlag("can_transform_to_final_form") || !target.hasFlag("final_form"))) {
            return false;
        }
        return TransformManager.forceTransform(player, target.id(), false);
    }

    private static boolean regress(ServerPlayer player, FormDefinition current, boolean powerful) {
        if (current.hasFlag("inhibitor_immune") || (!powerful && current.hasFlag("inhibitor_resist"))) {
            return false;
        }
        int targetStage = powerful && !current.hasFlag("inhibitor_resist") ? 0 : current.stage() - 1;
        FormDefinition target = targetStage > 0 ? FormRegistry.previousInProgression(current) : null;
        return target == null
                ? TransformManager.forceTransform(player, FormRegistry.ORIGINAL_SHIFTER, false)
                : TransformManager.forceTransform(player, target.id(), false);
    }
}
