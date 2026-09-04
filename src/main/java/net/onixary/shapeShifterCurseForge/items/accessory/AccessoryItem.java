package net.onixary.shapeShifterCurseForge.items.accessory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public abstract class AccessoryItem extends Item {
    public static record SlotData(ResourceLocation slot, int index) {}

    public static enum DropRule {
        KEEP, DROP, DESTROY, DEFAULT
    }

    public AccessoryItem(Properties properties) {
        super(properties);
        this.accessoryInit(properties);
    }

    public void accessoryInit(Properties properties) {
        return;
    }

    public void accessoryTick(ItemStack stack, LivingEntity accessoryOwner, SlotData slotData) {
        return;
    }

    public void onEquip(ItemStack stack, LivingEntity accessoryOwner, SlotData slotData) {
        return;
    }

    public void onUnequip(ItemStack stack, LivingEntity accessoryOwner, SlotData slotData) {
        return;
    }

    public boolean canEquip(ItemStack stack, LivingEntity entity, SlotData slotData) {
        return true;
    }

    public boolean canUnequip(ItemStack stack, LivingEntity entity, SlotData slotData) {
        return !EnchantmentHelper.hasBindingCurse(stack);
    }

    public void onBreak(ItemStack stack, LivingEntity entity, SlotData slotData) {
        return;
    }

    public DropRule getDropRule(ItemStack stack, LivingEntity entity, SlotData slotData) {
        return DropRule.DEFAULT;
    }
}
