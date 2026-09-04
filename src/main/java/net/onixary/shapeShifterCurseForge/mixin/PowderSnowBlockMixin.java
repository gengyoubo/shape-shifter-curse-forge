package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {
    @Inject(method = "canEntityWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void ssc$allowPowderSnowWalking(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LivingEntity living && living instanceof Player player) {
            final boolean[] hasPower = {false};
            FormPowerRegistry.visitActive(player, (id, power) -> {
                if ("shape-shifter-curse:powder_snow_walker".equals(FormPowerRegistry.typeOf(power))
                        && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                    hasPower[0] = true;
                }
            });
            if (hasPower[0]) {
                cir.setReturnValue(true);
            }
        }
    }
}
