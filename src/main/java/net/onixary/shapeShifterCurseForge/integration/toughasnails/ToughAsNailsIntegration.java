package net.onixary.shapeShifterCurseForge.integration.toughasnails;

import net.minecraft.world.entity.player.Player;

/**
 * Optional single seam for Tough As Nails. TAN is not a build dependency, so the Forge port keeps
 * every TAN touch-point here instead of spreading reflection across the power interpreter.
 *
 * <p>TODO[PARITY] TAN integration is incomplete:</p>
 * <ul>
 *   <li>{@code tan_add_thirst} (action) — best-effort reflection below, unverified against the exact
 *       TAN 1.20.1 API.</li>
 *   <li>{@code tan_form_temperature_modifier} — needs a mixin into TAN's temperature computation;
 *       only the offsets (icy/cold/neutral/warm/hot) are currently parsed by nothing.</li>
 *   <li>{@code tan_prevent_dirty_water_thirst_effect} / thirst exhaustion — only approximated in
 *       MissingPowerEvents.</li>
 * </ul>
 */
public final class ToughAsNailsIntegration {
    private static final boolean LOADED = net.minecraftforge.fml.ModList.get().isLoaded("toughasnails");
    private static boolean warned;

    private ToughAsNailsIntegration() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    /** Best-effort AddThirst; safely inert when TAN (or a matching API) is unavailable. */
    public static void addThirst(Player player, float amount) {
        if (!LOADED || amount == 0.0F) {
            return;
        }
        try {
            Class<?> helper = Class.forName("com.momosoftworks.toughasnails.api.thirst.ThirstHelper");
            Object thirst = helper.getMethod("getThirst", Player.class).invoke(null, player);
            if (thirst == null) {
                return;
            }
            try {
                thirst.getClass().getMethod("addThirst", float.class).invoke(thirst, amount);
            } catch (NoSuchMethodException noFloat) {
                thirst.getClass().getMethod("addThirst", int.class).invoke(thirst, Math.round(amount));
            }
        } catch (Throwable throwable) {
            if (!warned) {
                warned = true;
                net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge.LOGGER.warn(
                        "[ssc-tan] Tough As Nails integration could not reach the thirst API; TAN powers stay inert.");
            }
        }
    }
}