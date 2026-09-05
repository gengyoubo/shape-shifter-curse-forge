package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops player camera rotation while the data-driven rotation lock is active. */
@Mixin(Entity.class)
public abstract class EntityRotationMixin {
    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
    private void ssc$disablePlayerRotation(double yRot, double xRot, CallbackInfo ci) {
        if (!((Object) this instanceof Player player)) return;
        final boolean[] disabled = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!disabled[0] && "shape-shifter-curse:disable_player_rotation".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) disabled[0] = true;
        });
        if (disabled[0]) ci.cancel();
    }
}
