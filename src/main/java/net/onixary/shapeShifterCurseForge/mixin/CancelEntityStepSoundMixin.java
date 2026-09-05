package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class CancelEntityStepSoundMixin {
    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void ssc$disableStepSound(BlockPos pos, BlockState state, CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            final boolean[] hasPower = {false};
            FormPowerRegistry.visitActive(player, (id, power) -> {
                if ("apoli:action_on_land".equals(FormPowerRegistry.typeOf(power))) {
                    // Fabric's NoStepSoundPower is mapped to apoli:no_step_sound or similar; check generic
                }
                if ("shape-shifter-curse:no_step_sound".equals(FormPowerRegistry.typeOf(power))
                        || "apoli:play_sound".equals(FormPowerRegistry.typeOf(power))) {
                    // Fallback: many forms use no_step_sound via custom power type
                }
                if ("shape-shifter-curse:no_step_sound".equals(FormPowerRegistry.typeOf(power))
                        && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                    hasPower[0] = true;
                }
                if ("apoli:no_step_sound".equals(FormPowerRegistry.typeOf(power))) {
                    hasPower[0] = true;
                }
                if ("apoli:prevent_game_event".equals(FormPowerRegistry.typeOf(power))
                        && "minecraft:step".equals(FormPowerRuntime.stringValue(power, "event", ""))
                        && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                    hasPower[0] = true;
                }
            });
            if (hasPower[0]) {
                ci.cancel();
            }
        }
    }
}
