package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.PerkPowerService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MobEffectInstance.class)
public abstract class WitherIntervalMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/effect/MobEffect;isDurationEffectTick(II)Z"))
    private boolean ssc$witherInterval(MobEffect effect, int duration, int amplifier,
                                       LivingEntity target, Runnable onExpiration) {
        boolean vanilla = effect.isDurationEffectTick(duration, amplifier);
        return effect == MobEffects.WITHER && target instanceof Player player
                ? PerkPowerService.shouldApplyWither(player, duration, amplifier, vanilla) : vanilla;
    }
}
