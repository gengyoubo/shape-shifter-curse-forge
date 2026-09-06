package net.onixary.shapeShifterCurseForge.api.registry;

import java.util.Set;

/**
 * The single internal mapping between SSC's public form semantics and legacy flag storage.
 *
 * <p>This is deliberately package-private: add-ons should use the identically named semantic
 * methods on {@link SscForm.Builder} or {@link FormProperties}, rather than depending on flags.</p>
 */
final class SscFormRules {
    private SscFormRules() {
    }

    static void applyStarterForm(Set<String> flags) { flags.add(SscFormFlags.STARTER); }
    static void applySpecialForm(Set<String> flags) { flags.add(SscFormFlags.SPECIAL); }

    static void applyFinalForm(Set<String> flags) {
        flags.add(SscFormFlags.FINAL);
        flags.add(SscFormFlags.INHIBITOR_IMMUNE);
        flags.add(SscFormFlags.INSTINCT_DISABLED);
        flags.add(SscFormFlags.CURSED_MOON_IMMUNE);
    }

    static void applyInhibitorImmunity(Set<String> flags) { flags.add(SscFormFlags.INHIBITOR_IMMUNE); }
    static void applyInhibitorResistance(Set<String> flags) { flags.add(SscFormFlags.INHIBITOR_RESISTANT); }
    static void applyCatalystImmunity(Set<String> flags) { flags.add(SscFormFlags.CATALYST_IMMUNE); }
    static void applyCatalystResistance(Set<String> flags) { flags.add(SscFormFlags.CATALYST_RESISTANT); }
    static void applyCanTransformToFinalForm(Set<String> flags) { flags.add(SscFormFlags.CAN_REACH_FINAL); }
    static void applyInstinctDisabled(Set<String> flags) { flags.add(SscFormFlags.INSTINCT_DISABLED); }
    static void applyInstinctLocked(Set<String> flags) { flags.add(SscFormFlags.INSTINCT_LOCKED); }
    static void applyCursedMoonImmunity(Set<String> flags) { flags.add(SscFormFlags.CURSED_MOON_IMMUNE); }
    static void applyCursedMoonFinalForm(Set<String> flags) { flags.add(SscFormFlags.CURSED_MOON_FINAL); }
}
