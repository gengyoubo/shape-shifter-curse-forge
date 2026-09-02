package net.onixary.shapeShifterCurseForge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.registry.ModBlocks;
import net.onixary.shapeShifterCurseForge.registry.ModCreativeModeTabs;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

@Mod(ShapeShifterCurseForge.MOD_ID)
public final class ShapeShifterCurseForge {
    public static final String MOD_ID = "shape_shifter_curse";
    public static final String RESOURCE_NAMESPACE = "shape-shifter-curse";

    public ShapeShifterCurseForge() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModCreativeModeTabs.TABS.register(modBus);
        ModNetwork.initialize();
    }
}
