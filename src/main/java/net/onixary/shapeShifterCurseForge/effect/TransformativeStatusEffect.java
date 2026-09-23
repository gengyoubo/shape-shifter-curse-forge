package net.onixary.shapeShifterCurseForge.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.onixary.shapeShifterCurseForge.power.TransformativeEffectService;

/** Potion-visible equivalent of Fabric's queued transformative status effect. */
public final class TransformativeStatusEffect extends MobEffect {
    private final ResourceLocation targetForm;
    public TransformativeStatusEffect(ResourceLocation targetForm) {
        super(MobEffectCategory.NEUTRAL, 0xB98BEE);
        this.targetForm = targetForm;
    }
    /** Queue the transformation when the effect is first applied, rather than after
     * its visible potion timer elapses.  The queued transformation has its own
     * lifetime and is activated by sleeping, like Fabric's transformative effect. */
    public void queue(ServerPlayer player) {
        TransformativeEffectService.apply(player, targetForm);
    }
}
