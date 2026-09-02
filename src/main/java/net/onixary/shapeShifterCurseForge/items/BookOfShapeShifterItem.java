package net.onixary.shapeShifterCurseForge.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.capability.ModCapabilities;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;

import java.util.List;

public final class BookOfShapeShifterItem extends Item {
    public BookOfShapeShifterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            player.getCapability(ModCapabilities.PLAYER_FORM).ifPresent(formData -> {
                if (FormRegistry.ORIGINAL_BEFORE_ENABLE.equals(net.minecraft.resources.ResourceLocation.tryParse(formData.getFormId()))) {
                    FormManager.setForm(player, FormRegistry.ORIGINAL_SHIFTER);
                    player.sendSystemMessage(Component.translatable(
                            "info.shape-shifter-curse.on_enable_mod"
                    ).withStyle(ChatFormatting.LIGHT_PURPLE));
                } else {
                    player.sendSystemMessage(Component.translatable(
                            "info.shape-shifter-curse.on_enable_mod_after"
                    ).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            });
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "item.shape-shifter-curse.book_of_shape_shifter.tooltip"
        ).withStyle(ChatFormatting.GRAY));
    }
}
