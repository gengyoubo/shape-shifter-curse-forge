package net.onixary.shapeShifterCurseForge.power;

/** Runtime bridge exposed by the LivingEntity mixin to the Forge event layer. */
public interface LivingEntityJumpState {
    boolean ssc$wasJumpStartedOnBlock();

    void ssc$clearJumpStartedOnBlock();
}
