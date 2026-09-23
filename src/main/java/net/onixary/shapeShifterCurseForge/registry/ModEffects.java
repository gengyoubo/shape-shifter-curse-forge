package net.onixary.shapeShifterCurseForge.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.effect.FeedEffect;
import net.onixary.shapeShifterCurseForge.effect.EntangledEffect;
import net.onixary.shapeShifterCurseForge.effect.ImmobilityEffect;
import net.onixary.shapeShifterCurseForge.effect.TransformativeStatusEffect;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ShapeShifterCurseForge.RESOURCE_NAMESPACE);
    public static final RegistryObject<MobEffect> IMMOBILITY = EFFECTS.register("immobility_effect", ImmobilityEffect::new);
    public static final RegistryObject<MobEffect> FEED = EFFECTS.register("feed_effect", FeedEffect::new);
    public static final RegistryObject<MobEffect> ENTANGLED = EFFECTS.register("entangled_effect", EntangledEffect::new);
    public static final RegistryObject<MobEffect> ENTANGLED_FULL = EFFECTS.register("entangled_full_effect", () -> new MobEffect(MobEffectCategory.HARMFUL, 0xFFFFFF) {}
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "shape-shifter-curse.entangled_full_speed", -1D, AttributeModifier.Operation.MULTIPLY_BASE)
            .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, "shape-shifter-curse.entangled_full_knockback", 100D, AttributeModifier.Operation.ADDITION)
            .addAttributeModifier(Attributes.ATTACK_SPEED, "shape-shifter-curse.entangled_full_attack_speed", -0.8D, AttributeModifier.Operation.MULTIPLY_BASE));
    public static final RegistryObject<MobEffect> TO_BAT = transform("to_bat_0_effect", "form_bat_0");
    public static final RegistryObject<MobEffect> TO_AXOLOTL = transform("to_axolotl_0_effect", "form_axolotl_0");
    public static final RegistryObject<MobEffect> TO_OCELOT = transform("to_ocelot_0_effect", "form_ocelot_0");
    public static final RegistryObject<MobEffect> TO_FAMILIAR_FOX = transform("to_familiar_fox_0_effect", "form_familiar_fox_0");
    public static final RegistryObject<MobEffect> TO_SNOW_FOX = transform("to_snow_fox_0_effect", "form_snow_fox_0");
    public static final RegistryObject<MobEffect> TO_ANUBIS_WOLF = transform("to_anubis_wolf_0_effect", "form_anubis_wolf_0");
    public static final RegistryObject<MobEffect> TO_SPIDER = transform("to_spider_0_effect", "form_spider_0");
    public static final RegistryObject<MobEffect> TO_ALLAY = transform("to_allay_sp_effect", "form_allay_sp");
    public static final RegistryObject<MobEffect> TO_FERAL_CAT = transform("to_feral_cat_sp_effect", "form_feral_cat_sp");
    private static RegistryObject<MobEffect> transform(String id, String form) { return EFFECTS.register(id, () -> new TransformativeStatusEffect(ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, form))); }
    private ModEffects() {}
}
