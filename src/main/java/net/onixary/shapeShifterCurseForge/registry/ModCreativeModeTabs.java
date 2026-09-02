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
                        output.accept(ModItems.UNTREATED_MOONDUST.get());
                        output.accept(ModItems.MOONDUST_MATRIX.get());
                        output.accept(ModItems.MOONDUST_CRYSTAL_SHARD.get());
                        output.accept(ModItems.ECTOPLASM_RAG.get());
                        output.accept(ModItems.FIRE_CHARM_PAPER.get());
                        output.accept(ModItems.ICON_CURSED_MOON.get());
                        output.accept(ModItems.WEB_PROJECTILE.get());
                        output.accept(ModItems.SILK_DEW.get());
                        output.accept(ModItems.MOONDUST_CRYSTAL_GRIT_ITEM.get());
                    })
                    .build());

    private ModCreativeModeTabs() {
    }
}
