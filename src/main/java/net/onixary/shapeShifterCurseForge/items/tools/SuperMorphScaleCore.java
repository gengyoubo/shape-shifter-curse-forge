package net.onixary.shapeShifterCurseForge.items.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Vanishable;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Forge port of Fabric's {@code SuperMorphScaleCore}: hold to consume experience
 * and repair the core (or all of it when sneaking). Snapshot durability grants
 * the "remaining charge" tooltip.
 */
public class SuperMorphScaleCore extends Item implements Vanishable {
    public static final int DAMAGE_PER_ITEM = 64;
    public static final float MENDING_MULTIPLIER = 2.0F;
    public static final float QUICK_CHARGE_COST_MULTIPLIER = 0.75F;
    public static final float QUICK_CHARGE_COST_MULTIPLIER_NO_MENDING = 0.20F;

    public SuperMorphScaleCore(Properties properties) {
        super(properties);
    }

    public static int getMaxUseCount(ItemStack stack, int multiplier) {
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        int perCount = DAMAGE_PER_ITEM * multiplier;
        return remaining / perCount;
    }

    public static int getUpgradeDamageMultiplier(ItemStack stack) {
        int maxStack = stack.getMaxStackSize();
        if (maxStack == 0) {
            return 1;
        }
        return 64 / maxStack;
    }

    public static void damageItemAfterUpgrade(ItemStack stack, int multiplier) {
        int perCount = DAMAGE_PER_ITEM * multiplier;
        int target = stack.getDamageValue() + perCount;
        stack.setDamageValue(Math.min(target, stack.getMaxDamage()));
    }

    public static int getTotalExperience(int level, int xp) {
        int totalExp;
        if (level <= 16) {
            totalExp = level * level + 6 * level;
        } else if (level <= 31) {
            totalExp = (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            totalExp = (int) (4.5 * level * level - 162.5 * level + 2220);
        }
        int sum = totalExp + xp;
        return sum < 0 ? totalExp : sum;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 24;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof Player player && !level.isClientSide) {
            int damage = stack.getDamageValue();
            int needRepair = user.isCrouching() ? stack.getMaxDamage() : DAMAGE_PER_ITEM;
            needRepair = Math.min(needRepair, damage);
            float expMultiplier = MENDING_MULTIPLIER;
            if (EnchantmentHelper.getTagEnchantmentLevel(Enchantments.MENDING, stack) > 0) {
                expMultiplier *= QUICK_CHARGE_COST_MULTIPLIER;
            } else {
                expMultiplier *= QUICK_CHARGE_COST_MULTIPLIER_NO_MENDING;
            }
            int playerExp = getTotalExperience(player.experienceLevel,
                    (int) (player.experienceProgress * (float) player.getXpNeededForNextLevel()));
            int maxRepair = (int) Math.floor(playerExp * expMultiplier);
            needRepair = Math.min(needRepair, maxRepair);
            int expCost = (int) Math.ceil(needRepair / expMultiplier);
            if (needRepair > 0) {
                stack.setDamageValue(Math.max(0, damage - needRepair));
                player.giveExperiencePoints(-expCost);
                player.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.shape-shifter-curse.super_morphscale_core.tooltip",
                getMaxUseCount(stack, 1)).withStyle(ChatFormatting.DARK_PURPLE));
    }
}