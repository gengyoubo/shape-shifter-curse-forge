package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.capability.IPlayerFormData;
import net.onixary.shapeShifterCurseForge.capability.ModCapabilities;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormGrowthService;
import net.onixary.shapeShifterCurseForge.form.FormManager;

/** Persisted replacement for Cardinal Components' instinct meter and timed instinct effects. */
public final class InstinctService {
    private static final float MAX_INSTINCT = 100.0F;
    private static final float BASE_RATE = MAX_INSTINCT / 180_000.0F;

    private InstinctService() { }

    public static float value(Player player) {
        return player.getCapability(ModCapabilities.PLAYER_FORM).map(IPlayerFormData::getInstinctValue).orElse(0.0F);
    }

    public static void add(Player player, String effectId, float amount, int duration, boolean immediate) {
        player.getCapability(ModCapabilities.PLAYER_FORM).ifPresent(data -> {
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
        player.getCapability(ModCapabilities.PLAYER_FORM).ifPresent(data -> {
            FormDefinition form = FormManager.current(player);
            if (form.hasFlag("no_instinct")) {
                data.setInstinctValue(0.0F);
                data.setInstinctRate(0.0F);
                data.setInstinctEffects(new CompoundTag());
                return;
            }
            CompoundTag effects = data.getInstinctEffects();
            float rate = form.hasFlag("lock_instinct") ? -MAX_INSTINCT : BASE_RATE;
            for (String id : java.util.List.copyOf(effects.getAllKeys())) {
                CompoundTag effect = effects.getCompound(id);
                rate += effect.getFloat("Value");
                if (effect.getInt("Duration") <= 0) effects.remove(id);
                else effect.putInt("Duration", effect.getInt("Duration") - 1);
            }
            data.setInstinctEffects(effects);
            data.setInstinctRate(rate);
            data.setInstinctValue(value(player) + rate);
            if (value(player) >= MAX_INSTINCT) {
                FormGrowthService.advanceByInstinct(player);
                data.setInstinctValue(0.0F);
                data.setInstinctEffects(new CompoundTag());
            }
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
