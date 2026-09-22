package net.onixary.shapeShifterCurseForge.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;

public final class TransformativeSpiderEntity extends Spider {
    public TransformativeSpiderEntity(EntityType<? extends Spider> type, Level level) { super(type, level); }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) TransformativeMobEffects.tryApply(target, 0.5F, "spider_0");
        return hit;
    }
}
