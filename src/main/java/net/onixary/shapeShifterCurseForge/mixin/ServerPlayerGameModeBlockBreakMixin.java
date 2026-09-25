package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import net.onixary.shapeShifterCurseForge.power.MissingPowerEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Run action_on_block_break after the successful harvest, as in Apoli. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeBlockBreakMixin {
    @Shadow public ServerPlayer player;
    @Unique private BlockState ssc$stateBeforeBreak;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void ssc$rememberBrokenState(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ssc$stateBeforeBreak = player.level().getBlockState(pos);
    }

    @Inject(method = "destroyBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;playerDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.AFTER), require = 1)
    private void ssc$runHarvestedBlockActions(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (ssc$stateBeforeBreak != null) {
            MissingPowerEvents.onHarvestedBlock(player, ssc$stateBeforeBreak);
        }
    }
}
