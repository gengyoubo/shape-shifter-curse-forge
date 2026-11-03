package net.onixary.shapeShifterCurseForge.items.trinkets;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;

/** The beta form accessories use their own item ID for the description. */
public final class FormTrinket extends AccessoryItem {
    public FormTrinket(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.YELLOW));
    }
}
