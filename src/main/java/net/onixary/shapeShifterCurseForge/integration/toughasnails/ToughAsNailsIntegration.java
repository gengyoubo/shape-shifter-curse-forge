package net.onixary.shapeShifterCurseForge.integration.toughasnails;

import net.minecraft.world.entity.player.Player;

/**
 * Optional single seam for Tough As Nails. Keep direct TAN references out of classes loaded when
 * the mod is absent.
 *
 * <p>TAN is an optional runtime integration. Mixin hooks are gated on the mod being present.</p>
 * <ul>
 *   <li>{@code tan_add_thirst} (action) uses TAN's thirst capability and sync method.</li>
 *   <li>{@code tan_form_temperature_modifier} and
 *       {@code tan_prevent_dirty_water_thirst_effect} are handled by optional mixins.</li>
 * </ul>
 */
public final class ToughAsNailsIntegration {
    private static boolean warned;

    private ToughAsNailsIntegration() {
    }

    public static boolean isLoaded() {
        var mods = net.minecraftforge.fml.ModList.get();
        return mods != null && mods.isLoaded("toughasnails");
    }

    /** Mirrors Fabric's clamped integer thirst mutation and explicit server sync. */
    public static void addThirst(Player player, int amount) {
        if (!isLoaded() || player.level().isClientSide || amount == 0) {
            return;
        }
        try {
            Class<?> helper = Class.forName("toughasnails.api.thirst.ThirstHelper");
            Object thirst = helper.getMethod("getThirst", Player.class).invoke(null, player);
            if (thirst == null) {
                return;
            }
            Class<?> thirstApi = Class.forName("toughasnails.api.thirst.IThirst");
            int current = (int) thirstApi.getMethod("getThirst").invoke(thirst);
            thirstApi.getMethod("setThirst", int.class).invoke(thirst, Math.max(0, Math.min(20, current + amount)));
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                var sync = Class.forName("toughasnails.thirst.ThirstHandler")
                        .getDeclaredMethod("syncThirst", net.minecraft.server.level.ServerPlayer.class);
                sync.setAccessible(true);
                sync.invoke(null, serverPlayer);
            }
        } catch (Throwable throwable) {
            if (!warned) {
                warned = true;
                net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge.LOGGER.warn(
                        "[ssc-tan] Tough As Nails thirst API call failed.", throwable);
            }
        }
    }
}
