package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.MovementPowerService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feed forced sneak into vanilla's actual input/pose sync, as Fabric does. */
@Mixin(KeyboardInput.class)
public abstract class KeepSneakingInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void ssc$forceSneakInput(boolean slowDown, float multiplier, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player != null && MovementPowerService.shouldForceSneaking(player)) {
            this.shiftKeyDown = true;
        }
    }
}
