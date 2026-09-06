package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.network.ActivePowerKeyPacket;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class FormKeyInputEvents {
    private static final Map<KeyMapping, Boolean> LAST_STATE = new HashMap<>();
    private static final ResourceLocation TOGGLE_CLIP_POWER = ResourceLocation.fromNamespaceAndPath(
            "shape-shifter-curse", "toggle_clip_at_ledge");
    private static boolean clipAtLedgeDisabled;

    private FormKeyInputEvents() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            clipAtLedgeDisabled = false;
        } else if (!FormPowerRegistry.has(minecraft.player, TOGGLE_CLIP_POWER)) {
            clipAtLedgeDisabled = false;
        }
        for (KeyMapping key : FormKeyMappings.ACTIVE_SKILLS) {
            boolean pressed = key.isDown();
            boolean previous = LAST_STATE.getOrDefault(key, false);
            if (pressed != previous) {
                ModNetwork.CHANNEL.sendToServer(new ActivePowerKeyPacket(key.getName(), pressed));
                LAST_STATE.put(key, pressed);
                if (key == FormKeyMappings.TOGGLE_CLIP_AT_LEDGE && pressed
                        && minecraft.player != null
                        && FormPowerRegistry.has(minecraft.player, TOGGLE_CLIP_POWER)) {
                    clipAtLedgeDisabled = !clipAtLedgeDisabled;
                }
            }
        }
        KeyMapping jump = minecraft.options.keyJump;
        boolean pressed = jump.isDown();
        boolean previous = LAST_STATE.getOrDefault(jump, false);
        if (pressed != previous) {
            ModNetwork.CHANNEL.sendToServer(new ActivePowerKeyPacket("key.jump", pressed));
            LAST_STATE.put(jump, pressed);
        }
    }

    public static boolean isClipAtLedgeDisabled() {
        Minecraft minecraft = Minecraft.getInstance();
        return clipAtLedgeDisabled && minecraft.player != null
                && FormPowerRegistry.has(minecraft.player, TOGGLE_CLIP_POWER);
    }
}
