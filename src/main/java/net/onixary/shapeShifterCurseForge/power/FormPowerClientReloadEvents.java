package net.onixary.shapeShifterCurseForge.power;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

/** Keeps client-side form, power, and dynamic-form registries in sync with the server pipeline. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FormPowerClientReloadEvents {
    private FormPowerClientReloadEvents() {
    }

    @SubscribeEvent
    public static void register(RegisterClientReloadListenersEvent event) {
        FormPowerRegistry.addClientReloadListeners(event);
    }
}
