package net.onixary.shapeShifterCurseForge.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService;
import net.onixary.shapeShifterCurseForge.other.advancement.SscAdvancementTriggers;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;
import net.onixary.shapeShifterCurseForge.power.InstinctService;
import net.onixary.shapeShifterCurseForge.power.BatAttachService;

@SuppressWarnings("deprecation")
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class PlayerCapabilityEvents {
    private static final ResourceLocation PLAYER_FORM_ID = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE,
            "player_form"
    );
    private static final ResourceLocation PLAYER_SKIN_ID = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE,
            "player_skin"
    );

    private PlayerCapabilityEvents() {
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerFormProvider formProvider = new PlayerFormProvider();
            event.addCapability(PLAYER_FORM_ID, formProvider);
            event.addListener(formProvider::invalidate);
            PlayerSkinProvider skinProvider = new PlayerSkinProvider();
            event.addCapability(PLAYER_SKIN_ID, skinProvider);
            event.addListener(skinProvider::invalidate);
        }
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        try {
            SscApi.copyPlayerData(event.getOriginal(), event.getEntity());
        } finally {
            event.getOriginal().invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            SscAdvancementTriggers.ON_FIRST_JOIN_WITH_MOD.trigger(player);
            ModNetwork.sendFormSync(player);
            ModNetwork.sendSkinSync(player);
            SscApi.currentForm(player).ifPresent(data -> ModNetwork.sendItemStores(player, data.getItemStores()));
            CursedMoonService.sendDaySync(player);
            FormActivePowerService.synchronizeMana(player);
            InstinctService.synchronizeHud(player);
        }
    }

    @SubscribeEvent
    public static void playerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            BatAttachService.clear(player);
            ModNetwork.sendFormSync(player);
            ModNetwork.sendSkinSync(player);
            SscApi.currentForm(player).ifPresent(data -> ModNetwork.sendItemStores(player, data.getItemStores()));
            CursedMoonService.sendDaySync(player);
            FormActivePowerService.synchronizeMana(player);
            InstinctService.synchronizeHud(player);
        }
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            BatAttachService.clear(player);
            ModNetwork.sendFormSync(player);
            ModNetwork.sendSkinSync(player);
            SscApi.currentForm(player).ifPresent(data -> ModNetwork.sendItemStores(player, data.getItemStores()));
            CursedMoonService.sendDaySync(player);
            FormActivePowerService.synchronizeMana(player);
            InstinctService.synchronizeHud(player);
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer tracker
                && event.getTarget() instanceof net.minecraft.server.level.ServerPlayer target) {
            ModNetwork.sendFormSyncTo(target, tracker);
            ModNetwork.sendSkinSyncTo(target, tracker);
        }
    }
}
