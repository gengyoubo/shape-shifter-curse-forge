package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.power.ElytraFlightPowerService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds virtual wings only when an active elytra_flight power requests rendering. */
@Mixin(ElytraLayer.class)
public abstract class ElytraLayerMixin {
    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true, remap = false)
    private void ssc$renderPowerElytra(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && entity instanceof Player player
                && ElytraFlightPowerService.rendersVirtualElytra(player)) {
            cir.setReturnValue(true);
        }
    }
}
