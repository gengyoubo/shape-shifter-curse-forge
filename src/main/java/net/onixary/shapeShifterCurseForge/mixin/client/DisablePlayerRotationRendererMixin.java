package net.onixary.shapeShifterCurseForge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric parity: forms with this power face north in third-person rendering. */
@Mixin(value = PlayerRenderer.class, priority = 100)
public abstract class DisablePlayerRotationRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void ssc$lockRotationToNorth(AbstractClientPlayer player, float yaw, float partialTick,
                                         PoseStack poseStack, MultiBufferSource buffers, int light,
                                         CallbackInfo ci) {
        if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) return;
        boolean[] hasPower = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!hasPower[0] && "shape-shifter-curse:disable_player_rotation".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                hasPower[0] = true;
            }
        });
        if (!hasPower[0]) return;
        player.yBodyRotO = player.yBodyRot = 180.0F;
        player.yHeadRotO = player.yHeadRot = 180.0F;
    }
}
