package net.onixary.shapeShifterCurseForge.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.level.Level;

public final class TransformativeOcelotEntity extends Ocelot {
    public TransformativeOcelotEntity(EntityType<? extends Ocelot> type, Level level) { super(type, level); }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) TransformativeMobEffects.tryApply(target, 0.5F, "ocelot_0");
        return hit;
    }
}
