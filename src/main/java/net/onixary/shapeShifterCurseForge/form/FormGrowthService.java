package net.onixary.shapeShifterCurseForge.form;

import net.minecraft.server.level.ServerPlayer;

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
        return switch (mode) {
            case CATALYST -> advance(player, current, false);
            case POWERFUL_CATALYST -> advance(player, current, true);
            case INHIBITOR -> regress(player, current, false);
            case POWERFUL_INHIBITOR -> regress(player, current, true);
        };
    }

    private static boolean advance(ServerPlayer player, FormDefinition current, boolean powerful) {
        if (current.hasFlag("special_form") || current.hasFlag("catalyst_immune")
                || (!powerful && current.hasFlag("catalyst_resist"))) {
            return false;
        }

        FormGroup group = FormRegistry.getGroup(current.groupId());
        if (group == null) {
            return false;
        }
        FormDefinition target = group.firstAtTier(current.tier() + 1);
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
        FormDefinition target = null;
        FormGroup group = FormRegistry.getGroup(current.groupId());
        if (group != null && targetTier > 0) {
            target = group.firstAtTier(targetTier);
        }
        return target == null ? FormManager.setForm(player, FormRegistry.ORIGINAL_SHIFTER) : FormManager.setForm(player, target.id());
    }
}
