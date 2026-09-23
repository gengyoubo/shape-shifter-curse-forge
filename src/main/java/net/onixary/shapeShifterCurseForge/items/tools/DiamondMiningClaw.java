package net.onixary.shapeShifterCurseForge.items.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Bat form tool. Mining efficiency and damage come from the form's powers; the
 * pickaxe grants only the durability profile (Fabric parity).
 */
public class DiamondMiningClaw extends PickaxeItem {
    public DiamondMiningClaw(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.shape-shifter-curse.diamond_mining_claw.tooltip")
                .withStyle(ChatFormatting.YELLOW));
    }
}