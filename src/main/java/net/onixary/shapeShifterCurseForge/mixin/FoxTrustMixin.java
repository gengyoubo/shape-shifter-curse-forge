package net.onixary.shapeShifterCurseForge.mixin;

import java.util.UUID;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fabric's FoxEntityMixin grants the fox its usual trusted-player behavior. */
@Mixin(Fox.class)
public abstract class FoxTrustMixin {
    @Inject(method = "trusts", at = @At("HEAD"), cancellable = true)
    private void ssc$trustSnowFoxForm(UUID uuid, CallbackInfoReturnable<Boolean> cir) {
        Fox fox = (Fox) (Object) this;
        Player player = fox.level().getPlayerByUUID(uuid);
        if (player == null) return;
        final boolean[] friendly = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:fox_friendly".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                friendly[0] = true;
            }
        });
        if (friendly[0]) cir.setReturnValue(true);
    }
}
