package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.advancement.SscAdvancementTriggers;
import net.onixary.shapeShifterCurseForge.api.PlayerFormData;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Forge-side state machine for Fabric's temporary transformative effects. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class TransformativeEffectService {
    public static final int DEFAULT_DURATION = 400 * 20;
    private static final Map<UUID, Boolean> WAS_SLEEPING = new HashMap<>();

    private TransformativeEffectService() {
    }

    public static boolean apply(ServerPlayer player, ResourceLocation targetId) {
        FormDefinition target = FormRegistry.get(targetId);
        if (target == null || !canHaveEffect(player)) {
            return false;
        }
        SscApi.currentForm(player).ifPresent(data -> {
            data.setTransformativeEffectFormId(target.id().toString());
            data.setTransformativeEffectTicks(DEFAULT_DURATION);
        });
        SscAdvancementTriggers.ON_GET_TRANSFORM_EFFECT.trigger(player);
        return true;
    }

    public static boolean has(Player player) {
        return SscApi.currentForm(player).map(data ->
                data.getTransformativeEffectFormId() != null && data.getTransformativeEffectTicks() > 0
        ).orElse(false);
    }

    public static boolean clear(Player player) {
        return SscApi.currentForm(player).map(data -> {
            boolean hadEffect = hasData(data);
            data.setTransformativeEffectFormId(null);
            data.setTransformativeEffectTicks(0);
            return hadEffect;
        }).orElse(false);
    }

    public static boolean activate(ServerPlayer player) {
        ResourceLocation targetId = SscApi.currentForm(player)
                .map(PlayerFormData::getTransformativeEffectFormId)
                .map(ResourceLocation::tryParse)
                .orElse(null);
        if (targetId == null) {
            return false;
        }
        FormDefinition current = FormManager.current(player);
        boolean applied = current.hasFlag("transform_effect_can_apply")
                && FormRegistry.get(targetId) != null
                && FormManager.setForm(player, targetId);
        clear(player);
        return applied;
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean sleeping = player.isSleeping();
        boolean wasSleeping = WAS_SLEEPING.getOrDefault(player.getUUID(), false);
        if (wasSleeping && !sleeping && has(player)) {
            SscAdvancementTriggers.ON_SLEEP_WHEN_HAVE_TRANSFORM_EFFECT.trigger(player);
            activate(player);
        }
        WAS_SLEEPING.put(player.getUUID(), sleeping);

        SscApi.currentForm(player).ifPresent(data -> {
            if (!hasData(data)) {
                return;
            }
            int remaining = data.getTransformativeEffectTicks() - 1;
            data.setTransformativeEffectTicks(remaining);
            if (remaining <= 0) {
                data.setTransformativeEffectFormId(null);
                SscAdvancementTriggers.ON_TRANSFORM_EFFECT_FADE.trigger(player);
            }
        });
    }

    private static boolean canHaveEffect(Player player) {
        FormDefinition form = FormManager.current(player);
        return form.hasFlag("can_have_transform_effect")
                && form.hasFlag("transform_effect_can_apply");
    }

    private static boolean hasData(PlayerFormData data) {
        return data.getTransformativeEffectFormId() != null
                && !data.getTransformativeEffectFormId().isBlank()
                && data.getTransformativeEffectTicks() > 0;
    }
}
