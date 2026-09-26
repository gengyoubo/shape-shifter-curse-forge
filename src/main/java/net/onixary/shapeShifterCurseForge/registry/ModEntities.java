package net.onixary.shapeShifterCurseForge.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.monster.Spider;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.entity.WebBulletEntity;
import net.onixary.shapeShifterCurseForge.entity.AnubisWolfMinionEntity;
import net.onixary.shapeShifterCurseForge.entity.TransformativeAxolotlEntity;
import net.onixary.shapeShifterCurseForge.entity.TransformativeBatEntity;
import net.onixary.shapeShifterCurseForge.entity.TransformativeOcelotEntity;
import net.onixary.shapeShifterCurseForge.entity.TransformativeSpiderEntity;
import net.onixary.shapeShifterCurseForge.entity.TransformativeWolfEntity;

@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(
            ForgeRegistries.ENTITY_TYPES, ShapeShifterCurseForge.RESOURCE_NAMESPACE);

    public static final RegistryObject<EntityType<WebBulletEntity>> WEB_BULLET = ENTITIES.register("web_bullet",
            () -> EntityType.Builder.<WebBulletEntity>of(WebBulletEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(1).build("web_bullet"));

    public static final RegistryObject<EntityType<AnubisWolfMinionEntity>> ANUBIS_WOLF_MINION = ENTITIES.register(
            "anubis_wolf_minion", () -> EntityType.Builder.of(AnubisWolfMinionEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).build("anubis_wolf_minion"));

    public static final RegistryObject<EntityType<TransformativeBatEntity>> TRANSFORMATIVE_BAT = ENTITIES.register("transformative_bat",
            () -> EntityType.Builder.of(TransformativeBatEntity::new, MobCategory.AMBIENT).sized(0.5F, 0.9F).build("transformative_bat"));
    public static final RegistryObject<EntityType<TransformativeAxolotlEntity>> TRANSFORMATIVE_AXOLOTL = ENTITIES.register("transformative_axolotl",
            () -> EntityType.Builder.of(TransformativeAxolotlEntity::new, MobCategory.AXOLOTLS).sized(0.75F, 0.42F).build("transformative_axolotl"));
    public static final RegistryObject<EntityType<TransformativeOcelotEntity>> TRANSFORMATIVE_OCELOT = ENTITIES.register("transformative_ocelot",
            () -> EntityType.Builder.of(TransformativeOcelotEntity::new, MobCategory.MONSTER).sized(0.6F, 0.7F).build("transformative_ocelot"));
    public static final RegistryObject<EntityType<TransformativeSpiderEntity>> TRANSFORMATIVE_SPIDER = ENTITIES.register("transformative_spider",
            () -> EntityType.Builder.of(TransformativeSpiderEntity::new, MobCategory.MONSTER).sized(0.7F, 0.45F).build("transformative_spider"));
    public static final RegistryObject<EntityType<TransformativeWolfEntity>> TRANSFORMATIVE_WOLF = ENTITIES.register("transformative_wolf",
            () -> EntityType.Builder.of(TransformativeWolfEntity::new, MobCategory.CREATURE).sized(0.6F, 0.85F).build("transformative_wolf"));

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ANUBIS_WOLF_MINION.get(), AnubisWolfMinionEntity.createAttributes().build());
        event.put(TRANSFORMATIVE_BAT.get(), TransformativeBatEntity.createAttributes().build());
        event.put(TRANSFORMATIVE_AXOLOTL.get(), TransformativeAxolotlEntity.createAttributes().build());
        event.put(TRANSFORMATIVE_OCELOT.get(), TransformativeOcelotEntity.createAttributes().build());
        event.put(TRANSFORMATIVE_SPIDER.get(), TransformativeSpiderEntity.createAttributes().build());
        event.put(TRANSFORMATIVE_WOLF.get(), TransformativeWolfEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}
