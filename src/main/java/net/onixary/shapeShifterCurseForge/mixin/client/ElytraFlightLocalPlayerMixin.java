package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.power.ElytraFlightPowerService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Let virtual Elytra powers reach the normal jump-input and server-command path. */
@Mixin(LocalPlayer.class)
public abstract class ElytraFlightLocalPlayerMixin {
    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z",
            remap = false))
    private boolean ssc$allowPowerFlight(ItemStack chestItem, LivingEntity entity) {
        return chestItem.canElytraFly(entity)
                || entity instanceof Player player && ElytraFlightPowerService.hasFlight(player);
    }
}
