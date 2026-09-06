package net.onixary.shapeShifterCurseForge.api.registry;

import net.minecraft.world.entity.player.Player;

/**
 * A Java power attached to one or more SSC forms.
 *
 * <p>The callback is invoked once each server-side player tick while its assigned form is
 * active. Java powers that need another Forge event may subscribe to that event themselves and
 * use {@link SscJavaRegistries#isPowerActive(Player, net.minecraft.resources.ResourceLocation)}
 * to check whether they are enabled. A power object is shared registration data: never store one
 * player's cooldown or mutable ability state in its fields; use a per-player capability or
 * attachment instead.</p>
 */
@FunctionalInterface
public interface SscPower {
    void tick(Player player);
}
