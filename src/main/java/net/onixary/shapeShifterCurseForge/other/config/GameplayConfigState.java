package net.onixary.shapeShifterCurseForge.other.config;

import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;

/** Server config is authoritative for client-side prediction. */
public final class GameplayConfigState {
    private static volatile boolean clientFoodHabit = true;
    private static volatile boolean clientFullStack = true;
    private static volatile boolean clientShiftCrawl = true;
    private GameplayConfigState() {}
    public static boolean foodHabitEnabled() {
        return EffectiveSide.get() == LogicalSide.SERVER ? SscCommonConfig.FOOD_HABIT.get() : clientFoodHabit;
    }
    public static boolean fullStackUpgrade() {
        return EffectiveSide.get() == LogicalSide.SERVER ? SscCommonConfig.FULL_STACK_UPGRADE.get() : clientFullStack;
    }
    public static boolean shiftCrawl() {
        return EffectiveSide.get() == LogicalSide.SERVER ? SscCommonConfig.SHIFT_CRAWL.get() : clientShiftCrawl;
    }
    public static void setClient(boolean foodHabit, boolean fullStack, boolean shiftCrawl) {
        clientFoodHabit = foodHabit;
        clientFullStack = fullStack;
        clientShiftCrawl = shiftCrawl;
    }
}
