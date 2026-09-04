package net.onixary.shapeShifterCurseForge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.util.List;

@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FormKeyMappings {
    public static final KeyMapping ACTIVE_SKILL_1 = key("active_skill_1");
    public static final KeyMapping ACTIVE_SKILL_2 = key("active_skill_2");
    public static final KeyMapping TOGGLE_CLIP_AT_LEDGE = key("toggle_clip_at_ledge");
    public static final KeyMapping MAKE_SOUND = key("make_sound");

    /** The four user-configurable SSC entries shown on Minecraft's Controls screen. */
    public static final List<KeyMapping> ACTIVE_SKILLS = List.of(
            ACTIVE_SKILL_2, ACTIVE_SKILL_1, TOGGLE_CLIP_AT_LEDGE, MAKE_SOUND
    );

    private FormKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        ACTIVE_SKILLS.forEach(event::register);
    }

    private static KeyMapping key(String name) {
        return new KeyMapping("key.shape-shifter-curse." + name, InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(), "category.shape-shifter-curse");
    }
}
