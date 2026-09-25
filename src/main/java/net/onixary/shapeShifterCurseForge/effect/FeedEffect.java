package net.onixary.shapeShifterCurseForge.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;

/** Fabric's compressed-energy potion: food for all players, mana for familiar foxes. */
public final class FeedEffect extends MobEffect {
    public FeedEffect() { super(MobEffectCategory.BENEFICIAL, 0x9ACE67); }
    @Override public boolean isInstantenous() { return true; }
    @Override public void applyInstantenousEffect(Entity source, Entity indirectSource, LivingEntity target, int amplifier, double proximity) {
        if (target instanceof Player player) {
            double multiplier = Math.max(0.5D, proximity);
            player.getFoodData().eat((int) Math.ceil(8 * multiplier), (float) (0.6F * multiplier));
            if (FormActivePowerService.hasFamiliarFoxMana(player)) {
                FormActivePowerService.gainMana(player, (float) (25.0D * multiplier));
            }
        }
    }

    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration >= 1; }

    @Override public void applyEffectTick(LivingEntity target, int amplifier) {
        if (target instanceof Player player) {
            player.getFoodData().eat(8, 0.6F);
            if (FormActivePowerService.hasFamiliarFoxMana(player)) {
                FormActivePowerService.gainMana(player, 38.0F);
            }
        }
    }
}
