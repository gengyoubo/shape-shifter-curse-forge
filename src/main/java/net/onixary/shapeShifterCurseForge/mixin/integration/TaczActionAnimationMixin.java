package net.onixary.shapeShifterCurseForge.mixin.integration;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep humanoid fire, reload and melee clips off feral forms, as on Fabric. */
@Pseudo
@Mixin(targets = "com.tacz.guns.compat.playeranimator.animation.AnimationManager", remap = false)
public abstract class TaczActionAnimationMixin {
    @Inject(method = "playOnceAnimation", at = @At("HEAD"), cancellable = true)
    private static void ssc$skipFeralGunAction(AbstractClientPlayer player, @Coerce Object display,
                                               ResourceLocation dataId, String animationName, CallbackInfo ci) {
        if (FormManager.current(player).bodyType() == FormBodyType.FERAL) {
            ci.cancel();
        }
    }
}
