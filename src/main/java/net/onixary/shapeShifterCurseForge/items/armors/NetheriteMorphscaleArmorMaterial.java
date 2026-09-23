package net.onixary.shapeShifterCurseForge.items.armors;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Forge port of Fabric's {@code NetheriteMorphscaleArmorMaterial}: netherite-tier
 * armor with the moondust durability profile and knockback resistance.
 */
public final class NetheriteMorphscaleArmorMaterial implements ArmorMaterial {
    public static final NetheriteMorphscaleArmorMaterial INSTANCE = new NetheriteMorphscaleArmorMaterial();

    private static final int[] BASE_DURABILITY = new int[]{462, 555, 592, 481};
    private static final int[] PROTECTION_VALUES = new int[]{3, 6, 7, 3};

    private NetheriteMorphscaleArmorMaterial() {
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
        return 15;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.NETHERITE_SCRAP);
    }

    @Override
    public String getName() {
        return "netherite";
    }

    @Override
    public float getToughness() {
        return 2.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.1F;
    }
}