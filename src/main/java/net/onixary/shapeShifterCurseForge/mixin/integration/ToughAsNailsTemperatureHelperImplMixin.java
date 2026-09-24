package net.onixary.shapeShifterCurseForge.mixin.integration;

import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.integration.toughasnails.ToughAsNailsPowerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies TAN temperature offsets using the same five-level clamp as the Fabric implementation. */
@Mixin(targets = "toughasnails.temperature.TemperatureHelperImpl", remap = false)
@Pseudo
public abstract class ToughAsNailsTemperatureHelperImplMixin {
    @Inject(method = "armorModifier", at = @At("RETURN"), cancellable = true, remap = false)
    private static void ssc$modifyArmorTemperature(Player player, @Coerce Object current,
                                                    CallbackInfoReturnable<Object> cir) {
        cir.setReturnValue(ToughAsNailsPowerUtils.modifyTemperatureValue(player, cir.getReturnValue()));
    }
}
