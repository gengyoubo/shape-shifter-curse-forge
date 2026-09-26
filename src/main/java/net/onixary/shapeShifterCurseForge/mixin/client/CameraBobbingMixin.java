package net.onixary.shapeShifterCurseForge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class CameraBobbingMixin {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void ssc$bobView(PoseStack matrices, float tickDelta, CallbackInfo ci) {
        if (!(Minecraft.getInstance().getCameraEntity() instanceof Player player)) return;
        String[] type = {null};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (type[0] == null && "shape-shifter-curse:form_camera_bobbing".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                type[0] = FormPowerRuntime.stringValue(power, "bobbing_type", "default");
            }
        });
        if (type[0] == null) return;
        ci.cancel();
        shape_shifter_curse$applyBobbing(matrices, tickDelta, player, type[0]);
    }

    @Unique
    private void shape_shifter_curse$applyBobbing(PoseStack matrices, float tickDelta, Player player, String bobbingType) {
        switch (bobbingType) {
            case "none"   -> { /* 完全无晃动，不做任何矩阵变换 */ }
            case "float"  -> shape_shifter_curse$applyFloatBobbing(matrices, tickDelta, player);
            case "feral" -> shape_shifter_curse$applyFeralBobbing(matrices, tickDelta, player);
            case "bat"    -> shape_shifter_curse$applyBatBobbing(matrices, tickDelta, player);
            default       -> shape_shifter_curse$applyDefaultBobbing(matrices, tickDelta, player);
        }
    }

    // -------------------------------------------------------------------------
    // 各 BobbingType 实现
    // -------------------------------------------------------------------------

    /**
     * default — 复现原版晃动逻辑（用于不识别的 bobbingType 时兜底）。
     */
    @Unique
    private void shape_shifter_curse$applyDefaultBobbing(PoseStack matrices, float tickDelta, Player player) {
        float deltaH   = player.walkDist - player.walkDistO;
        float phase    = -(player.walkDist + deltaH * tickDelta);
        float amplitude = Mth.lerp(tickDelta, player.oBob, player.bob);

        matrices.translate(
                (double)(Mth.sin(phase * Mth.PI) * amplitude * 0.5f),
                (double)(-Math.abs(Mth.cos(phase * Mth.PI) * amplitude)),
                0.0
        );
        matrices.mulPose(Axis.ZP.rotationDegrees(
                Mth.sin(phase * Mth.PI) * amplitude * 3.0f));
        matrices.mulPose(Axis.XP.rotationDegrees(
                Math.abs(Mth.cos(phase * Mth.PI - 0.2f) * amplitude) * 5.0f));
    }

    /**
     * float — 漂浮，慢速上下移动。
     */
    @Unique
    private void shape_shifter_curse$applyFloatBobbing(PoseStack matrices, float tickDelta, Player player) {
        float deltaH    = player.walkDist - player.walkDistO;
        float phase     = -(player.walkDist + deltaH * tickDelta);
        float amplitude = Mth.lerp(tickDelta, player.oBob, player.bob);
        float sin       = Mth.sin(phase * Mth.PI * 0.6f);

        matrices.translate(
                0.0,
                (double)(-Math.abs(sin) * amplitude) * 0.75f,
                0.0
        );
    }

    /**
     * feral
     */
    @Unique
    private void shape_shifter_curse$applyFeralBobbing(PoseStack matrices, float tickDelta, Player player) {
        float deltaH    = player.walkDist - player.walkDistO;
        float phase     = -(player.walkDist + deltaH * tickDelta);
        float amplitude = Mth.lerp(tickDelta, player.oBob, player.bob) * 0.55f;

        matrices.translate(
                (double)(Mth.sin(phase * Mth.PI) * amplitude * 0.3f),
                (double)(-Math.abs(Mth.cos(phase * Mth.PI * 1.1f) * amplitude) * 1.2f),
                0.0
        );
        matrices.mulPose(Axis.ZP.rotationDegrees(
                Mth.sin(phase * Mth.PI) * amplitude * 2.0f));
        matrices.mulPose(Axis.XP.rotationDegrees(
                Math.abs(Mth.cos(phase * Mth.PI - 0.2f) * amplitude) * 3.0f));
    }

    /**
     * bat
     */
    @Unique
    private void shape_shifter_curse$applyBatBobbing(PoseStack matrices, float tickDelta, Player player) {
        float deltaH    = player.walkDist - player.walkDistO;
        float phase     = -(player.walkDist + deltaH * tickDelta);
        float amplitude = Mth.lerp(tickDelta, player.oBob, player.bob);
        float sin       = Mth.sin(phase * Mth.PI );

        matrices.translate(
                0.0,
                (double)(-Math.abs(sin) * amplitude * 0.8f),
                0.0
        );
        matrices.mulPose(Axis.XP.rotationDegrees(
                sin * amplitude * 2.0f));
        matrices.mulPose(Axis.ZP.rotationDegrees(
                sin * amplitude * 1.0f));
    }
}

