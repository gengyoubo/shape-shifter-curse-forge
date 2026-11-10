package net.onixary.shapeShifterCurseForge.api.registry;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Behaviour for a data-defined power whose JSON {@code "type"} is not built into SSC.
 *
 * <p>Register one instance per type id with {@link SscJavaRegistries#registerPowerType}. SSC then
 * finds every power of that type on the player's current form (including children of
 * {@code apoli:multiple}) and drives the callbacks below. {@code powerId} is the power file id, or
 * for a child of {@code apoli:multiple} Apoli's sub-power id {@code <parent id>_<child key>};
 * {@code data} is that power's JSON object, which must be treated as read-only.</p>
 *
 * <p>A power type object is shared registration data: never store one player's state in its
 * fields; key per-player state by the player's UUID and clean it up in {@link #onRemoved}.</p>
 *
 * <p>Key callbacks fire for powers whose {@code "key": {"key": "..."}} names the key that changed.
 * SSC's own keys ({@code key.shape-shifter-curse.*}, {@code key.jump}, {@code key.sprint}) always
 * qualify; add-on key names must be registered with {@link SscJavaRegistries#registerPowerKey}.</p>
 */
public interface SscPowerType {
    /** Every server player tick while the power is on the player's current form. */
    default void tick(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
    }

    /** Every client player tick (logical client) while the power is on that player's form. */
    default void clientTick(Player player, ResourceLocation powerId, JsonObject data) {
    }

    /** The power's key went down. Fires once per press, after SSC's own toggle handling. */
    default void onKeyPressed(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
    }

    /** Every server tick while the power's key stays down (including the press tick's following ticks). */
    default void whileKeyHeld(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
    }

    /** The power's key was released. Not called when the power is removed while held; see {@link #onRemoved}. */
    default void onKeyReleased(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
    }

    /** The power became active for the player (login, form change, data reload), before its first tick. */
    default void onAdded(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
    }

    /**
     * The power is no longer active for the player: form change, data reload, logout or server stop.
     * {@code data} is the definition the power was last ticked with.
     */
    default void onRemoved(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
    }
}
