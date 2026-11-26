package net.onixary.shapeShifterCurseForge.mixin.integration;

import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.client.render.GeoBonePose;
import net.onixary.shapeShifterCurseForge.client.render.TaczAnimationCompat;
import net.onixary.shapeShifterCurseForge.client.render.TaczFormBodyMask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.tacz.guns.compat.playeranimator.animation.AnimationDataRegisterFactory", remap = false)
public abstract class TaczPlayerAnimatorLayersMixin {
    // Newer TaCZ releases can clear modifiers when changing clips, so also use their layer-wide guard.
    @Inject(method = "lambda$createLayer$0", at = @At("RETURN"), cancellable = true, require = 0)
    private static void ssc$suppressFeralTransforms(AbstractClientPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && GeoBonePose.isFramePlayer(player) && TaczAnimationCompat.usesSscThirdPerson(player)) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(method = "registerData", at = @At(value = "INVOKE",
            target = "Ldev/kosmx/playerAnim/minecraftApi/PlayerAnimationFactory$FactoryHolder;registerFactory(Lnet/minecraft/resources/ResourceLocation;ILdev/kosmx/playerAnim/minecraftApi/PlayerAnimationFactory;)V"))
    private static void ssc$maskFormBody(PlayerAnimationFactory.FactoryHolder registry, ResourceLocation layer,
                                        int priority, PlayerAnimationFactory factory) {
        PlayerAnimationFactory effectiveFactory = switch (layer.getPath()) {
            case "lower_animation" -> TaczFormBodyMask.wrap(factory, TaczFormBodyMask.Layer.LOWER);
            case "loop_upper_animation", "once_upper_animation" -> TaczFormBodyMask.wrap(factory, TaczFormBodyMask.Layer.UPPER);
            case "rotation" -> TaczFormBodyMask.wrap(factory, TaczFormBodyMask.Layer.ROTATION);
            default -> factory;
        };
        registry.registerFactory(layer, priority, effectiveFactory);
    }
}
