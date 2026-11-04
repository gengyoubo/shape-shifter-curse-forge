package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.client.FormKeyInputEvents;
import net.onixary.shapeShifterCurseForge.client.FormKeyMappings;
import net.onixary.shapeShifterCurseForge.network.ActivePowerKeyPacket;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.other.config.SscClientConfig;

/** Six-slot Fabric skill selector and its three visible sprites. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class MagicHudRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/perk/textures.png");
    private static int selected, held = -1;
    private MagicHudRenderer() {}
    private static String key(int index) { return "key.shape-shifter-curse.active_skill_" + (index + 1); }
    private static void change(int delta) { selected = Math.floorMod(selected + delta, 6); }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) { held = -1; return; }
        if (mc.screen == null) {
            while (FormKeyMappings.PREVIOUS_SKILL.consumeClick()) change(-1);
            while (FormKeyMappings.NEXT_SKILL.consumeClick()) change(1);
        }
        held = mc.screen == null && FormKeyMappings.SELECTED_SKILL.isDown() ? selected : -1;
    }
    public static boolean virtualPressed(int index) { return held == index; }
    @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !FormKeyMappings.SCROLL_SKILL.isDown() || event.getScrollDelta() == 0) return;
        change(event.getScrollDelta() > 0 ? -1 : 1); event.setCanceled(true);
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance(); if (mc.player == null || mc.options.hideGui) return;
        var g = event.getGuiGraphics();
        int x = HudPosition.x(g.guiWidth(), SscClientConfig.MAGIC_POSITION.get(), SscClientConfig.MAGIC_OFFSET_X.get());
        int y = HudPosition.y(g.guiHeight(), SscClientConfig.MAGIC_POSITION.get(), SscClientConfig.MAGIC_OFFSET_Y.get());
        int[] slots = { Math.floorMod(selected - 1, 6), selected, (selected + 1) % 6 };
        for (int i = 0; i < 3; i++) {
            int size = i == 1 ? 24 : 16, px = x + (i == 0 ? 0 : i == 1 ? 18 : 44), py = y + (i == 1 ? 0 : 4);
            g.pose().pushPose(); g.pose().translate(px, py, 0); g.pose().scale(size / 16f, size / 16f, 1);
            g.blit(TEXTURE, 0, 0, (slots[i] + (i == 1 ? 1 : 7)) * 16, 0, 16, 16, 512, 512);
            g.pose().popPose();
        }
    }
}
