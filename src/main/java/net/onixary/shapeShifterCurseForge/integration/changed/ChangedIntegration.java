package net.onixary.shapeShifterCurseForge.integration.changed;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.power.TransformativeEffectService;

/** Mutual exclusion with Changed; SSC wins when loading an existing conflicting state. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class ChangedIntegration {
    private static final Map<Player, Integer> LAST_BLOCKED_MESSAGE_TICKS = new WeakHashMap<>();
    private static final int BLOCKED_MESSAGE_COOLDOWN = 40;

    private ChangedIntegration() {
    }

    public static boolean isOriginalForm(ResourceLocation formId) {
        return FormRegistry.ORIGINAL_BEFORE_ENABLE.equals(formId)
                || FormRegistry.ORIGINAL_SHIFTER.equals(formId);
    }

    public static boolean hasSscVariant(Player player) {
        return SscApi.currentForm(player).map(data -> {
            ResourceLocation formId = ResourceLocation.tryParse(data.getFormId());
            return formId != null && !isOriginalForm(formId);
        }).orElse(false);
    }

    public static boolean hasChangedVariant(Player player) {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded("changed") && ChangedAccess.hasVariant(player);
    }

    public static boolean canTransformTo(Player player, ResourceLocation targetId) {
        return isOriginalForm(targetId) || !hasChangedVariant(player);
    }

    public static void notifyBlocked(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Integer lastTick = LAST_BLOCKED_MESSAGE_TICKS.get(player);
        if (lastTick != null && player.tickCount - lastTick < BLOCKED_MESSAGE_COOLDOWN) {
            return;
        }
        LAST_BLOCKED_MESSAGE_TICKS.put(player, player.tickCount);
        serverPlayer.sendSystemMessage(Component.translatable(
                "info.shape-shifter-curse.transform_blocked").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof ServerPlayer player)
                || !hasChangedVariant(player)) {
            return;
        }
        if (hasSscVariant(player)) {
            ChangedAccess.removeVariant(player);
            player.refreshDimensions();
        } else {
            // Enabling SSC content in either original human form is not a transformation.
            TransformativeEffectService.clear(player);
        }
    }

    /** Keep optional Changed classes out of the always-loaded integration's signatures. */
    private static final class ChangedAccess {
        private static boolean hasVariant(Player player) {
            return net.ltxprogrammer.changed.process.ProcessTransfur.getPlayerTransfurVariant(player) != null;
        }

        private static void removeVariant(Player player) {
            net.ltxprogrammer.changed.process.ProcessTransfur.removePlayerTransfurVariant(player);
            net.ltxprogrammer.changed.process.ProcessTransfur.setPlayerTransfurProgress(player, 0.0F);
        }
    }
}
