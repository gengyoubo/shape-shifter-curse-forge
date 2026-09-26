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
    private static final java.util.UUID MOD_SPEED_UUID = java.util.UUID.nameUUIDFromBytes(
            "ssc.entangled_full_speed".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final java.util.UUID MOD_KNOCKBACK_UUID = java.util.UUID.nameUUIDFromBytes(
            "ssc.entangled_full_knockback".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final java.util.UUID MOD_ATTACK_UUID = java.util.UUID.nameUUIDFromBytes(
            "ssc.entangled_full_attack_speed".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    public static final RegistryObject<MobEffect> ENTANGLED_FULL = EFFECTS.register("entangled_full_effect", () -> new MobEffect(MobEffectCategory.HARMFUL, 0xFFFFFF) {}
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, MOD_SPEED_UUID.toString(), -1D, AttributeModifier.Operation.MULTIPLY_BASE)
            .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, MOD_KNOCKBACK_UUID.toString(), 100D, AttributeModifier.Operation.ADDITION)
            .addAttributeModifier(Attributes.ATTACK_SPEED, MOD_ATTACK_UUID.toString(), -0.8D, AttributeModifier.Operation.MULTIPLY_BASE));
    // Transformative effects target FormRegistry IDs. The "form_" prefix belongs
    // to Origin resource IDs and is not part of the form ID.
    public static final RegistryObject<MobEffect> TO_BAT = transform("to_bat_0_effect", "bat_0");
    public static final RegistryObject<MobEffect> TO_AXOLOTL = transform("to_axolotl_0_effect", "axolotl_0");
    public static final RegistryObject<MobEffect> TO_OCELOT = transform("to_ocelot_0_effect", "ocelot_0");
    public static final RegistryObject<MobEffect> TO_FAMILIAR_FOX = transform("to_familiar_fox_0_effect", "familiar_fox_0");
    public static final RegistryObject<MobEffect> TO_SNOW_FOX = transform("to_snow_fox_0_effect", "snow_fox_0");
    public static final RegistryObject<MobEffect> TO_ANUBIS_WOLF = transform("to_anubis_wolf_0_effect", "anubis_wolf_0");
    public static final RegistryObject<MobEffect> TO_SPIDER = transform("to_spider_0_effect", "spider_0");
    public static final RegistryObject<MobEffect> TO_ALLAY = transform("to_allay_sp_effect", "allay_sp");
    public static final RegistryObject<MobEffect> TO_FERAL_CAT = transform("to_feral_cat_sp_effect", "feral_cat_sp");

    private static RegistryObject<MobEffect> transform(String id, String form) { return EFFECTS.register(id, () -> new TransformativeStatusEffect(ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, form))); }
    private ModEffects() {}
}
