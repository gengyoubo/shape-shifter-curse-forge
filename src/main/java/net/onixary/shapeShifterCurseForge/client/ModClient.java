package net.onixary.shapeShifterCurseForge.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.AxolotlRenderer;
import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.OcelotRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.client.screen.AltarScreen;
import net.onixary.shapeShifterCurseForge.client.render.FormAttunerBeamRenderer;
import net.onixary.shapeShifterCurseForge.registry.ModBlockEntities;
import net.onixary.shapeShifterCurseForge.registry.ModMenuTypes;
import net.onixary.shapeShifterCurseForge.registry.ModEntities;

@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.ALTAR.get(), AltarScreen::new);
            MenuScreens.register(ModMenuTypes.ALTER.get(), AltarScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TRANSFORMATIVE_BAT.get(), BatRenderer::new);
        event.registerEntityRenderer(ModEntities.TRANSFORMATIVE_AXOLOTL.get(), AxolotlRenderer::new);
        event.registerEntityRenderer(ModEntities.TRANSFORMATIVE_OCELOT.get(), OcelotRenderer::new);
        event.registerEntityRenderer(ModEntities.TRANSFORMATIVE_SPIDER.get(), SpiderRenderer::new);
        event.registerEntityRenderer(ModEntities.TRANSFORMATIVE_WOLF.get(), WolfRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FORM_ATTUNER.get(), FormAttunerBeamRenderer::new);
    }
}
