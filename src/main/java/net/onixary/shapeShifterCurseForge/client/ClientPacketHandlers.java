package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.onixary.shapeShifterCurseForge.client.codex.FormColorSelectMenuV2;
import net.onixary.shapeShifterCurseForge.client.codex.NormalFormSelectScreen;
import net.onixary.shapeShifterCurseForge.client.screen.FormUpgradeScreen;
import net.minecraft.resources.ResourceLocation;

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
    public static void setChargeState(net.minecraft.resources.ResourceLocation id, boolean charging,
                                      String chargeKey, int tier) {
        var player = Minecraft.getInstance().player;
        if (player != null) net.onixary.shapeShifterCurseForge.power.FormActivePowerService.acceptClientCharge(player, id, charging);
        ManaClientState.setChargeTier(chargeKey, charging ? tier : 0);
    }
    public static void setPowerResource(net.minecraft.resources.ResourceLocation id, double value) {
        var player = Minecraft.getInstance().player;
        if (player != null) net.onixary.shapeShifterCurseForge.power.FormActivePowerService.acceptClientResource(player, id, value);
    }
    public static void applyFrostDive(boolean diving) {
        var player = Minecraft.getInstance().player;
        if (player != null) net.onixary.shapeShifterCurseForge.power.FrostDiveService.setClientDiving(player, diving);
    }
    private ClientPacketHandlers() {
    }

    public static void openSelectForm(String targetName, UUID targetUUID) {
        Minecraft.getInstance().setScreen(new NormalFormSelectScreen(
                Component.literal("FormSelectScreen"), targetName, targetUUID));
    }

    public static void openRippleMirror(java.util.List<ResourceLocation> forms) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setScreen(new net.onixary.shapeShifterCurseForge.client.codex.RippleMirrorSelectScreen(
                    minecraft.player.getGameProfile().getName(), minecraft.player.getUUID(), forms));
        }
    }

    public static void openColorMenu() {
        if (FormColorSelectMenuV2.instance == null && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().setScreen(new FormColorSelectMenuV2(
                    Component.literal("text.shape-shifter-curse.config.form_color_select_menu_v2"),
                    Minecraft.getInstance().screen));
        }
    }

    /** Like Fabric, the attuner opens the skill tree directly; refreshes update it in place. */
    public static void openFormAttuner(int level, int maxLevel, String formGroupId,
                                       Set<String> unlockedPerks, String statusKey) {
        ResourceLocation groupId = ResourceLocation.tryParse(formGroupId == null ? "" : formGroupId);
        Set<ResourceLocation> unlocked = new java.util.LinkedHashSet<>();
        if (unlockedPerks != null) for (String perkId : unlockedPerks) {
            ResourceLocation id = ResourceLocation.tryParse(perkId);
            if (id != null) unlocked.add(id);
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof FormUpgradeScreen open && java.util.Objects.equals(open.formGroupId(), groupId)) {
            open.update(level, unlocked, statusKey);
            return;
        }
        minecraft.setScreen(new FormUpgradeScreen(level, groupId, unlocked, statusKey));
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
