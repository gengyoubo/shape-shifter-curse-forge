package net.onixary.shapeShifterCurseForge.items.tools;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Forge port of Fabric's {@code AuxiliarySwordToolMaterial}. No mining level,
 * plain attack bonus: extra-hand sword.
 */
public final class AuxiliarySwordToolMaterial implements Tier {
    public static final AuxiliarySwordToolMaterial INSTANCE = new AuxiliarySwordToolMaterial();

    private AuxiliarySwordToolMaterial() {
    }

    @Override
    public int getUses() {
        return 781;
    }

    @Override
    public float getSpeed() {
        return 1.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 4.0F;
    }

    @Override
    public int getLevel() {
        return 0;
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.DIAMOND);
    }
}