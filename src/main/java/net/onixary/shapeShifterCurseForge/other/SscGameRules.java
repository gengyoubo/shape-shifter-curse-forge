package net.onixary.shapeShifterCurseForge.other;

import net.minecraft.world.level.GameRules;

/** World-persistent SSC rules, registered before any world is loaded. */
public final class SscGameRules {
    public static final GameRules.Key<GameRules.BooleanValue> KEEP_FORM_AFTER_DEATH =
            GameRules.register("sscKeepFormAfterDeath", GameRules.Category.PLAYER,
                    GameRules.BooleanValue.create(true));

    private SscGameRules() { }

    public static void initialize() { }
}
