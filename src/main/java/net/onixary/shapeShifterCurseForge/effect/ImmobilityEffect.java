package net.onixary.shapeShifterCurseForge.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ImmobilityEffect extends MobEffect {
    public ImmobilityEffect() { super(MobEffectCategory.HARMFUL, 0); }
    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return true; }
    @Override public void applyEffectTick(LivingEntity entity, int amplifier) {
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(0, motion.y, 0);
        entity.hurtMarked = true;
    }
}
