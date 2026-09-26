package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.onixary.shapeShifterCurseForge.client.PowerEntityGlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class PowerEntityGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void ssc$powerOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && PowerEntityGlow.matchingPower(entity) != null) cir.setReturnValue(true);
    }
}
