package net.onixary.shapeShifterCurseForge.mixin.integration;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.integration.toughasnails.ToughAsNailsPowerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Prevents TAN's dirty-drink effect at the point where TAN applies it. */
@Mixin(targets = "toughasnails.thirst.ThirstHandler", remap = false)
@Pseudo
public abstract class ToughAsNailsThirstHandlerMixin {
    @Redirect(method = "onItemUseFinish", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            remap = true), remap = false)
    private boolean ssc$preventDirtyDrinkEffect(Player player, MobEffectInstance effect) {
        return !ToughAsNailsPowerUtils.shouldPreventDirtyWaterThirstEffect(player) && player.addEffect(effect);
    }
}
