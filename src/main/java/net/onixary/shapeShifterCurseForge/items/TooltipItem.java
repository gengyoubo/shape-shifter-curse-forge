package net.onixary.shapeShifterCurseForge.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class TooltipItem extends Item {
    private final String tooltipKey;
    private final ChatFormatting formatting;

    public TooltipItem(Properties properties, String tooltipKey, ChatFormatting formatting) {
        super(properties);
        this.tooltipKey = tooltipKey;
        this.formatting = formatting;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey).withStyle(formatting));
    }
}
