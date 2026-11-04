package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fabric's AbstractClientPlayerEntityMixin#modifyPlayerSkin. */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerSkinMixin {
    @Unique
    private static final ResourceLocation SSC$BASE_SKIN = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/entity/base_player/ssc_base_skin.png");

    @Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true)
    private void ssc$useBaseSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        if (FormTextureUtils.usesBaseSkin((AbstractClientPlayer) (Object) this)) {
            cir.setReturnValue(SSC$BASE_SKIN);
        }
    }
}
