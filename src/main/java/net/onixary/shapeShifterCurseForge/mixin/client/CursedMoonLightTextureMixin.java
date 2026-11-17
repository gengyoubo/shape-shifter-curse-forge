package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.onixary.shapeShifterCurseForge.client.codex.CursedMoonData;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LightTexture.class)
public abstract class CursedMoonLightTextureMixin {
    @Shadow @Final private Minecraft minecraft;

    // This first Vector3f local is the sky color, before block light and night vision.
    @ModifyVariable(method = "updateLightTexture", at = @At("STORE"), ordinal = 0)
    private Vector3f ssc$tintSkyLight(Vector3f skyColor) {
        ClientLevel level = minecraft.level;
        if (CursedMoonData.isCursedMoonDay(level) && level.dimensionType().hasSkyLight()) {
            float blend = Math.max(0.0F, 1.0F - level.getSkyDarken(1.0F) - level.getRainLevel(1.0F));
            skyColor.lerp(new Vector3f(1.0F, 0.24F, 0.82F), blend);
        }
        return skyColor;
    }
}
