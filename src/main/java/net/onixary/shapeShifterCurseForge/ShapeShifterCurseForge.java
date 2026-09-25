package net.onixary.shapeShifterCurseForge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.onixary.shapeShifterCurseForge.other.config.SscClientConfig;
import net.onixary.shapeShifterCurseForge.other.config.SscCommonConfig;
import net.onixary.shapeShifterCurseForge.other.advancement.SscAdvancementTriggers;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.other.SscGameRules;
import net.onixary.shapeShifterCurseForge.registry.ModBlocks;
import net.onixary.shapeShifterCurseForge.registry.ModBlockEntities;
import net.onixary.shapeShifterCurseForge.registry.ModCreativeModeTabs;
import net.onixary.shapeShifterCurseForge.registry.ModEntities;
import net.onixary.shapeShifterCurseForge.registry.ModItems;
import net.onixary.shapeShifterCurseForge.registry.ModMenuTypes;
import net.onixary.shapeShifterCurseForge.registry.ModRecipeSerializers;
import net.onixary.shapeShifterCurseForge.registry.ModEffects;
import net.onixary.shapeShifterCurseForge.registry.ModPotions;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ShapeShifterCurseForge.MOD_ID)
public final class ShapeShifterCurseForge {
    public static final String MOD_ID = "shape_shifter_curse";
    public static final String RESOURCE_NAMESPACE = "shape-shifter-curse";
    public static final Logger LOGGER = LogManager.getLogger(ShapeShifterCurseForge.MOD_ID);

    @SuppressWarnings("removal")
    public ShapeShifterCurseForge() {
        SscGameRules.initialize();
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SscClientConfig.SPEC,
                "shape-shifter-curse-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SscCommonConfig.SPEC,
                "shape-shifter-curse-common.toml");

        ModItems.ITEMS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeModeTabs.TABS.register(modBus);
        ModMenuTypes.MENUS.register(modBus);
        ModRecipeSerializers.SERIALIZERS.register(modBus);
        ModRecipeSerializers.TYPES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModPotions.POTIONS.register(modBus);
        modBus.addListener(this::commonSetup);
        SscAdvancementTriggers.initialize();
        FormManager.initialize();
        ModNetwork.initialize();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModPotions::registerBrewing);
    }
}
