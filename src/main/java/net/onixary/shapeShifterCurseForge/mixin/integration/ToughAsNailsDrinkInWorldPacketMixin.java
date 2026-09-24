package net.onixary.shapeShifterCurseForge.mixin.integration;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.integration.toughasnails.ToughAsNailsPowerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Prevents TAN's hand-drinking dirty-water effect at the point where TAN applies it. */
@Mixin(targets = "toughasnails.network.MessageDrinkInWorld$Handler", remap = false)
@Pseudo
public abstract class ToughAsNailsDrinkInWorldPacketMixin {
    @Redirect(method = "lambda$handle$0", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            remap = true), remap = false)
    private static boolean ssc$preventHandDrinkEffect(ServerPlayer player, MobEffectInstance effect) {
        return !ToughAsNailsPowerUtils.shouldPreventDirtyWaterThirstEffect(player) && player.addEffect(effect);
    }
}
