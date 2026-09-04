package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SweetBerryBushBlock.class)
public abstract class SweetBerryBushBlockMixin {
    @Inject(method = "entityInside", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), cancellable = true)
    private void ssc$preventBerryDamage(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (entity instanceof Player player) {
            final boolean[] immune = {false};
            FormPowerRegistry.visitActive(player, (id, power) -> {
                if ("shape-shifter-curse:prevent_berry_effect".equals(FormPowerRegistry.typeOf(power))
                        && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                    immune[0] = true;
                }
            });
            if (immune[0]) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "entityInside", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;makeStuckInBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V"), cancellable = true)
    private void ssc$preventBerrySlow(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (entity instanceof Player player) {
            final boolean[] immune = {false};
            FormPowerRegistry.visitActive(player, (id, power) -> {
                if ("shape-shifter-curse:prevent_berry_effect".equals(FormPowerRegistry.typeOf(power))
                        && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                    immune[0] = true;
                }
            });
            if (immune[0]) {
                ci.cancel();
            }
        }
    }
}
