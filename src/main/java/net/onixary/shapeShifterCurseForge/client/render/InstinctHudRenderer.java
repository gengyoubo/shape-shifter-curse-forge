package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.client.InstinctClientState;

/** Renders the instinct meter just to the right of the hotbar. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class InstinctHudRenderer {
    private static final int WIDTH = 48;
    private static final int HEIGHT = 7;

    private InstinctHudRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !InstinctClientState.visible()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int x = (graphics.guiWidth() + 182) / 2 + 5;
        int y = graphics.guiHeight() - 15;
        int border = InstinctClientState.locked() ? 0xFFE95C9D : 0xFFD9A2C1;
        int fill = InstinctClientState.locked() ? 0xFF74304F : 0xFFE83D9A;
        float valueRatio = Math.max(0.0F, Math.min(1.0F, InstinctClientState.value() / 100.0F));
        float rateRatio = Math.max(0.0F, Math.min(1.0F,
                Math.abs(InstinctClientState.rate()) * 3600.0F / 100.0F));

        graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xCC17121A);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, border);
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, 0xFF382A36);
        int filled = Math.round((WIDTH - 2) * valueRatio);
        if (filled > 0) graphics.fill(x + 1, y + 1, x + 1 + filled, y + HEIGHT - 1, fill);

        // The thin pink accent reflects the current instinct change rate.
        int rateWidth = Math.round(WIDTH * rateRatio);
        if (!InstinctClientState.locked() && rateWidth > 0) {
            graphics.fill(x, y - 2, x + rateWidth, y - 1, 0xFFFF8CCB);
        }
    }
}
