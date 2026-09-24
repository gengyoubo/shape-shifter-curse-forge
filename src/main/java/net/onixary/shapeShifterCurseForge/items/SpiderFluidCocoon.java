package net.onixary.shapeShifterCurseForge.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/** Nutrient Sac: edible fluid with a poison drawback (Fabric parity). */
public class SpiderFluidCocoon extends Item {
    public SpiderFluidCocoon(Properties properties) {
        super(properties.stacksTo(64).food(new FoodProperties.Builder()
                .nutrition(6)
                .saturationMod(0.8F)
                .effect(() -> new MobEffectInstance(MobEffects.POISON, 150, 0), 1.0F)
                .build()));
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.shape-shifter-curse.spider_fluid_cocoon.tooltip")
                .withStyle(ChatFormatting.YELLOW));
    }
}
