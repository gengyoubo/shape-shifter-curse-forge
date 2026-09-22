package net.onixary.shapeShifterCurseForge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.blockentity.AltarBlockEntity;
import net.onixary.shapeShifterCurseForge.blockentity.AlterBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ShapeShifterCurseForge.RESOURCE_NAMESPACE);

    public static final RegistryObject<BlockEntityType<AltarBlockEntity>> ALTAR = BLOCK_ENTITIES.register("altar",
            () -> BlockEntityType.Builder.of(AltarBlockEntity::new, ModBlocks.ALTAR.get()).build(null));
    public static final RegistryObject<BlockEntityType<AlterBlockEntity>> ALTER = BLOCK_ENTITIES.register("alter",
            () -> BlockEntityType.Builder.of(AlterBlockEntity::new, ModBlocks.ALTER.get()).build(null));

    private ModBlockEntities() {}
}
