package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.network.ActivePowerKeyPacket;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class FormKeyInputEvents {
    private static final Map<KeyMapping, Boolean> LAST_STATE = new HashMap<>();

    private FormKeyInputEvents() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (KeyMapping key : FormKeyMappings.ACTIVE_SKILLS) {
            boolean pressed = key.isDown();
            boolean previous = LAST_STATE.getOrDefault(key, false);
            if (pressed != previous) {
                ModNetwork.CHANNEL.sendToServer(new ActivePowerKeyPacket(key.getName(), pressed));
                LAST_STATE.put(key, pressed);
            }
        }
    }
}
