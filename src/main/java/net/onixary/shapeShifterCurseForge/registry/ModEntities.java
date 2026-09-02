package net.onixary.shapeShifterCurseForge.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.entity.WebBulletEntity;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(
            ForgeRegistries.ENTITY_TYPES, ShapeShifterCurseForge.RESOURCE_NAMESPACE);

    public static final RegistryObject<EntityType<WebBulletEntity>> WEB_BULLET = ENTITIES.register("web_bullet",
            () -> EntityType.Builder.<WebBulletEntity>of(WebBulletEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(1).build("web_bullet"));

    private ModEntities() {
    }
}
