package net.onixary.shapeShifterCurseForge.items.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Extra-hand sword: boosts melee attack speed, consumes extra durability (Fabric parity). */
public class AuxiliarySword extends SwordItem {
    public AuxiliarySword(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.shape-shifter-curse.auxiliary_sword.tooltip")
                .withStyle(ChatFormatting.YELLOW));
    }
}