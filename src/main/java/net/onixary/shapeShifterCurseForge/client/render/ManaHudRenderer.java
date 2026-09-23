package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.client.ManaClientState;

/** Small server-synchronised mana bar above the hotbar. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class ManaHudRenderer {
    private ManaHudRenderer() {}

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ManaClientState.available()) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int width = 80;
        int height = 6;
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - 47;
        float ratio = Math.min(1.0F, ManaClientState.amount() / ManaClientState.maximum());
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xCC12121A);
        graphics.fill(x, y, x + width, y + height, 0xFF312244);
        graphics.fill(x, y, x + Math.round(width * ratio), y + height, 0xFF79D9FF);
        String text = String.format(java.util.Locale.ROOT, "Mana %.0f / %.0f",
                ManaClientState.amount(), ManaClientState.maximum());
        graphics.drawString(minecraft.font, text, x + (width - minecraft.font.width(text)) / 2, y - 10, 0xD7F7FF, true);
    }
}
