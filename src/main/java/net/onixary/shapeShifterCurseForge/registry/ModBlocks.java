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

    // --- 1.10.0 fabric parity: blocks that existed in fabric jar but were missing in forge port ---
    public static final RegistryObject<Block> DEW_COVERED_COBWEB = BLOCKS.register(
            "dew_covered_cobweb",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.4F).noCollission().sound(net.minecraft.world.level.block.SoundType.WOOL))
    );

    public static final RegistryObject<Block> WEB_COMPOSTER = BLOCKS.register(
            "web_composter",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F).sound(net.minecraft.world.level.block.SoundType.WOOD))
    );

    public static final RegistryObject<Block> ALTAR = BLOCKS.register(
            "altar",
            () -> new net.onixary.shapeShifterCurseForge.block.AltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion())
    );

    public static final RegistryObject<Block> ALTER = BLOCKS.register(
            "alter",
            () -> new net.onixary.shapeShifterCurseForge.block.AlterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion())
    );

    public static final RegistryObject<Block> FORM_ATTUNER = BLOCKS.register(
            "form_attuner",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F).sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion())
    );

    private ModBlocks() {
    }
}
