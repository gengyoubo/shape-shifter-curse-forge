package net.onixary.shapeShifterCurseForge.api;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Server-thread calculation of an active mana pool's capacity, on the Forge event bus.
 * The original maximum includes SSC's built-in mana_attribute powers. Listeners may
 * add, multiply or replace the current maximum; SSC clamps the final result to at least one.
 *
 * <p>Listeners must only calculate a value, without changing player state. Results
 * are not cached: conditions are evaluated again on the next query. A nested maximum
 * query for the same player sees SSC's built-in maximum and does not post another event.</p>
 */
public final class SscManaMaximumEvent extends PlayerEvent {
    private final ResourceLocation manaType;
    private final float originalMaximum;
    private float maximum;

    public SscManaMaximumEvent(ServerPlayer player, ResourceLocation manaType, float maximum) {
        super(Objects.requireNonNull(player, "player"));
        this.manaType = Objects.requireNonNull(manaType, "manaType");
        setMaximum(maximum);
        this.originalMaximum = maximum;
    }

    @Override
    public ServerPlayer getEntity() {
        return (ServerPlayer) super.getEntity();
    }

    public ResourceLocation getManaType() { return manaType; }
    public float getOriginalMaximum() { return originalMaximum; }
    public float getMaximum() { return maximum; }

    /** Accepts any finite value; the final result is clamped to at least one after all listeners run. */
    public void setMaximum(float maximum) {
        if (!Float.isFinite(maximum)) throw new IllegalArgumentException("maximum must be finite");
        this.maximum = maximum;
    }
}
