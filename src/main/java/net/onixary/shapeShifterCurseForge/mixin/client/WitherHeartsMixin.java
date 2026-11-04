package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.client.render.ClientPowerEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.gui.Gui$HeartType")
public abstract class WitherHeartsMixin {
    @Redirect(method = "forPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z", ordinal = 1))
    private static boolean ssc$hideWitherHearts(Player player, MobEffect effect) {
        return player.hasEffect(effect) && !ClientPowerEffects.has(player, "shape-shifter-curse:disable_wither_hearts");
    }
}
