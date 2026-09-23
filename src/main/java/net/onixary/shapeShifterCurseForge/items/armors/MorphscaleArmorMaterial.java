package net.onixary.shapeShifterCurseForge.items.armors;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Forge port of Fabric's {@code MorphscaleArmorMaterial}: diamond-tier armor with
 * the moondust durability profile.
 */
public final class MorphscaleArmorMaterial implements ArmorMaterial {
    public static final MorphscaleArmorMaterial INSTANCE = new MorphscaleArmorMaterial();

    private static final int[] BASE_DURABILITY = new int[]{429, 495, 528, 363};
    private static final int[] PROTECTION_VALUES = new int[]{2, 6, 7, 2};

    private MorphscaleArmorMaterial() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> BASE_DURABILITY[0];
            case LEGGINGS -> BASE_DURABILITY[1];
            case CHESTPLATE -> BASE_DURABILITY[2];
            case HELMET -> BASE_DURABILITY[3];
        };
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> PROTECTION_VALUES[0];
            case LEGGINGS -> PROTECTION_VALUES[1];
            case CHESTPLATE -> PROTECTION_VALUES[2];
            case BOOTS -> PROTECTION_VALUES[3];
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 10;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_DIAMOND;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.DIAMOND);
    }

    @Override
    public String getName() {
        return "diamond";
    }

    @Override
    public float getToughness() {
        return 1.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}