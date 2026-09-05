package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the vanilla goal selector for form-affinity goals. */
@Mixin(Mob.class)
public interface MobGoalSelectorAccessor {
    @Accessor("goalSelector")
    GoalSelector ssc$getGoalSelector();
}
