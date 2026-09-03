package net.onixary.shapeShifterCurseForge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.onixary.shapeShifterCurseForge.config.SscClientConfig;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.registry.ModBlocks;
import net.onixary.shapeShifterCurseForge.registry.ModCreativeModeTabs;
import net.onixary.shapeShifterCurseForge.registry.ModEntities;
import net.onixary.shapeShifterCurseForge.registry.ModItems;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ShapeShifterCurseForge.MOD_ID)
public final class ShapeShifterCurseForge {
    public static final String MOD_ID = "shape_shifter_curse";
    public static final String RESOURCE_NAMESPACE = "shape-shifter-curse";
    public static final Logger LOGGER = LogManager.getLogger(ShapeShifterCurseForge.MOD_ID);

    public ShapeShifterCurseForge() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SscClientConfig.SPEC,
                "shape-shifter-curse-client.toml");

        ModItems.ITEMS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeModeTabs.TABS.register(modBus);
        FormManager.initialize();
        ModNetwork.initialize();
    }
}
