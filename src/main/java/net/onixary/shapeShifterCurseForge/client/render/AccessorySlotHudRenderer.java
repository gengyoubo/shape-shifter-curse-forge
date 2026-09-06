package net.onixary.shapeShifterCurseForge.client.render;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import net.onixary.shapeShifterCurseForge.util.Accessory.AccessoryUtils;

import java.util.ArrayList;
import java.util.List;

/** Forge HUD equivalent of Fabric's render_accessory_slot power. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class AccessorySlotHudRenderer {
    private static final int SLOT_SIZE = 20;
    private static final int COLUMNS = 4;

    private AccessorySlotHudRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) return;

        List<SlotEntry> entries = new ArrayList<>();
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:render_accessory_slot".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            String mod = FormPowerRuntime.stringValue(power, "accessory_mod", "auto");
            String group = FormPowerRuntime.stringValue(power, "accessory_group", "");
            String slot = FormPowerRuntime.stringValue(power, "accessory_slot", "");
            int index = FormPowerRuntime.intValue(power, "accessory_slot_index", 0);
            int position = Math.max(0, FormPowerRuntime.intValue(power, "slot", 0));
            ItemStack stack = AccessoryUtils.getEntitySlot(player, mod, group, slot, index);
            if (stack != null && !stack.isEmpty()) entries.add(new SlotEntry(position, stack));
        });
        if (entries.isEmpty()) return;

        entries.sort((left, right) -> Integer.compare(left.position, right.position));
        GuiGraphics graphics = event.getGuiGraphics();
        int maxPosition = entries.stream().mapToInt(SlotEntry::position).max().orElse(0);
        int rows = Math.max(1, maxPosition / COLUMNS + 1);
        int left = 4;
        int top = graphics.guiHeight() - rows * SLOT_SIZE - 4;
        for (SlotEntry entry : entries) {
            int x = left + (entry.position % COLUMNS) * SLOT_SIZE;
            int y = top + (entry.position / COLUMNS) * SLOT_SIZE;
            graphics.fill(x - 2, y - 2, x + 18, y + 18, 0x88000000);
            graphics.renderItem(entry.stack, x, y);
            graphics.renderItemDecorations(minecraft.font, entry.stack, x, y);
        }
    }

    private record SlotEntry(int position, ItemStack stack) {
    }
}
