package net.onixary.shapeShifterCurseForge.mixin.integration;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric excludes feral forms from TACZ's humanoid hold and aim poses. */
@Pseudo
@Mixin(targets = "com.tacz.guns.client.animation.third.InnerThirdPersonManager", remap = false)
public abstract class TaczThirdPersonMixin {
    @Inject(method = "setRotationAnglesHead", at = @At("HEAD"), cancellable = true)
    private static void ssc$skipFeralGunPose(LivingEntity entity, ModelPart rightArm, ModelPart leftArm,
                                             ModelPart body, ModelPart head, float limbSwingAmount,
                                             CallbackInfo ci) {
        if (entity instanceof Player player && FormManager.current(player).bodyType() == FormBodyType.FERAL) {
            ci.cancel();
        }
    }
}
