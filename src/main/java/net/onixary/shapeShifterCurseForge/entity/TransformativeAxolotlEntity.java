package net.onixary.shapeShifterCurseForge.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.level.Level;

public final class TransformativeAxolotlEntity extends Axolotl {
    public TransformativeAxolotlEntity(EntityType<? extends Axolotl> type, Level level) { super(type, level); }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) TransformativeMobEffects.tryApply(target, 0.7F, "axolotl_0");
        return hit;
    }
}
