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

    private ModItems() {
    }
}
