package net.onixary.shapeShifterCurseForge.api.registry;

/** Internal storage keys for the legacy form-flag engine. Public APIs should use semantic methods. */
final class SscFormFlags {
    static final String STARTER = "starter_form";
    static final String SPECIAL = "special_form";
    static final String FINAL = "final_form";
    static final String INHIBITOR_IMMUNE = "inhibitor_immune";
    static final String INHIBITOR_RESISTANT = "inhibitor_resist";
    static final String CATALYST_IMMUNE = "catalyst_immune";
    static final String CATALYST_RESISTANT = "catalyst_resist";
    static final String CAN_REACH_FINAL = "can_transform_to_final_form";
    static final String INSTINCT_DISABLED = "no_instinct";
    static final String INSTINCT_LOCKED = "lock_instinct";
    static final String CURSED_MOON_IMMUNE = "no_cursed_moon_effect";
    static final String CURSED_MOON_FINAL = "cursed_moon_final_form";
    private SscFormFlags() {
    }
}
