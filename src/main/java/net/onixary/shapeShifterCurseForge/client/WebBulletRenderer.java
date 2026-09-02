package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.entity.WebBulletEntity;
import net.onixary.shapeShifterCurseForge.registry.ModEntities;

@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WebBulletRenderer {
    private WebBulletRenderer() {
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WEB_BULLET.get(), WebBulletRenderer::create);
    }

    private static ThrownItemRenderer<WebBulletEntity> create(EntityRendererProvider.Context context) {
        return new ThrownItemRenderer<>(context, 0.75F, true);
    }
}
