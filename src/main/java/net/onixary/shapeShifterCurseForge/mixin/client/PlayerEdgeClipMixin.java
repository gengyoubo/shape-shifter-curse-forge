package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.onixary.shapeShifterCurseForge.client.FormKeyInputEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Client equivalent of Fabric's clipAtLedge toggle. */
@Mixin(Player.class)
public abstract class PlayerEdgeClipMixin {
    @Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
    private void ssc$disableClipAtLedge(Vec3 delta, MoverType moverType,
                                        CallbackInfoReturnable<Vec3> callback) {
        if (FormKeyInputEvents.isClipAtLedgeDisabled()) {
            callback.setReturnValue(delta);
        }
    }
}
