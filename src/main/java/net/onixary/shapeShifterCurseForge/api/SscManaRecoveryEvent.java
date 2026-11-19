package net.onixary.shapeShifterCurseForge.api;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * Posted on the Forge event bus on the server thread before positive mana recovery.
 * Includes gain_mana actions, cursed-moon recovery, reservoirs and feeding effects.
 * Cancel to suppress recovery, or change the amount to scale it. Consumption,
 * absolute assignment, form/respawn refills and grapple refunds do not post this event.
 */
@Cancelable
public final class SscManaRecoveryEvent extends PlayerEvent {
    private final ResourceLocation manaType;
    private final float originalAmount;
    private float amount;

    public SscManaRecoveryEvent(ServerPlayer player, ResourceLocation manaType, float amount) {
        super(Objects.requireNonNull(player, "player"));
        this.manaType = Objects.requireNonNull(manaType, "manaType");
        setAmount(amount);
        this.originalAmount = amount;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    public ResourceLocation getManaType() { return manaType; }
    public float getOriginalAmount() { return originalAmount; }
    public float getAmount() { return amount; }

    /** The final amount is clamped to the active pool's capacity after all listeners run. */
    public void setAmount(float amount) {
        if (!Float.isFinite(amount) || amount < 0.0F) {
            throw new IllegalArgumentException("amount must be finite and non-negative");
        }
        this.amount = amount;
    }
}
