package net.onixary.shapeShifterCurseForge.other.initial;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.onixary.shapeShifterCurseForge.capability.ModCapabilities;
import net.onixary.shapeShifterCurseForge.capability.PlayerFormData;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.other.config.SscCommonConfig;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InitialFormService {
    public static final GameRules.Key<GameRules.BooleanValue> USE_INITIAL_FORM = GameRules.register(
            "sscUseInitialForm", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    private InitialFormService() {}
    public static void initialize() { }

    public static void applyOnce(ServerPlayer player) {
        player.getCapability(ModCapabilities.PLAYER_FORM).ifPresent(data -> {
            if (!(data instanceof PlayerFormData actual) || !actual.takeInitialFormPending()) return;
            if (!SscCommonConfig.ENABLE_INITIAL_FORM.get() || !player.level().getGameRules().getBoolean(USE_INITIAL_FORM)) return;
            ResourceLocation chosen = choose(player, SscCommonConfig.INITIAL_FORMS.get());
            if (!FormRegistry.ORIGINAL_BEFORE_ENABLE.equals(chosen)) FormManager.setForm(player, chosen, false);
        });
    }

    public static ResourceLocation choose(ServerPlayer player, java.util.List<? extends String> entries) {
        Map<ResourceLocation, Integer> weights = new LinkedHashMap<>();
        for (String entry : entries) {
            String[] parts = entry.trim().split(":");
            if (parts.length < 2 || parts.length > 3) continue;
            ResourceLocation id = ResourceLocation.tryParse(parts[0] + ":" + parts[1]);
            int weight = 1;
            if (parts.length == 3) try { weight = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) { }
            if (id != null && FormRegistry.get(id) != null && weight > 0) weights.put(id, weight);
        }
        long total = weights.values().stream().mapToLong(Integer::longValue).sum();
        if (total <= 0) return FormRegistry.ORIGINAL_BEFORE_ENABLE;
        long choice = (long) (player.getRandom().nextDouble() * total);
        for (var entry : weights.entrySet()) {
            choice -= entry.getValue();
            if (choice < 0) return entry.getKey();
        }
        return FormRegistry.ORIGINAL_BEFORE_ENABLE;
    }
}
