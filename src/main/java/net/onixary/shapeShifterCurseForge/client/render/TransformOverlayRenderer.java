package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.client.TransformClientState;

/**
 * Full-screen transform overlay, mirroring Fabric's {@code TransformOverlay}: two
 * stacked textures (nausea-black then pure black) tinted by the live strengths.
 */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class TransformOverlayRenderer {
    private static final ResourceLocation NAUSEA_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/overlay/nausea_black.png");
    private static final ResourceLocation BLACK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/overlay/black.png");

    private TransformOverlayRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        if (!TransformClientState.isTransforming()) {
            return;
        }
        float nausea = TransformClientState.nauseaStrength();
        float black = TransformClientState.blackStrength();
        if (nausea <= 0.0F && black <= 0.0F) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (nausea > 0.0F) {
            graphics.setColor(1.0F, 1.0F, 1.0F, nausea);
            graphics.blit(NAUSEA_TEXTURE, 0, 0, width, height, 0.0F, 0.0F, 256, 256, 256, 256);
        }
        if (black > 0.0F) {
            graphics.setColor(1.0F, 1.0F, 1.0F, black);
            graphics.blit(BLACK_TEXTURE, 0, 0, width, height, 0.0F, 0.0F, 256, 256, 256, 256);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
