package net.onixary.shapeShifterCurseForge.mixin;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Matches Fabric's hiss check when a phantom continues its swoop. */
@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomSweepAttackGoal")
public abstract class HissPhantomMixin {
    @Unique private Phantom ssc$phantom;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ssc$capturePhantom(Phantom phantom, CallbackInfo ci) {
        ssc$phantom = phantom;
    }

    @Inject(method = "canContinueToUse", at = @At("RETURN"), cancellable = true)
    private void ssc$hiss(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = ssc$phantom.getTarget();
        if (!cir.getReturnValueZ() || target == null) return;
        if (target instanceof Player player && ssc$invokeHiss(player)) {
            cir.setReturnValue(false);
            return;
        }
        for (Player player : ssc$phantom.level().getEntitiesOfClass(Player.class,
                target.getBoundingBox().inflate(8.0D), player -> !player.isSpectator())) {
            if (ssc$invokeHiss(player)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    @Unique
    private boolean ssc$invokeHiss(Player player) {
        JsonObject[] first = {null};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (first[0] == null && "shape-shifter-curse:hiss_phantom_power".equals(FormPowerRegistry.typeOf(power))) {
                first[0] = power;
            }
        });
        if (first[0] == null || !FormPowerRuntime.test(player, player, first[0].getAsJsonObject("condition"))) return false;
        FormPowerRuntime.executeBiEntity(player, ssc$phantom, first[0].getAsJsonObject("on_hiss_phantom_action"));
        return true;
    }
}
