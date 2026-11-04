package net.onixary.shapeShifterCurseForge.other.config;

import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;

/** Server config is authoritative for client-side prediction. */
public final class GameplayConfigState {
    private static volatile boolean clientFoodHabit = true;
    private static volatile boolean clientFullStack = true;
    private GameplayConfigState() {}
    public static boolean foodHabitEnabled() {
        return EffectiveSide.get() == LogicalSide.SERVER ? SscCommonConfig.FOOD_HABIT.get() : clientFoodHabit;
    }
    public static boolean fullStackUpgrade() {
        return EffectiveSide.get() == LogicalSide.SERVER ? SscCommonConfig.FULL_STACK_UPGRADE.get() : clientFullStack;
    }
    public static void setClient(boolean foodHabit, boolean fullStack) {
        clientFoodHabit = foodHabit;
        clientFullStack = fullStack;
    }
}
