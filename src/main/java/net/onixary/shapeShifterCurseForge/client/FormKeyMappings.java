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
    public static final List<KeyMapping> ACTIVE_SKILLS = List.of(
            key("active_skill_1"), key("active_skill_2"), key("active_skill_3"),
            key("active_skill_4"), key("active_skill_5"), key("active_skill_6"), key("make_sound")
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
