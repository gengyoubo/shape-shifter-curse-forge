package net.onixary.shapeShifterCurseForge.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.other.menu.AltarMenu;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Altar crafting screen, Forge port of Fabric's {@code AltarCraftUI}: the 174x166 window from
 * the 256x256 {@code altar_craft_ui.png}, with the process bar (u 174, v 0, 43x9) and the
 * moondust fuel bar (u 174, v 9, 43x3) filling left to right, and the fuel tooltip.
 */
public class AltarScreen extends AbstractContainerScreen<AltarMenu> {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/gui/altar_craft_ui.png");
    private static final int WIDTH = 174;
    private static final int HEIGHT = 166;
    private static final int TEXTURE_SIZE = 256;
    private static final int BAR_X = 84;
    private static final int PROCESS_BAR_Y = 39;
    private static final int FUEL_BAR_Y = 49;
    private static final int BAR_W = 43;
    private static final int PROCESS_BAR_H = 9;
    private static final int FUEL_BAR_H = 3;
    private static final int SPRITE_U = 174;
    private static final int MAX_FUEL = 102400;
    /** Fuel units per moondust, for the tooltip (Fabric: fuel / 800). */
    private static final long FUEL_PER_MOONDUST = 800L;

    public AltarScreen(AltarMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    /** Fabric draws no labels on this screen; the title and inventory text are part of the art. */
    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
    }

    @Override protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        g.blit(BG, leftPos, topPos, 0, 0, WIDTH, HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        int total = menu.getTotal();
        if (total > 0) {
            int width = (int) (BAR_W * (menu.getProgress() / (float) total));
            g.blit(BG, leftPos + BAR_X, topPos + PROCESS_BAR_Y, SPRITE_U, 0, width, PROCESS_BAR_H, TEXTURE_SIZE, TEXTURE_SIZE);
        }
        int fuelWidth = (int) (BAR_W * Math.min(1.0F, menu.getFuel() / (float) MAX_FUEL));
        g.blit(BG, leftPos + BAR_X, topPos + FUEL_BAR_Y, SPRITE_U, PROCESS_BAR_H, fuelWidth, FUEL_BAR_H, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        if (isHovering(BAR_X, FUEL_BAR_Y, BAR_W, FUEL_BAR_H, mx, my)) {
            g.renderTooltip(font, Component.translatable("gui.shape_shifter_curse.altar.remaining_moondust",
                    formatMoondust(menu.getFuel()), formatMoondust(MAX_FUEL)), mx, my);
        } else if (menu.getCarried().isEmpty() && hoveredSlot == menu.getSlot(9) && !hoveredSlot.hasItem()) {
            g.renderTooltip(font, Component.translatable("gui.shape_shifter_curse.altar.catalyst"), mx, my);
        } else if (menu.getCarried().isEmpty() && hoveredSlot == menu.getSlot(10) && !hoveredSlot.hasItem()) {
            g.renderTooltip(font, Component.translatable("gui.shape_shifter_curse.altar.fuel"), mx, my);
        } else {
            renderTooltip(g, mx, my);
        }
    }

    private static String formatMoondust(int fuel) {
        return BigDecimal.valueOf(fuel).divide(BigDecimal.valueOf(FUEL_PER_MOONDUST), 2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }
}
