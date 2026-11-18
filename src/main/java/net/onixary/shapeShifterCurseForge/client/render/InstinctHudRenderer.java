package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.client.InstinctClientState;
import net.onixary.shapeShifterCurseForge.other.config.SscClientConfig;
import net.onixary.shapeShifterCurseForge.power.InstinctService;

/**
 * Instinct meter, Forge port of Fabric's {@code InstinctBarRenderer}.
 *
 * <p>{@code instinct_bar.png} is a 160x40 sheet of 5px rows: the row picks the colour for the
 * current rate (v 0 falling, 5 normal, 15/20/25/30 rising faster), the bar fills from the
 * right using the row's right half, and v 35 is the lock overlay (lock-instinct form or
 * cursed moon). Visibility (survival/adventure, forms with instinct) comes from the server.</p>
 */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class InstinctHudRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/gui/instinct_bar.png");
    private static final int WIDTH = 80;
    private static final int HEIGHT = 5;
    private static final int SHEET_WIDTH = 160;
    private static final int SHEET_HEIGHT = 40;
    private static final int LOCK_ROW = 35;
    /*
     * Fabric's thresholds are its base rate plus 0.005 / 0.01 / 0.1 of instinct effects. Forge
     * fills the meter with its own base rate (InstinctService.BASE_RATE) but adds the same
     * effect values, so the rows compare the amount above Forge's base rate.
     */
    private static final float EXTRA_1 = 0.005F;
    private static final float EXTRA_2 = 0.01F;
    private static final float EXTRA_3 = 0.1F;

    private InstinctHudRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !InstinctClientState.visible()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int x = HudPosition.x(graphics.guiWidth(), SscClientConfig.INSTINCT_POSITION.get(), SscClientConfig.INSTINCT_OFFSET_X.get());
        int y = HudPosition.y(graphics.guiHeight(), SscClientConfig.INSTINCT_POSITION.get(), SscClientConfig.INSTINCT_OFFSET_Y.get());
        int row = rowFor(InstinctClientState.rate());
        float proportion = Math.max(0.0F, Math.min(100.0F, InstinctClientState.value())) / 100.0F;
        int filled = (int) Math.ceil(WIDTH * proportion);
        graphics.blit(TEXTURE, x, y, 0, row, WIDTH - filled, HEIGHT, SHEET_WIDTH, SHEET_HEIGHT);
        graphics.blit(TEXTURE, x + WIDTH - filled, y, SHEET_WIDTH - filled, row, filled, HEIGHT,
                SHEET_WIDTH, SHEET_HEIGHT);
        if (InstinctClientState.locked()) {
            graphics.blit(TEXTURE, x, y, 0, LOCK_ROW, WIDTH, HEIGHT, SHEET_WIDTH, SHEET_HEIGHT);
        }
    }

    private static int rowFor(float rate) {
        float extra = rate - InstinctService.BASE_RATE;
        if (extra > EXTRA_3) return 30;
        if (extra > EXTRA_2) return 25;
        if (extra > EXTRA_1) return 20;
        if (extra > 0.0F) return 15;
        if (rate < 0.0F) return 0;
        return 5;
    }
}
