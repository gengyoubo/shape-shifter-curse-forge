package net.onixary.shapeShifterCurseForge.items.trinkets;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CharmOfHollowFangTrinket extends AccessoryItem {
    public CharmOfHollowFangTrinket(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.shape-shifter-curse.charm_of_hollow_fang.tooltip").withStyle(ChatFormatting.YELLOW));
    }
}
