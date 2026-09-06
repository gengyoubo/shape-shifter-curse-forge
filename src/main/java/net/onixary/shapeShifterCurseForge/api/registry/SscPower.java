package net.onixary.shapeShifterCurseForge.api.registry;

import net.minecraft.world.entity.player.Player;

/**
 * A Java power attached to one or more SSC forms.
 *
 * <p>The callback is invoked once each server-side player tick while its assigned form is
 * active. Java powers that need another Forge event may subscribe to that event themselves and
 * use {@link SscJavaRegistries#isPowerActive(Player, net.minecraft.resources.ResourceLocation)}
 * to check whether they are enabled.</p>
 */
@FunctionalInterface
public interface SscPower {
    void tick(Player player);
}
