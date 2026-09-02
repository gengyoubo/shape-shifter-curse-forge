package net.onixary.shapeShifterCurseForge.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(
            ForgeRegistries.BLOCKS,
            ShapeShifterCurseForge.RESOURCE_NAMESPACE
    );

    public static final RegistryObject<Block> MOONDUST_CRYSTAL_GRIT = BLOCKS.register(
            "moondust_crystal_grit",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.6F))
    );

    public static final RegistryObject<Block> TEMP_WEB_BRIDGE = BLOCKS.register(
            "temp_web_bridge", () -> new TemporaryWebBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL).strength(4.0F).noLootTable().randomTicks().noCollission())
    );

    private ModBlocks() {
    }
}
