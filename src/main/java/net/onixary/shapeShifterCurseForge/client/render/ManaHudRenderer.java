package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.client.ManaClientState;
import net.onixary.shapeShifterCurseForge.other.config.SscClientConfig;

/**
 * Mana bar, Forge port of Fabric's per-type {@code IManaRender}s:
 * {@code FamiliarFoxManaBar} for familiar fox and DP mana, {@code WebResourceBar} for the
 * spider's web resource. Unknown mana types keep a plain fallback bar.
 */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class ManaHudRenderer {
    private static final String FAMILIAR_FOX_MANA = "shape-shifter-curse:familiar_fox_mana";
    private static final String DP_MANA = "shape-shifter-curse:dp_mana";
    private static final String WEB_RESOURCE = "shape-shifter-curse:web_resource";
    private static final String WEB_CHARGE_1 = "shape-shifter-curse:web_charge_1";
    private static final String WEB_CHARGE_2 = "shape-shifter-curse:web_charge_2";

    private static final ResourceLocation FOX_BAR_FULL = texture("textures/gui/familiar_fox_mana_bar_full.png");
    private static final ResourceLocation FOX_BAR_EMPTY = texture("textures/gui/familiar_fox_mana_bar_empty.png");
    private static final ResourceLocation WEB_BAR = texture("textures/gui/web_bar.png");
    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 5;
    /** Fabric's web bar text colour when the pool has no regeneration (always, in SSC data). */
    private static final int WEB_TEXT_NO_REGEN = -8421505;

    private ManaHudRenderer() {}

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, path);
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ManaClientState.available()) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int x = HudPosition.x(graphics.guiWidth(), SscClientConfig.MANA_POSITION.get(), SscClientConfig.MANA_OFFSET_X.get());
        int y = HudPosition.y(graphics.guiHeight(), SscClientConfig.MANA_POSITION.get(), SscClientConfig.MANA_OFFSET_Y.get());
        float ratio = Math.max(0.0F, Math.min(1.0F, ManaClientState.amount() / ManaClientState.maximum()));
        int filled = (int) Math.ceil(BAR_WIDTH * ratio);
        switch (ManaClientState.type()) {
            case FAMILIAR_FOX_MANA, DP_MANA -> renderFamiliarFoxBar(graphics, x, y, filled);
            case WEB_RESOURCE -> renderWebBar(graphics, minecraft, x, y, filled);
            default -> renderFallbackBar(graphics, minecraft, x, y, ratio);
        }
    }

    /** Fabric {@code FamiliarFoxManaBar}: empty 80x5 strip, then the filled part on top. */
    private static void renderFamiliarFoxBar(GuiGraphics graphics, int x, int y, int filled) {
        graphics.blit(FOX_BAR_EMPTY, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        graphics.blit(FOX_BAR_FULL, x, y, 0, 0, filled, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
    }

    /**
     * Fabric {@code WebResourceBar}: one 80x18 sheet with the empty strip (v 0), the full strip
     * (v 5) and 8x8 charge-level icons (v 10, u = tier * 8); amount text above the bar.
     */
    private static void renderWebBar(GuiGraphics graphics, Minecraft minecraft, int x, int y, int filled) {
        graphics.blit(WEB_BAR, x, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, 18);
        graphics.blit(WEB_BAR, x, y, 0, 5, filled, BAR_HEIGHT, BAR_WIDTH, 18);
        Component text = Component.literal((int) ManaClientState.amount() + "/" + (int) ManaClientState.maximum());
        graphics.drawString(minecraft.font, text, x + 10, y - 8, WEB_TEXT_NO_REGEN, false);
        int chargeLevel = Math.max(ManaClientState.chargeTier(WEB_CHARGE_1), ManaClientState.chargeTier(WEB_CHARGE_2));
        graphics.blit(WEB_BAR, x, y - 8, chargeLevel * 8, 10, 8, 8, BAR_WIDTH, 18);
    }

    /** Mana types without a Fabric renderer keep the previous plain bar. */
    private static void renderFallbackBar(GuiGraphics graphics, Minecraft minecraft, int x, int y, float ratio) {
        int height = 6;
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + height + 1, 0xCC12121A);
        graphics.fill(x, y, x + BAR_WIDTH, y + height, 0xFF312244);
        graphics.fill(x, y, x + Math.round(BAR_WIDTH * ratio), y + height, 0xFF79D9FF);
        String text = String.format(java.util.Locale.ROOT, "Mana %.0f / %.0f",
                ManaClientState.amount(), ManaClientState.maximum());
        graphics.drawString(minecraft.font, text, x + (BAR_WIDTH - minecraft.font.width(text)) / 2, y - 10, 0xD7F7FF, true);
    }
}
