package net.onixary.shapeShifterCurseForge.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class PlayerCapabilityEvents {
    private static final ResourceLocation PLAYER_FORM_ID = new ResourceLocation(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE,
            "player_form"
    );

    private PlayerCapabilityEvents() {
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerFormProvider provider = new PlayerFormProvider();
            event.addCapability(PLAYER_FORM_ID, provider);
            event.addListener(provider.getCapability(ModCapabilities.PLAYER_FORM, null)::invalidate);
        }
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(ModCapabilities.PLAYER_FORM).ifPresent(oldData ->
                event.getEntity().getCapability(ModCapabilities.PLAYER_FORM).ifPresent(newData ->
                        newData.copyFrom(oldData)
                )
        );
        event.getOriginal().invalidateCaps();
    }
}
