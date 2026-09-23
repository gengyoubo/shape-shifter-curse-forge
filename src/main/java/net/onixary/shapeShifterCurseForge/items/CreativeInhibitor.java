package net.onixary.shapeShifterCurseForge.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.power.TransformativeEffectService;

import java.util.List;

/**
 * Creative-only inhibitor: always-edible and reverses the player to the original
 * shifter form while clearing any pending transformative effect (Fabric parity).
 */
public class CreativeInhibitor extends Item {
    public CreativeInhibitor(Properties properties) {
        super(properties.stacksTo(16).food(new FoodProperties.Builder()
                .nutrition(2)
                .saturationMod(0.3F)
                .alwaysEat()
                .build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof ServerPlayer player) {
            FormManager.setForm(player, FormRegistry.ORIGINAL_SHIFTER, false);
            TransformativeEffectService.clear(player);
        }
        ItemStack remaining = super.finishUsingItem(stack, level, user);
        if (user instanceof Player player && player.getAbilities().instabuild) {
            return remaining;
        }
        return new ItemStack(Items.BOWL);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.shape-shifter-curse.creative_inhibitor.tooltip")
                .withStyle(ChatFormatting.YELLOW));
    }
}