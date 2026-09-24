package net.onixary.shapeShifterCurseForge.integration.toughasnails;

import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

/** Runtime helpers shared by the optional Tough As Nails mixins. */
public final class ToughAsNailsPowerUtils {
    private ToughAsNailsPowerUtils() { }

    public static int modifyFormTemperatureOrdinal(Player player, int currentOrdinal) {
        if (player == null || !ToughAsNailsIntegration.isLoaded()) return currentOrdinal;
        int[] modified = {currentOrdinal};
        boolean[] found = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (found[0] || !"shape-shifter-curse:tan_form_temperature_modifier".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            int clamped = Math.max(0, Math.min(4, currentOrdinal));
            String key = switch (clamped) {
                case 0 -> "icy_offset";
                case 1 -> "cold_offset";
                case 2 -> "neutral_offset";
                case 3 -> "warm_offset";
                default -> "hot_offset";
            };
            modified[0] = Math.max(0, Math.min(4,
                    clamped + FormPowerRuntime.intValue(power, key, 0)));
            found[0] = true;
        });
        return modified[0];
    }

    public static boolean shouldPreventDirtyWaterThirstEffect(Player player) {
        if (player == null || !ToughAsNailsIntegration.isLoaded()) return false;
        boolean[] prevent = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!prevent[0]
                    && "shape-shifter-curse:tan_prevent_dirty_water_thirst_effect".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                prevent[0] = true;
            }
        });
        return prevent[0];
    }

    /** Convert the result of the optional TAN enum method without linking TAN classes at compile time. */
    public static Object modifyTemperatureValue(Player player, Object original) {
        if (!(original instanceof Enum<?> temperature)) return original;
        Object[] values = temperature.getDeclaringClass().getEnumConstants();
        if (values == null || values.length == 0) return original;
        int ordinal = modifyFormTemperatureOrdinal(player, temperature.ordinal());
        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }
}
