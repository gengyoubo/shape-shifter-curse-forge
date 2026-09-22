package net.onixary.shapeShifterCurseForge.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;

public final class TransformativeWolfEntity extends Wolf {
    public TransformativeWolfEntity(EntityType<? extends Wolf> type, Level level) { super(type, level); }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) TransformativeMobEffects.tryApply(target, 0.5F, "anubis_wolf_0");
        return hit;
    }
}
