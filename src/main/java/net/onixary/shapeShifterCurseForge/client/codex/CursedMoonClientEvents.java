package net.onixary.shapeShifterCurseForge.client.codex;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

/** Client-side warning message, matching Fabric's midday Cursed Moon notification. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class CursedMoonClientEvents {
    private CursedMoonClientEvents() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CursedMoonData.clientTick(Minecraft.getInstance().level);
        }
    }
}
