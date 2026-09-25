package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.onixary.shapeShifterCurseForge.client.NightVisionPowerLookup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Apoli takes the maximum of its power strength and vanilla lightmap night vision. */
@Mixin(LightTexture.class)
public abstract class NightVisionLightTextureMixin {
    @Shadow @Final private Minecraft minecraft;

    @ModifyVariable(method = "updateLightTexture", at = @At(value = "STORE"), ordinal = 6)
    private float ssc$nightVisionLightmap(float vanilla) {
        if (minecraft.player == null) return vanilla;
        float strength = NightVisionPowerLookup.strength(minecraft.player);
        return strength >= 0.0F ? Math.max(strength, vanilla) : vanilla;
    }
}
