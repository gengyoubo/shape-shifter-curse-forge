package net.onixary.shapeShifterCurseForge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.client.render.ClientPowerEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class HurtCameraMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void ssc$disableHurtCamera(PoseStack pose, float partialTick, CallbackInfo ci) {
        if (Minecraft.getInstance().getCameraEntity() instanceof Player player
                && ClientPowerEffects.has(player, "shape-shifter-curse:disable_hurt_camera")) ci.cancel();
    }
}
