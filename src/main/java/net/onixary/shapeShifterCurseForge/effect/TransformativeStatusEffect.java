package net.onixary.shapeShifterCurseForge.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.onixary.shapeShifterCurseForge.power.TransformativeEffectService;

/** Potion-visible equivalent of Fabric's queued transformative status effect. */
public final class TransformativeStatusEffect extends MobEffect {
    private final ResourceLocation targetForm;
    public TransformativeStatusEffect(ResourceLocation targetForm) {
        super(MobEffectCategory.NEUTRAL, 0xB98BEE);
        this.targetForm = targetForm;
    }
    /** Queue the transformation for the same lifetime as the visible status effect. */
    public void queue(ServerPlayer player, int durationTicks) {
        TransformativeEffectService.apply(player, targetForm, durationTicks);
    }
}
