package net.onixary.shapeShifterCurseForge.mixin.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.onixary.shapeShifterCurseForge.client.render.TaczAnimationCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skips TaCZ's offhand and hotbar gun display on feral bodies. */
@Pseudo
@Mixin(targets = "com.tacz.guns.client.renderer.other.HumanoidOffhandRender", remap = false)
public abstract class TaczThirdPersonItemMixin {
    @Inject(method = "renderGun", at = @At("HEAD"), cancellable = true)
    private static void ssc$skipFeralGunAttachments(LivingEntity entity, PoseStack poseStack,
                                                    MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
        if (TaczAnimationCompat.usesSscThirdPerson(entity)) {
            ci.cancel();
        }
    }
}
