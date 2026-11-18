package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Apoli's {@code immune_freeze} power: snow foxes do not accumulate powder-snow freezing. */
@Mixin(Entity.class)
public abstract class ImmuneFreezeMixin {
    @Inject(method = "canFreeze", at = @At("HEAD"), cancellable = true)
    private void ssc$immuneFreeze(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player
                && FormPowerRegistry.hasActiveType(player, "shape-shifter-curse:immune_freeze")) {
            cir.setReturnValue(false);
        }
    }
}
