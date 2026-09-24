package net.onixary.shapeShifterCurseForge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.block.entity.AltarBlockEntity;
import net.onixary.shapeShifterCurseForge.block.entity.FormAttunerBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ShapeShifterCurseForge.RESOURCE_NAMESPACE);

    public static final RegistryObject<BlockEntityType<AltarBlockEntity>> ALTAR = BLOCK_ENTITIES.register("altar",
            () -> BlockEntityType.Builder.of(AltarBlockEntity::new, ModBlocks.ALTAR.get()).build(null));
    public static final RegistryObject<BlockEntityType<FormAttunerBlockEntity>> FORM_ATTUNER = BLOCK_ENTITIES.register("form_attuner",
            () -> BlockEntityType.Builder.of(FormAttunerBlockEntity::new, ModBlocks.FORM_ATTUNER.get()).build(null));

    private ModBlockEntities() {}
}
