package net.onixary.shapeShifterCurseForge.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;

public final class TransformativeBatEntity extends Bat {
    public TransformativeBatEntity(EntityType<? extends Bat> type, Level level) { super(type, level); }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) TransformativeMobEffects.tryApply(target, 0.5F, "bat_0");
        return hit;
    }
}
