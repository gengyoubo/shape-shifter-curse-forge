package net.onixary.shapeShifterCurseForge.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS,
            ShapeShifterCurseForge.RESOURCE_NAMESPACE
    );

    public static final RegistryObject<Item> BOOK_OF_SHAPE_SHIFTER = ITEMS.register(
            "book_of_shape_shifter",
            () -> new Item(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> MOONDUST_CRYSTAL_SHARD = ITEMS.register(
            "moondust_crystal_shard", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ECTOPLASM_RAG = ITEMS.register(
            "ectoplasm_rag", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FIRE_CHARM_PAPER = ITEMS.register(
            "fire_charm_paper", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ICON_CURSED_MOON = ITEMS.register(
            "icon_cursed_moon", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WEB_PROJECTILE = ITEMS.register(
            "web_projectile", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SILK_DEW = ITEMS.register(
            "silk_dew", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MOONDUST_CRYSTAL_GRIT_ITEM = ITEMS.register(
            "moondust_crystal_grit", () -> new BlockItem(
                    ModBlocks.MOONDUST_CRYSTAL_GRIT.get(), new Item.Properties()));

    private ModItems() {
    }
}
