package net.onixary.shapeShifterCurseForge.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.menu.AltarMenu;

public class AltarScreen extends AbstractContainerScreen<AltarMenu> {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/gui/altar_craft_ui.png");
    private static final ResourceLocation BG_ALTER = ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/gui/alter_craft_ui.png");
    private final boolean isAlter;
    public AltarScreen(AltarMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.isAlter = menu.toString().contains("alter"); // fallback, will be set via menu field
        this.imageWidth = 176; this.imageHeight = 166;
    }
    @Override protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        g.blit(isAlter ? BG_ALTER : BG, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int prog = menu.getProgress();
        int total = menu.getTotal();
        if (total > 0 && prog > 0) {
            int w = (int)(24 * (prog / (float) total));
            g.blit(isAlter ? BG_ALTER : BG, leftPos + 79, topPos + 34, 176, 0, w, 16);
        }
        int fuel = menu.getFuel();
        if (fuel > 0) {
            int h = (int)(14 * Math.min(1f, fuel / 8000f));
            g.blit(isAlter ? BG_ALTER : BG, leftPos + 152, topPos + 57 - h, 176, 14 + (14 - h), 14, h);
        }
    }
    @Override public void render(GuiGraphics g, int mx, int my, float pt) { renderBackground(g); super.render(g, mx, my, pt); renderTooltip(g, mx, my); }
}
