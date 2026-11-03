package net.onixary.shapeShifterCurseForge.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.other.menu.AltarMenu;

public class AltarScreen extends AbstractContainerScreen<AltarMenu> {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/gui/altar_craft_ui.png");
    /** The texture has a 24 px sprite strip at its right edge; it is not part of the window. */
    private static final int WINDOW_WIDTH = 176;
    private static final int TEXTURE_WIDTH = 200;
    private static final int TEXTURE_HEIGHT = 166;
    public AltarScreen(AltarMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = WINDOW_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
    }
    @Override protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        ResourceLocation background = BG;
        g.blit(background, leftPos, topPos, 0, 0, WINDOW_WIDTH, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        // The older Forge background has no separate catalyst slot.
        g.fill(leftPos + 7, topPos + 56, leftPos + 25, topPos + 74, 0xff373737);
        g.fill(leftPos + 8, topPos + 57, leftPos + 24, topPos + 73, 0xff8b8b8b);
        int prog = menu.getProgress();
        int total = menu.getTotal();
        if (total > 0 && prog > 0) {
            int w = (int)(24 * (prog / (float) total));
            g.blit(background, leftPos + 89, topPos + 35, 176, 0, w, 17, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
        int fuel = menu.getFuel();
        if (fuel > 0) {
            int h = (int)(14 * Math.min(1f, fuel / 102400f));
            g.blit(background, leftPos + 152, topPos + 57 - h, 176, 14 + (14 - h), 14, h,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
    }
    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        if (menu.getSlot(9).getItem().isEmpty() && isHovering(8, 57, 16, 16, mx, my)) {
            g.renderTooltip(font, Component.translatable("gui.shape_shifter_curse.altar.catalyst"), mx, my);
        } else if (menu.getSlot(10).getItem().isEmpty() && isHovering(152, 57, 16, 16, mx, my)) {
            g.renderTooltip(font, Component.translatable("gui.shape_shifter_curse.altar.fuel"), mx, my);
        }
    }
}
