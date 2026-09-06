package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes apoli:prevent_block_selection hide a matching block from the client hit result. */
@Mixin(GameRenderer.class)
public abstract class GameRendererBlockSelectionMixin {
    @Inject(method = "pick", at = @At("TAIL"))
    private void ssc$preventBlockSelection(float partialTick, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() == HitResult.Type.MISS) return;

        final boolean[] prevent = {false};
        FormPowerRegistry.visitActive(minecraft.player, (id, power) -> {
            if (prevent[0] || !"apoli:prevent_block_selection".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(minecraft.player, minecraft.player,
                    power.getAsJsonObject("condition"))) return;
            if (FormPowerRuntime.matchesBlockState(minecraft.player.level(), hit.getBlockPos(),
                    power.getAsJsonObject("block_condition"))) prevent[0] = true;
        });
        if (prevent[0]) {
            minecraft.hitResult = BlockHitResult.miss(hit.getLocation(), hit.getDirection(), hit.getBlockPos());
        }
    }
}
