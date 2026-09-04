package net.onixary.shapeShifterCurseForge.items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.client.codex.BookOfShapeShifterScreenV2_P1;
import net.onixary.shapeShifterCurseForge.client.codex.StartBookScreenV2;
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

        if (level.isClientSide) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> openBookScreen(player));
        }
        // Server side intentionally does nothing here: enabling moved to the
        // StartBook confirm button (ValidateStartBookPacket), mirroring Fabric.
        // TODO: ON_OPEN_BOOK_OF_SHAPE_SHIFTER advancement trigger once triggers land.

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    private static void openBookScreen(Player player) {
        if (FormRegistry.ORIGINAL_BEFORE_ENABLE.equals(FormManager.current(player).id())) {
            Minecraft.getInstance().setScreen(new StartBookScreenV2(player));
        } else {
            Minecraft.getInstance().setScreen(new BookOfShapeShifterScreenV2_P1(player));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "item.shape-shifter-curse.book_of_shape_shifter.tooltip"
        ).withStyle(ChatFormatting.GRAY));
    }
}
