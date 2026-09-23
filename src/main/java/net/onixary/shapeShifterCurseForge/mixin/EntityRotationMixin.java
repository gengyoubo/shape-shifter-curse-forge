package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops player camera rotation while the data-driven rotation lock is active. */
@Mixin(Entity.class)
public abstract class EntityRotationMixin {
    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
    private void ssc$disablePlayerRotation(double yRot, double xRot, CallbackInfo ci) {
    }
}
