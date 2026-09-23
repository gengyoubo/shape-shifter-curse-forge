package net.onixary.shapeShifterCurseForge.items.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Snow Fox form tool. Real behavior (snowball launching) comes from the form's
 * powers; the sword grants only the durability profile (Fabric parity).
 */
public class BottledSnowfall extends SwordItem {
    public BottledSnowfall(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.shape-shifter-curse.bottled_snowfall.tooltip")
                .withStyle(ChatFormatting.YELLOW));
    }
}