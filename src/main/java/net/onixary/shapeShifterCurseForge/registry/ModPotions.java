package net.onixary.shapeShifterCurseForge.registry;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

/** Forge registration and brewing paths corresponding to Fabric RegCustomPotions. */
public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(
            ForgeRegistries.POTIONS, ShapeShifterCurseForge.RESOURCE_NAMESPACE);

    public static final RegistryObject<Potion> MOONDUST = POTIONS.register("moondust_potion", Potion::new);
    public static final RegistryObject<Potion> TO_BAT = effectPotion("to_bat_0_potion", ModEffects.TO_BAT);
    public static final RegistryObject<Potion> TO_AXOLOTL = effectPotion("to_axolotl_0_potion", ModEffects.TO_AXOLOTL);
    public static final RegistryObject<Potion> TO_OCELOT = effectPotion("to_ocelot_0_potion", ModEffects.TO_OCELOT);
    public static final RegistryObject<Potion> TO_FAMILIAR_FOX = effectPotion("to_familiar_fox_0_potion", ModEffects.TO_FAMILIAR_FOX);
    public static final RegistryObject<Potion> TO_SNOW_FOX = effectPotion("to_snow_fox_0_potion", ModEffects.TO_SNOW_FOX);
    public static final RegistryObject<Potion> TO_ANUBIS_WOLF = effectPotion("to_anubis_wolf_0_potion", ModEffects.TO_ANUBIS_WOLF);
    public static final RegistryObject<Potion> TO_SPIDER = effectPotion("to_spider_0_potion", ModEffects.TO_SPIDER);
    public static final RegistryObject<Potion> TO_ALLAY = effectPotion("to_allay_sp_potion", ModEffects.TO_ALLAY);
    public static final RegistryObject<Potion> TO_FERAL_CAT = effectPotion("to_feral_cat_sp_potion", ModEffects.TO_FERAL_CAT);
    public static final RegistryObject<Potion> FEED = effectPotion("feed_potion", ModEffects.FEED);

    private static RegistryObject<Potion> effectPotion(String id, RegistryObject<net.minecraft.world.effect.MobEffect> effect) {
        return POTIONS.register(id, () -> new Potion(new MobEffectInstance(effect.get(), 3600)));
    }

    public static void registerBrewing() {
        add(Potions.AWKWARD, ModItems.MOONDUST_MATRIX.get(), MOONDUST.get());
        add(MOONDUST.get(), Items.POINTED_DRIPSTONE, TO_BAT.get());
        add(MOONDUST.get(), Items.BIG_DRIPLEAF, TO_AXOLOTL.get());
        add(MOONDUST.get(), Items.CHICKEN, TO_OCELOT.get());
        add(MOONDUST.get(), ModItems.ECTOPLASM_RAG.get(), TO_ANUBIS_WOLF.get());
        add(MOONDUST.get(), ModItems.SILK_DEW.get(), TO_SPIDER.get());
        add(TO_FAMILIAR_FOX.get(), Items.GOLD_NUGGET, TO_SNOW_FOX.get());
        add(MOONDUST.get(), Items.AMETHYST_SHARD, TO_ALLAY.get());
        add(MOONDUST.get(), Items.COD_BUCKET, TO_FERAL_CAT.get());
    }

    private static void add(Potion input, net.minecraft.world.item.Item ingredient, Potion output) {
        BrewingRecipeRegistry.addRecipe(new BrewingRecipe(
                Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), input)),
                Ingredient.of(ingredient), PotionUtils.setPotion(new ItemStack(Items.POTION), output)));
    }

    private ModPotions() {}
}
