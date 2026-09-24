package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class PlayerSweepingMixin {
    @ModifyVariable(method = "attack", at = @At("LOAD"), ordinal = 3)
    private boolean ssc$forceVanillaSweep(boolean original) {
        if (original) return true;
        Player player = (Player) (Object) this;
        final boolean[] force = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:always_sweeping".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                force[0] = true;
            }
        });
        return force[0];
    }
}
