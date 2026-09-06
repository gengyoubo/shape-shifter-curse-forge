package net.onixary.shapeShifterCurseForge.form;

import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.advancement.SscAdvancementTriggers;

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
        FormDefinition current = FormManager.current(player);
        boolean changed = switch (mode) {
            case CATALYST -> advance(player, current, false);
            case POWERFUL_CATALYST -> advance(player, current, true);
            case INHIBITOR -> regress(player, current, false);
            case POWERFUL_INHIBITOR -> regress(player, current, true);
        };
        if (changed) {
            switch (mode) {
                case CATALYST -> SscAdvancementTriggers.ON_TRANSFORM_BY_CATALYST.trigger(player);
                case INHIBITOR -> SscAdvancementTriggers.ON_TRANSFORM_BY_CURE.trigger(player);
                case POWERFUL_INHIBITOR -> SscAdvancementTriggers.ON_TRANSFORM_BY_CURE_FINAL.trigger(player);
                case POWERFUL_CATALYST -> { }
            }
        }
        return changed;
    }

    /** Instinct reaches its threshold independently of catalyst resistance. */
    public static boolean advanceByInstinct(ServerPlayer player) {
        FormDefinition current = FormManager.current(player);
        if (current.hasFlag("no_instinct") || current.hasFlag("lock_instinct") || current.hasFlag("special_form")) {
            return false;
        }
        FormDefinition target = FormRegistry.nextInProgression(current);
        return target != null && FormManager.setForm(player, target.id());
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
        return FormManager.setForm(player, target.id());
    }

    private static boolean regress(ServerPlayer player, FormDefinition current, boolean powerful) {
        if (current.hasFlag("inhibitor_immune") || (!powerful && current.hasFlag("inhibitor_resist"))) {
            return false;
        }
        int targetTier = powerful && !current.hasFlag("inhibitor_resist") ? 0 : current.tier() - 1;
        FormDefinition target = targetTier > 0 ? FormRegistry.previousInProgression(current) : null;
        return target == null ? FormManager.setForm(player, FormRegistry.ORIGINAL_SHIFTER) : FormManager.setForm(player, target.id());
    }
}
