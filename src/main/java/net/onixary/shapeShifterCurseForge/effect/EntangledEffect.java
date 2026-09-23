package net.onixary.shapeShifterCurseForge.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.onixary.shapeShifterCurseForge.registry.ModEffects;

/** Keeps the visible entanglement tier proportional to remaining binding time. */
public final class EntangledEffect extends MobEffect {
    public static final int TICKS_PER_LEVEL = 20 * 5;
    public EntangledEffect() { super(MobEffectCategory.HARMFUL, 0x9F9F9F); }
    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration >= 1; }
    @Override public void applyEffectTick(LivingEntity entity, int amplifier) {
        MobEffectInstance instance = entity.getEffect(ModEffects.ENTANGLED.get());
        if (instance == null) return;
        int targetLevel = instance.getDuration() / TICKS_PER_LEVEL;
        if (instance.getAmplifier() != targetLevel) {
            entity.removeEffect(ModEffects.ENTANGLED.get());
            entity.addEffect(new MobEffectInstance(ModEffects.ENTANGLED.get(), instance.getDuration(), targetLevel));
        }
    }
}
