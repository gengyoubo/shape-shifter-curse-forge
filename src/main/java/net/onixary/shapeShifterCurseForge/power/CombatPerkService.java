package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Applies combat perks to the primary melee hit and its actual health loss. */
public final class CombatPerkService {
    private CombatPerkService() { }

    public static float bonus(Player player, Entity target, boolean critical) {
        final float[] bonus = {0};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            String event = FormPowerRuntime.stringValue(power, "event", "melee");
            if ("shape-shifter-curse:action_on_combat_hit".equals(FormPowerRegistry.typeOf(power))
                    && ("melee".equals(event) || (!critical && "non_critical_melee".equals(event)))
                    && FormPowerRuntime.test(player, target, power.getAsJsonObject("condition"))
                    && FormPowerRuntime.test(player, target, power.getAsJsonObject("bientity_condition"))) {
                bonus[0] += FormPowerRuntime.floatValue(power, "bonus_damage", 0);
            }
        });
        return bonus[0];
    }

    public static void fire(Player player, LivingEntity target, String event, float damage) {
        if (player.level().isClientSide) return;
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:action_on_combat_hit".equals(FormPowerRegistry.typeOf(power))
                    && event.equals(FormPowerRuntime.stringValue(power, "event", "melee"))
                    && damage > FormPowerRuntime.floatValue(power, "min_damage", 0)
                    && FormPowerRuntime.test(player, target, power.getAsJsonObject("condition"))
                    && FormPowerRuntime.test(player, target, power.getAsJsonObject("bientity_condition"))) {
                FormPowerRuntime.executeBiEntity(player, target, power.getAsJsonObject("bientity_action"));
            }
        });
    }

    public static float health(Entity target) {
        return target instanceof LivingEntity living ? living.getHealth() + living.getAbsorptionAmount() : 0;
    }
}
