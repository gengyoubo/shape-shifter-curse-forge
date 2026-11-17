package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.client.codex.CursedMoonData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public abstract class CursedMoonSkyMixin {
    @Shadow @Final private static ResourceLocation MOON_LOCATION;
    @Shadow private ClientLevel level;

    @Unique private static final ResourceLocation SSC_CURSED_MOON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE,
                    "textures/environment/cursed_moon_phases.png");

    @ModifyArg(method = "renderSky", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"),
            index = 1)
    private ResourceLocation ssc$selectMoonTexture(ResourceLocation texture) {
        return MOON_LOCATION.equals(texture) && CursedMoonData.isCursedMoonDay(level)
                ? SSC_CURSED_MOON_TEXTURE : texture;
    }
}
