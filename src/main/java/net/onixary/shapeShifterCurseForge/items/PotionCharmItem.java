package net.onixary.shapeShifterCurseForge.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.entity.PotionCharmArrowEntity;
import net.onixary.shapeShifterCurseForge.registry.ModEntities;

public final class PotionCharmItem extends Item {
    private final Potion potion;

    public PotionCharmItem(Potion potion) {
        super(new Item.Properties());
        this.potion = potion;
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean familiarFox = SscApi.currentForm(player)
                .map(data -> "shape-shifter-curse:familiar_fox_3".equals(data.getFormId())).orElse(false);
        if (!familiarFox || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            PotionCharmArrowEntity arrow = ModEntities.POTION_CHARM_ARROW.get().create(level);
            if (arrow == null) return InteractionResultHolder.fail(stack);
            arrow.setOwner(player);
            arrow.setCharmStack(stack);
            arrow.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            arrow.setEffectsFromItem(PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), potion));
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 3.0F, 0);
            if (!level.addFreshEntity(arrow)) return InteractionResultHolder.fail(stack);
            player.getCooldowns().addCooldown(this, 20);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
