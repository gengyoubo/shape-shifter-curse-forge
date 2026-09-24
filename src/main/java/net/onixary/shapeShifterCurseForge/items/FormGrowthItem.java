package net.onixary.shapeShifterCurseForge.items;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.form.FormGrowthService;

/** Catalyst and inhibitor item backed by the Forge-native form growth rules. */
public final class FormGrowthItem extends Item {
    private final FormGrowthService.Mode mode;

    public FormGrowthItem(Properties properties, FormGrowthService.Mode mode) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(2)
                .saturationMod(0.3F)
                .build()));
        this.mode = mode;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide && user instanceof ServerPlayer serverPlayer) {
            FormGrowthService.apply(serverPlayer, mode);
        }
        return super.finishUsingItem(stack, level, user);
    }
}
