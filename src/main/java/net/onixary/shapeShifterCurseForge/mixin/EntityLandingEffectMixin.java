package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fabric's bypass_landing_effect suppresses block bounce, not fall damage. */
@Mixin(Entity.class)
public abstract class EntityLandingEffectMixin {
    @Inject(method = "isSuppressingBounce", at = @At("RETURN"), cancellable = true)
    private void ssc$bypassLandingBounce(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || !((Object) this instanceof Player player)) return;
        final boolean[] suppress = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!suppress[0]
                    && "shape-shifter-curse:bypass_landing_effect".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                suppress[0] = true;
            }
        });
        if (suppress[0]) cir.setReturnValue(true);
    }
}
