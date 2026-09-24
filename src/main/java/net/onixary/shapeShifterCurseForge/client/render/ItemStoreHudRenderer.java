package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.other.config.SscClientConfig;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import net.onixary.shapeShifterCurseForge.power.ItemStoreService;
import net.onixary.shapeShifterCurseForge.util.Accessory.AccessoryUtils;

import java.util.HashMap;
import java.util.Map;

/** Shared 12-slot HUD for item_store and render_accessory_slot, as in Fabric. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class ItemStoreHudRenderer {
    private ItemStoreHudRenderer() { }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) return;
        Map<Integer, Entry> entries = new HashMap<>();
        CompoundTag stores = SscApi.currentForm(player).map(data -> data.getItemStores()).orElse(new CompoundTag());
        FormPowerRegistry.visitActive(player, (id, power) -> {
            String type = FormPowerRegistry.typeOf(power);
            if (!"shape-shifter-curse:item_store".equals(type)
                    && !"shape-shifter-curse:render_accessory_slot".equals(type)) return;
            if (!FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            int slot = FormPowerRuntime.intValue(power, "slot", 0);
            if (slot < 0 || slot >= 12) return;
            if ("shape-shifter-curse:item_store".equals(type)) {
                entries.put(slot, new Entry(slot, ItemStoreService.get(player, id),
                        stores.getCompound(id.toString()).getInt("Bobbing")));
            } else {
                int index = FormPowerRuntime.intValue(power, "accessory_slot_index", 0);
                ItemStack stack = index < 0 ? ItemStack.EMPTY : AccessoryUtils.getEntitySlot(player,
                        FormPowerRuntime.stringValue(power, "accessory_mod", "auto"),
                        FormPowerRuntime.stringValue(power, "accessory_group", ""),
                        FormPowerRuntime.stringValue(power, "accessory_slot", ""),
                        index);
                entries.put(slot, new Entry(slot, stack == null ? ItemStack.EMPTY : stack, 0));
            }
        });
        if (entries.isEmpty()) return;
        int columns = entries.values().stream().mapToInt(entry -> entry.slot % 4 + 1).max().orElse(1);
        int rows = entries.values().stream().mapToInt(entry -> entry.slot / 4 + 1).max().orElse(1);
        GuiGraphics graphics = event.getGuiGraphics();
        int anchor = SscClientConfig.ITEM_STORE_POSITION.get();
        int x = switch ((anchor - 1) % 3) {
            case 0 -> 0;
            case 2 -> graphics.guiWidth();
            default -> graphics.guiWidth() / 2;
        };
        int y = switch ((anchor - 1) / 3) {
            case 0 -> 0;
            case 2 -> graphics.guiHeight();
            default -> graphics.guiHeight() / 2;
        };
        x += SscClientConfig.ITEM_STORE_OFFSET_X.get() - columns * 20;
        y += SscClientConfig.ITEM_STORE_OFFSET_Y.get() - rows * 20;
        for (Entry entry : entries.values()) {
            int slotX = x + entry.slot % 4 * 20;
            int slotY = y + entry.slot / 4 * 20;
            graphics.fill(slotX - 2, slotY - 3, slotX + 18, slotY + 18, 0xAA171717);
            graphics.fill(slotX - 2, slotY - 3, slotX + 18, slotY - 2, 0xFF777777);
            ItemStack stack = entry.stack;
            if (stack.isEmpty()) continue;
            if (entry.bobbing > 0) {
                float height = 1.0F + entry.bobbing / 5.0F;
                graphics.pose().pushPose();
                graphics.pose().translate(slotX + 8, slotY + 12, 0.0F);
                graphics.pose().scale(1.0F / height, (height + 1.0F) / 2.0F, 1.0F);
                graphics.pose().translate(-(slotX + 8), -(slotY + 12), 0.0F);
            }
            graphics.renderItem(stack, slotX, slotY);
            if (entry.bobbing > 0) graphics.pose().popPose();
            graphics.renderItemDecorations(minecraft.font, stack, slotX, slotY);
        }
    }

    private record Entry(int slot, ItemStack stack, int bobbing) { }
}
