package net.onixary.shapeShifterCurseForge.registry;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.items.BookOfShapeShifterItem;
import net.onixary.shapeShifterCurseForge.items.SelectFormItem;
import net.onixary.shapeShifterCurseForge.items.TooltipItem;
import net.onixary.shapeShifterCurseForge.items.FormGrowthItem;
import net.onixary.shapeShifterCurseForge.items.trinkets.*;
import net.onixary.shapeShifterCurseForge.form.FormGrowthService;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS,
            ShapeShifterCurseForge.RESOURCE_NAMESPACE
    );

    public static final RegistryObject<Item> BOOK_OF_SHAPE_SHIFTER = ITEMS.register(
            "book_of_shape_shifter",
            () -> new BookOfShapeShifterItem(new Item.Properties())
    );

    public static final RegistryObject<Item> SELECT_FORM_ITEM = ITEMS.register(
            "select_form_item",
            () -> new SelectFormItem(new Item.Properties())
    );

    // Curios trinkets (accessory slots)
    public static final RegistryObject<Item> AMULET_BRACELET = ITEMS.register(
            "amulet_bracelet", () -> new AmuletBraceletTrinket(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ATTACH_HOOK = ITEMS.register(
            "attach_hook", () -> new AttachHookTrinket(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CHARM_OF_HOLLOW_FANG = ITEMS.register(
            "charm_of_hollow_fang", () -> new CharmOfHollowFangTrinket(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CHARM_OF_NIGHT_CRYSTAL = ITEMS.register(
            "charm_of_night_crystal", () -> new CharmOfNightCrystalTrinket(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CHARM_OF_REVERSE_THERMOMETER = ITEMS.register(
            "charm_of_reverse_thermometer", () -> new CharmOfReverseThermometerTrinket(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> COLLAR_OF_TENSION = ITEMS.register(
            "collar_of_tension", () -> new CollarOfTensionTrinket(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> COLLAR_OF_WHISKERS = ITEMS.register(
            "collar_of_whiskers", () -> new CollarOfWhiskersTrinket(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DIGESTION_FIBER_BALL = ITEMS.register(
            "digestion_fiber_ball", () -> new DigestionFiberBallTrinket(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FOUNTAIN_BELT = ITEMS.register(
            "fountain_belt", () -> new FountainBeltTrinket(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FROST_PAWGLOVE = ITEMS.register(
            "frost_pawglove", () -> new FrostPawgloveTrinket(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> RESONANT_CORE = ITEMS.register(
            "resonant_core", () -> new ResonantCoreTrinket(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> VENOM_SPINDLE = ITEMS.register(
            "venom_spindle", () -> new VenomSpindle(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> WITHERED_BANDAGE = ITEMS.register(
            "withered_bandage", () -> new WitheredBandageTrinket(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> UNTREATED_MOONDUST = ITEMS.register(
            "untreated_moondust", () -> new TooltipItem(
                    new Item.Properties(),
                    "item.shape-shifter-curse.untreated_moondust.tooltip",
                    ChatFormatting.GRAY));

    public static final RegistryObject<Item> MOONDUST_MATRIX = ITEMS.register(
            "moondust_matrix", () -> new TooltipItem(
                    new Item.Properties().stacksTo(64),
                    "item.shape-shifter-curse.moondust_matrix.tooltip",
                    ChatFormatting.GRAY));

    public static final RegistryObject<Item> MOONDUST_CRYSTAL_SHARD = ITEMS.register(
            "moondust_crystal_shard", () -> new TooltipItem(
                    new Item.Properties().stacksTo(64),
                    "item.shape-shifter-curse.moondust_crystal_shard.tooltip",
                    ChatFormatting.YELLOW));

    public static final RegistryObject<Item> ECTOPLASM_RAG = ITEMS.register(
            "ectoplasm_rag", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FIRE_CHARM_PAPER = ITEMS.register(
            "fire_charm_paper", () -> new TooltipItem(
                    new Item.Properties().stacksTo(64),
                    "item.shape-shifter-curse.fire_charm_paper.tooltip",
                    ChatFormatting.YELLOW));

    public static final RegistryObject<Item> ICON_CURSED_MOON = ITEMS.register(
            "icon_cursed_moon", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WEB_PROJECTILE = ITEMS.register(
            "web_projectile", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SILK_DEW = ITEMS.register(
            "silk_dew", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CATALYST = ITEMS.register(
            "catalyst", () -> new FormGrowthItem(new Item.Properties(), FormGrowthService.Mode.CATALYST));

    public static final RegistryObject<Item> POWERFUL_CATALYST = ITEMS.register(
            "powerful_catalyst", () -> new FormGrowthItem(new Item.Properties(), FormGrowthService.Mode.POWERFUL_CATALYST));

    public static final RegistryObject<Item> INHIBITOR = ITEMS.register(
            "inhibitor", () -> new FormGrowthItem(new Item.Properties(), FormGrowthService.Mode.INHIBITOR));

    public static final RegistryObject<Item> POWERFUL_INHIBITOR = ITEMS.register(
            "powerful_inhibitor", () -> new FormGrowthItem(new Item.Properties(), FormGrowthService.Mode.POWERFUL_INHIBITOR));

    public static final RegistryObject<Item> MOONDUST_CRYSTAL_GRIT_ITEM = ITEMS.register(
            "moondust_crystal_grit", () -> new BlockItem(
                    ModBlocks.MOONDUST_CRYSTAL_GRIT.get(), new Item.Properties()));

    // --- 1.10.0 fabric parity: block items + ripple_mirror ---
    public static final RegistryObject<Item> DEW_COVERED_COBWEB_ITEM = ITEMS.register(
            "dew_covered_cobweb", () -> new BlockItem(ModBlocks.DEW_COVERED_COBWEB.get(), new Item.Properties()));

    public static final RegistryObject<Item> WEB_COMPOSTER_ITEM = ITEMS.register(
            "web_composter", () -> new BlockItem(ModBlocks.WEB_COMPOSTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> ALTAR_ITEM = ITEMS.register(
            "altar", () -> new BlockItem(ModBlocks.ALTAR.get(), new Item.Properties()));

    public static final RegistryObject<Item> ALTER_ITEM = ITEMS.register(
            "alter", () -> new BlockItem(ModBlocks.ALTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> FORM_ATTUNER_ITEM = ITEMS.register(
            "form_attuner", () -> new BlockItem(ModBlocks.FORM_ATTUNER.get(), new Item.Properties()));

    public static final RegistryObject<Item> RIPPLE_MIRROR = ITEMS.register(
            "ripple_mirror", () -> new TooltipItem(
                    new Item.Properties().stacksTo(1),
                    "item.shape-shifter-curse.ripple_mirror.tooltip",
                    ChatFormatting.GRAY));

    public static final RegistryObject<Item> TEMP_WEB_BRIDGE_ITEM = ITEMS.register(
            "temp_web_bridge", () -> new BlockItem(ModBlocks.TEMP_WEB_BRIDGE.get(), new Item.Properties()));

    // --- 1.10.0 fabric parity: tools / armor / special items referenced by recipes & models ---
    public static final RegistryObject<Item> AUXILIARY_AXE = ITEMS.register(
            "auxiliary_axe", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> AUXILIARY_PICKAXE = ITEMS.register(
            "auxiliary_pickaxe", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> AUXILIARY_SWORD = ITEMS.register(
            "auxiliary_sword", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BOTTLED_SNOWFALL = ITEMS.register(
            "bottled_snowfall", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> DIAMOND_MINING_CLAW = ITEMS.register(
            "diamond_mining_claw", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MORPHSCALE_CORE = ITEMS.register(
            "morphscale_core", () -> new TooltipItem(new Item.Properties(), "item.shape-shifter-curse.morphscale_core.tooltip", ChatFormatting.GRAY));
    public static final RegistryObject<Item> SUPER_MORPHSCALE_CORE = ITEMS.register(
            "super_morphscale_core", () -> new TooltipItem(new Item.Properties(), "item.shape-shifter-curse.super_morphscale_core.tooltip", ChatFormatting.GRAY));
    public static final RegistryObject<Item> MORPHSCALE_HEADRING = ITEMS.register(
            "morphscale_headring", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MORPHSCALE_VEST = ITEMS.register(
            "morphscale_vest", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MORPHSCALE_CUISH = ITEMS.register(
            "morphscale_cuish", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MORPHSCALE_ANKLET = ITEMS.register(
            "morphscale_anklet", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> NETHERITE_MORPHSCALE_HEADRING = ITEMS.register(
            "netherite_morphscale_headring", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> NETHERITE_MORPHSCALE_VEST = ITEMS.register(
            "netherite_morphscale_vest", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> NETHERITE_MORPHSCALE_CUISH = ITEMS.register(
            "netherite_morphscale_cuish", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> NETHERITE_MORPHSCALE_ANKLET = ITEMS.register(
            "netherite_morphscale_anklet", () -> new Item(new Item.Properties().stacksTo(1)));
    // cosmetics / spawn eggs / misc models present in fabric jar
    public static final RegistryObject<Item> CREATIVE_INHIBITOR = ITEMS.register(
            "creative_inhibitor", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CURSED_BOOK_OF_SHAPE_SHIFTER = ITEMS.register(
            "cursed_book_of_shape_shifter", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CUSTOM_TRINKET = ITEMS.register(
            "custom_trinket", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PATRON_FORM_ITEM = ITEMS.register(
            "patron_form_item", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SPIDER_FLUID_COCOON = ITEMS.register(
            "spider_fluid_cocoon", () -> new Item(new Item.Properties().stacksTo(16)));
    // Transformative spawn eggs create the dedicated entities, which apply the
    // matching temporary transformation effect on a successful attack.
    public static final RegistryObject<Item> CUSTOM_AXOLOTL_SPAWN_EGG = ITEMS.register(
            "custom_axolotl_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.TRANSFORMATIVE_AXOLOTL, 9145227, 14985134, new Item.Properties()));
    public static final RegistryObject<Item> CUSTOM_BAT_SPAWN_EGG = ITEMS.register(
            "custom_bat_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.TRANSFORMATIVE_BAT, 9145227, 2039583, new Item.Properties()));
    public static final RegistryObject<Item> CUSTOM_OCELOT_SPAWN_EGG = ITEMS.register(
            "custom_ocelot_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.TRANSFORMATIVE_OCELOT, 9145227, 16547869, new Item.Properties()));
    public static final RegistryObject<Item> CUSTOM_SPIDER_SPAWN_EGG = ITEMS.register(
            "custom_spider_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.TRANSFORMATIVE_SPIDER, 9145227, 16748640, new Item.Properties()));
    public static final RegistryObject<Item> CUSTOM_WOLF_SPAWN_EGG = ITEMS.register(
            "custom_wolf_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.TRANSFORMATIVE_WOLF, 9145227, 16765781, new Item.Properties()));
    public static final RegistryObject<Item> TRANSFORMATIVE_AXOLOTL_BUCKET = ITEMS.register(
            "transformative_axolotl_bucket", () -> new Item(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
