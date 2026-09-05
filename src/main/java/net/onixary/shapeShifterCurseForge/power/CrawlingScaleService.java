package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;

/**
 * Forge counterpart of Fabric's {@code CrawlingPower} (which drives Pehkui scales).
 * While a {@code shape-shifter-curse:crawling} power is active the hitbox height and
 * the eye height take the power's active scales; otherwise the form defaults apply.
 *
 * <p>No values are hardcoded and nothing is snapshot-saved: every recompute derives
 * from the form definition plus the power JSON, so releasing sneak (or any other
 * condition end) always lands back on the form default instead of a stale value.</p>
 *
 * <p>The sneak toggle itself never refreshes dimensions, and drift from any source
 * would otherwise stick forever (a collapse to ~0 was observed persisting with no
 * further toggles). So {@link #tick(Player)} continuously compares the live bounds
 * against the authoritatively recomputed default and refreshes on any mismatch. The
 * recompute is a pure function of current state (never of current bounds), so a
 * refresh always converges instead of stacking.</p>
 */
public final class CrawlingScaleService {
    /** Tolerance for float-exact recompute comparison. */
    private static final float EPSILON = 1.0E-5F;

    private CrawlingScaleService() {
    }

    /** Drift enforcer: refresh dimensions whenever live bounds leave form default. */
    public static void tick(Player player) {
        JsonObject crawling = crawlingPower(player);
        JsonObject conditional = conditionScalePower(player);
        if (crawling == null && conditional == null) {
            return;
        }
        Pose pose = player.getPose();
        EntityDimensions expected = expectedDimensions(player, pose);
        float expectedEye = expectedEyeHeight(player, pose);
        if (Math.abs(player.getBbWidth() - expected.width) > EPSILON
                || Math.abs(player.getBbHeight() - expected.height) > EPSILON
                || Math.abs(player.getEyeHeight() - expectedEye) > EPSILON) {
            player.refreshDimensions();
        }
    }

    /**
     * Authoritative form bounds: pristine vanilla pose base × form scale × crawling
     * scale. Reads no live or event state, so every recompute converges instead of
     * stacking. Shared by the Size handler and the drift enforcer.
     */
    public static EntityDimensions expectedDimensions(Player player, Pose pose) {
        EntityDimensions base = vanillaPlayerDimensions(pose);
        FormDefinition form = FormManager.current(player);
        float formScale = conditionalScale(player, form.widthScale());
        return EntityDimensions.scalable(
                base.width * formScale,
                base.height * conditionalScale(player, form.heightScale()) * heightScale(player));
    }

    private static EntityDimensions vanillaPlayerDimensions(Pose pose) {
        return switch (pose) {
            case CROUCHING ->
                    EntityDimensions.scalable(0.6F, 1.5F);

            case SWIMMING, FALL_FLYING, SPIN_ATTACK ->
                    EntityDimensions.scalable(0.6F, 0.6F);

            case SLEEPING ->
                    EntityDimensions.fixed(0.2F, 0.2F);

            default ->
                    EntityDimensions.scalable(0.6F, 1.8F);
        };
    }

    /**
     * Pristine vanilla eye height per pose (verified against Player bytecode:
     * swimming/fall-flying/spin 0.4, crouching 1.27, sleeping 0.2, else 1.62).
     * The Size event's eye input derives from live state and stacks the same way
     * dimensions did, so it is never read either.
     */
    private static float vanillaPlayerEyeHeight(Pose pose) {
        return switch (pose) {
            case SLEEPING -> 0.2F;
            case SWIMMING, FALL_FLYING, SPIN_ATTACK -> 0.4F;
            case CROUCHING -> 1.27F;
            default -> 1.62F;
        };
    }

    /** Authoritative eye height: pristine vanilla eye × form eye × crawling eye. */
    public static float expectedEyeHeight(Player player, Pose pose) {
        FormDefinition form = FormManager.current(player);
        return vanillaPlayerEyeHeight(pose) * conditionalEyeScale(player, form.eyeScale())
                * eyeScale(player);
    }

    private static float conditionalScale(Player player, float fallback) {
        JsonObject power = conditionScalePower(player);
        if (power == null) return fallback;
        boolean active = isActive(power, player);
        return FormPowerRuntime.floatValue(power, active ? "scale" : "original_scale", fallback);
    }

    private static float conditionalEyeScale(Player player, float fallback) {
        JsonObject power = conditionScalePower(player);
        if (power == null) return fallback;
        boolean active = isActive(power, player);
        return FormPowerRuntime.floatValue(power, active ? "eye_scale" : "original_eye_scale", fallback);
    }

    /** Height multiplier from the crawling power (active ? active_scale : scale). */
    public static float heightScale(Player player) {
        JsonObject power = crawlingPower(player);
        if (power == null) {
            return 1.0F;
        }
        boolean active = isActive(power, player);
        return FormPowerRuntime.floatValue(power, active ? "active_scale" : "scale",
                active ? 0.6F : 1.0F);
    }

    /** Eye multiplier from the crawling power (active ? active_eye_scale : eye_scale). */
    public static float eyeScale(Player player) {
        JsonObject power = crawlingPower(player);
        if (power == null) {
            return 1.0F;
        }
        boolean active = isActive(power, player);
        return FormPowerRuntime.floatValue(power, active ? "active_eye_scale" : "eye_scale",
                active ? 0.35F : 1.0F);
    }

    private static boolean isActive(JsonObject power, Player player) {
        if (!power.has("condition")
                || FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
            return true;
        }
        // The automatic one-block crawl is represented by keep_sneaking.  Its
        // purpose is to force the crawl state even though the player is not
        // physically holding sneak, so the crawling power's active scale must
        // follow that state as well.
        return "shape-shifter-curse:crawling".equals(FormPowerRegistry.typeOf(power))
                && isForcedCrawling(player);
    }

    public static boolean isForcedCrawling(Player player) {
        if (player.isInWater() || player.isPassenger() || player.isSpectator()) {
            return false;
        }
        final boolean[] forced = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!forced[0] && "shape-shifter-curse:keep_sneaking".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                forced[0] = true;
            }
        });
        return forced[0];
    }

    private static JsonObject conditionScalePower(Player player) {
        final JsonObject[] found = {null};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (found[0] == null && "shape-shifter-curse:condition_scale".equals(FormPowerRegistry.typeOf(power))) {
                found[0] = power;
            }
        });
        return found[0];
    }

    private static JsonObject crawlingPower(Player player) {
        final JsonObject[] found = {null};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (found[0] == null && "shape-shifter-curse:crawling".equals(FormPowerRegistry.typeOf(power))) {
                found[0] = power;
            }
        });
        return found[0];
    }
}
