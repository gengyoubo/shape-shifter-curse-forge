package net.onixary.shapeShifterCurseForge.items;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.form.FormGrowthService;

/** Catalyst and inhibitor item backed by the Forge-native form growth rules. */
public final class FormGrowthItem extends Item {
    private final FormGrowthService.Mode mode;

    public FormGrowthItem(Properties properties, FormGrowthService.Mode mode) {
        super(properties);
        this.mode = mode;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && FormGrowthService.apply(serverPlayer, mode) && !serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
