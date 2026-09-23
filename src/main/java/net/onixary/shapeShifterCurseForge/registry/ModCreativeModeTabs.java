package net.onixary.shapeShifterCurseForge.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB, ShapeShifterCurseForge.RESOURCE_NAMESPACE);

    public static final RegistryObject<CreativeModeTab> SSC_ITEMS = TABS.register(
            "ssc_items", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.shape_shifter_curse.sscitems"))
                    .icon(() -> new ItemStack(ModItems.BOOK_OF_SHAPE_SHIFTER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BOOK_OF_SHAPE_SHIFTER.get());
                        output.accept(ModItems.SELECT_FORM_ITEM.get());
                        output.accept(ModItems.AMULET_BRACELET.get());
                        output.accept(ModItems.ATTACH_HOOK.get());
                        output.accept(ModItems.CHARM_OF_HOLLOW_FANG.get());
                        output.accept(ModItems.CHARM_OF_NIGHT_CRYSTAL.get());
                        output.accept(ModItems.CHARM_OF_REVERSE_THERMOMETER.get());
                        output.accept(ModItems.COLLAR_OF_TENSION.get());
                        output.accept(ModItems.COLLAR_OF_WHISKERS.get());
                        output.accept(ModItems.DIGESTION_FIBER_BALL.get());
                        output.accept(ModItems.FOUNTAIN_BELT.get());
                        output.accept(ModItems.FROST_PAWGLOVE.get());
                        output.accept(ModItems.RESONANT_CORE.get());
                        output.accept(ModItems.VENOM_SPINDLE.get());
                        output.accept(ModItems.WITHERED_BANDAGE.get());
                        output.accept(ModItems.UNTREATED_MOONDUST.get());
                        output.accept(ModItems.MOONDUST_MATRIX.get());
                        output.accept(ModItems.MOONDUST_CRYSTAL_SHARD.get());
                        output.accept(ModItems.ECTOPLASM_RAG.get());
                        output.accept(ModItems.FIRE_CHARM_PAPER.get());
                        output.accept(ModItems.ICON_CURSED_MOON.get());
                        output.accept(ModItems.WEB_PROJECTILE.get());
                        output.accept(ModItems.SILK_DEW.get());
                        output.accept(ModItems.CATALYST.get());
                        output.accept(ModItems.POWERFUL_CATALYST.get());
                        output.accept(ModItems.INHIBITOR.get());
                        output.accept(ModItems.POWERFUL_INHIBITOR.get());
                        output.accept(ModItems.MOONDUST_CRYSTAL_GRIT_ITEM.get());
                        output.accept(ModItems.DEW_COVERED_COBWEB_ITEM.get());
                        output.accept(ModItems.WEB_COMPOSTER_ITEM.get());
                        output.accept(ModItems.ALTAR_ITEM.get());
                        output.accept(ModItems.FORM_ATTUNER_ITEM.get());
                        output.accept(ModItems.RIPPLE_MIRROR.get());
                        output.accept(ModItems.AUXILIARY_AXE.get());
                        output.accept(ModItems.AUXILIARY_PICKAXE.get());
                        output.accept(ModItems.AUXILIARY_SWORD.get());
                        output.accept(ModItems.BOTTLED_SNOWFALL.get());
                        output.accept(ModItems.DIAMOND_MINING_CLAW.get());
                        output.accept(ModItems.MORPHSCALE_CORE.get());
                        output.accept(ModItems.SUPER_MORPHSCALE_CORE.get());
                        output.accept(ModItems.MORPHSCALE_HEADRING.get());
                        output.accept(ModItems.MORPHSCALE_VEST.get());
                        output.accept(ModItems.MORPHSCALE_CUISH.get());
                        output.accept(ModItems.MORPHSCALE_ANKLET.get());
                        output.accept(ModItems.NETHERITE_MORPHSCALE_HEADRING.get());
                        output.accept(ModItems.NETHERITE_MORPHSCALE_VEST.get());
                        output.accept(ModItems.NETHERITE_MORPHSCALE_CUISH.get());
                        output.accept(ModItems.NETHERITE_MORPHSCALE_ANKLET.get());
                    })
                    .build());

    private ModCreativeModeTabs() {
    }
}
