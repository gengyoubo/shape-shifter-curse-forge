package net.onixary.shapeShifterCurseForge.api;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;

/**
 * Server-thread access to the player's currently active SSC mana pool.
 * Writes are clamped to the pool's bounds and synchronized to the owning client.
 * Players without an active mana power report zero and ignore writes.
 */
public final class SscManaApi {
    private SscManaApi() {}

    public static boolean hasManaPool(ServerPlayer player) {
        return FormActivePowerService.hasActiveManaType(Objects.requireNonNull(player, "player"));
    }

    public static Optional<ResourceLocation> manaType(ServerPlayer player) {
        return hasManaPool(player)
                ? Optional.of(ResourceLocation.parse(FormActivePowerService.manaType(player)))
                : Optional.empty();
    }

    public static float mana(ServerPlayer player) {
        return hasManaPool(player) ? FormActivePowerService.mana(player) : 0.0F;
    }

    /** Includes built-in modifiers and add-on adjustments from {@link SscManaMaximumEvent}. */
    public static float maximumMana(ServerPlayer player) {
        return hasManaPool(player) ? FormActivePowerService.maximumMana(player) : 0.0F;
    }

    /** Returns a fraction, or zero when no mana pool is active. */
    public static float manaPercent(ServerPlayer player) {
        return hasManaPool(player) ? FormActivePowerService.manaPercent(player) : 0.0F;
    }

    public static boolean hasMana(ServerPlayer player, float amount) {
        requireAmount(amount);
        return hasManaPool(player) && FormActivePowerService.hasMana(player, amount);
    }

    /** Absolute assignment; bypasses recovery events, including for refunds or administrative changes. */
    public static void setMana(ServerPlayer player, float value) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException("value must be finite");
        if (hasManaPool(player)) FormActivePowerService.setMana(player, value);
    }

    /** Recovery; posts a cancellable {@link SscManaRecoveryEvent} before changing the pool. */
    public static void gainMana(ServerPlayer player, float amount) {
        requireAmount(amount);
        if (hasManaPool(player)) FormActivePowerService.gainMana(player, amount);
    }

    /** All-or-nothing consumption. Returns false when no pool is active or mana is insufficient. */
    public static boolean consumeMana(ServerPlayer player, float amount) {
        if (!hasMana(player, amount)) return false;
        FormActivePowerService.consumeMana(player, amount);
        return true;
    }

    /** Recalculate capacity, clamp excess mana and immediately update the owning client's HUD. */
    public static void synchronizeMana(ServerPlayer player) {
        FormActivePowerService.synchronizeMana(Objects.requireNonNull(player, "player"));
    }

    private static void requireAmount(float amount) {
        if (!Float.isFinite(amount) || amount < 0.0F) {
            throw new IllegalArgumentException("amount must be finite and non-negative");
        }
    }
}
