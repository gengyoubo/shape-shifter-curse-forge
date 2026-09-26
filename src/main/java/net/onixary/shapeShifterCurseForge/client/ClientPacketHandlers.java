package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.onixary.shapeShifterCurseForge.client.codex.FormColorSelectMenuV2;
import net.onixary.shapeShifterCurseForge.client.codex.NormalFormSelectScreen;
import net.onixary.shapeShifterCurseForge.client.screen.FormAttunerScreen;

import java.util.Set;
import java.util.UUID;

/**
 * Client-only packet side effects.
 *
 * <p>Network packet classes are loaded on the dedicated server too (for registration).
 * Any client-only class referenced directly in a packet's bytecode (for example a
 * {@code Screen} subclass) is therefore loaded during that registration, which throws
 * "Attempted to load class ... for invalid dist DEDICATED_SERVER". Routing the client
 * work through this helper keeps the packet classes free of client references: the
 * packets only contain a deferred {@code invokestatic} into this class, which is never
 * loaded on the server.</p>
 */
// TODO[C-S] Any new server->client packet must route its client-only work through this helper
//   (never reference Minecraft/Screen subclasses directly), or the dedicated server crashes on load.
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void openSelectForm(String targetName, UUID targetUUID) {
        Minecraft.getInstance().setScreen(new NormalFormSelectScreen(
                Component.literal("FormSelectScreen"), targetName, targetUUID));
    }

    public static void openColorMenu() {
        if (FormColorSelectMenuV2.instance == null && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().setScreen(new FormColorSelectMenuV2(
                    Component.literal("text.shape-shifter-curse.config.form_color_select_menu_v2"),
                    Minecraft.getInstance().screen));
        }
    }

    public static void openFormAttuner(int level, int maxLevel, String formGroupId,
                                       Set<String> unlockedPerks, String statusKey) {
        Minecraft.getInstance().setScreen(new FormAttunerScreen(
                level, maxLevel, formGroupId, unlockedPerks, statusKey));
    }

    public static void setInstinct(float value, float rate, boolean visible, boolean locked) {
        InstinctClientState.set(value, rate, visible, locked);
    }

    public static void setItemStores(net.minecraft.nbt.CompoundTag stores) {
        var player = Minecraft.getInstance().player;
        if (player != null) net.onixary.shapeShifterCurseForge.api.SscApi.currentForm(player)
                .ifPresent(data -> data.setItemStores(stores));
    }

    public static void setBatAttachState(UUID playerId, BlockPos pos, Direction side) {
        net.onixary.shapeShifterCurseForge.power.BatAttachService.applyClientState(
                playerId, Minecraft.getInstance().level, pos, side);
    }

    public static void applyForcedSneaking(int entityId, boolean forced) {
        var level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(entityId) instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        net.onixary.shapeShifterCurseForge.power.MovementPowerService.applySyncedForcedSneaking(player, forced);
    }

    public static void setTransformState(int entityId, boolean transforming,
                                         String startFormId, String endFormId) {
        TransformClientState.apply(entityId, transforming, startFormId, endFormId);
    }

    public static void applyMovementLock(int noMoveTicks, int noJumpTicks) {
        TransformClientState.applyMovementLock(noMoveTicks, noJumpTicks);
    }

    /** Plays the vanilla totem activation animation for a virtual totem stack. */
    public static void displayTotem(net.minecraft.world.item.ItemStack stack) {
        if (!stack.isEmpty()) {
            Minecraft.getInstance().gameRenderer.displayItemActivation(stack);
        }
    }
}
