package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.world.entity.Entity;
import net.onixary.shapeShifterCurseForge.client.PowerEntityGlow;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PowerEntityGlowColorMixin {
    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void ssc$outlineColor(CallbackInfoReturnable<Integer> cir) {
        var power = PowerEntityGlow.matchingPower((Entity) (Object) this);
        if (power == null || FormPowerRuntime.booleanValue(power, "use_teams", true)) return;
        int red = (int) (255 * FormPowerRuntime.floatValue(power, "red", 1));
        int green = (int) (255 * FormPowerRuntime.floatValue(power, "green", 1));
        int blue = (int) (255 * FormPowerRuntime.floatValue(power, "blue", 1));
        cir.setReturnValue((red << 16) | (green << 8) | blue);
    }
}
