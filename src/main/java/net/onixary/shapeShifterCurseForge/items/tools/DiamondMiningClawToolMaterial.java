package net.onixary.shapeShifterCurseForge.items.tools;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Forge port of Fabric's {@code DiamondMiningClawToolMaterial}. Stone-pick speed,
 * diamond mining level: Bat form tool.
 */
public final class DiamondMiningClawToolMaterial implements Tier {
    public static final DiamondMiningClawToolMaterial INSTANCE = new DiamondMiningClawToolMaterial();

    private DiamondMiningClawToolMaterial() {
    }

    @Override
    public int getUses() {
        return 781;
    }

    @Override
    public float getSpeed() {
        return 4.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 2.0F;
    }

    @Override
    public int getLevel() {
        return 3;
    }

    @Override
    public int getEnchantmentValue() {
        return 10;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.DIAMOND);
    }
}