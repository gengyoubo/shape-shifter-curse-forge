package net.onixary.shapeShifterCurseForge.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Fabric's feed potion. Mana bonus is intentionally delegated to the future persistent Mana port. */
public final class FeedEffect extends MobEffect {
    public FeedEffect() { super(MobEffectCategory.BENEFICIAL, 0x9ACE67); }
    @Override public boolean isInstantenous() { return true; }
    @Override public void applyInstantenousEffect(Entity source, Entity indirectSource, LivingEntity target, int amplifier, double proximity) {
        if (target instanceof net.minecraft.world.entity.player.Player player) {
            double multiplier = Math.max(0.5D, proximity);
            player.getFoodData().eat((int) Math.ceil(8 * multiplier), (float) (0.6F * multiplier));
        }
    }
}
