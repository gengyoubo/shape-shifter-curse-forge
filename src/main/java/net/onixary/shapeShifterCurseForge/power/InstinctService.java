package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.api.PlayerFormData;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormGrowthService;
import net.onixary.shapeShifterCurseForge.form.FormManager;

/** Persisted replacement for Cardinal Components' instinct meter and timed instinct effects. */
public final class InstinctService {
    private static final float MAX_INSTINCT = 100.0F;
    // Fabric uses 180s (3600 ticks) to fill 0->100; 180_000 ticks would be 2.5h and is ms-vs-ticks confusion.
    private static final float BASE_RATE = MAX_INSTINCT / 3600.0F;

    private InstinctService() { }

    public static float value(Player player) {
        return SscApi.currentForm(player).map(PlayerFormData::getInstinctValue).orElse(0.0F);
    }

    public static void add(Player player, String effectId, float amount, int duration, boolean immediate) {
        SscApi.currentForm(player).ifPresent(data -> {
            FormDefinition form = FormManager.current(player);
            if (form.hasFlag("no_instinct") || form.hasFlag("lock_instinct")) return;
            if (immediate) {
                data.setInstinctValue(value(player) + amount * Math.max(1, duration));
                return;
            }
            CompoundTag effects = data.getInstinctEffects();
            CompoundTag effect = new CompoundTag();
            effect.putFloat("Value", amount);
            effect.putInt("Duration", Math.max(0, duration));
            effects.put(effectId, effect);
            data.setInstinctEffects(effects);
        });
    }

    public static void tick(ServerPlayer player) {
        SscApi.currentForm(player).ifPresent(data -> {
            FormDefinition form = FormManager.current(player);
            if (form.hasFlag("no_instinct")) {
                data.setInstinctValue(0.0F);
                data.setInstinctRate(0.0F);
                data.setInstinctEffects(new CompoundTag());
                return;
            }
            CompoundTag effects = data.getInstinctEffects();
            // drain at ~2x fill rate when locked, not instant -100/tick
            float rate = form.hasFlag("lock_instinct") ? -BASE_RATE * 2.0F : BASE_RATE;
            for (String id : java.util.List.copyOf(effects.getAllKeys())) {
                CompoundTag effect = effects.getCompound(id);
                int dur = effect.getInt("Duration");
                if (dur <= 0) { effects.remove(id); continue; }
                rate += effect.getFloat("Value");
                effect.putInt("Duration", dur - 1);
            }
            data.setInstinctEffects(effects);
            data.setInstinctRate(rate);
            float next = value(player) + rate;
            // clamp 0..MAX and preserve overshoot via modulo
            if (next < 0.0F) next = 0.0F;
            else if (next >= MAX_INSTINCT) {
                float overshoot = next - MAX_INSTINCT;
                FormGrowthService.advanceByInstinct(player);
                // carry overshoot, clamped
                next = Math.max(0.0F, Math.min(overshoot, MAX_INSTINCT - 0.001F));
                data.setInstinctEffects(new CompoundTag());
            }
            data.setInstinctValue(Math.max(0.0F, Math.min(next, MAX_INSTINCT)));
        });
    }

    public static void applyImmediatePowers(ServerPlayer player) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:add_immediate_instinct".equals(FormPowerRegistry.typeOf(power))) {
                add(player, FormPowerRuntime.stringValue(power, "instinct_effect_id", id.toString()),
                        FormPowerRuntime.floatValue(power, "value", 0.0F), 1, true);
            }
        });
    }
}
