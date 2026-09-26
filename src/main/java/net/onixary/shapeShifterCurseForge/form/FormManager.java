package net.onixary.shapeShifterCurseForge.form;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.api.PlayerFormData;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.other.advancement.SscAdvancementTriggers;
import net.onixary.shapeShifterCurseForge.power.InstinctService;
import net.onixary.shapeShifterCurseForge.power.BatAttachService;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;
import net.onixary.shapeShifterCurseForge.power.FormPowerEvents;
import net.onixary.shapeShifterCurseForge.power.MissingPowerEvents;
import net.onixary.shapeShifterCurseForge.power.PowerAnimationService;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerSyncGuard;

public final class FormManager {
    private FormManager() {
    }

    public static void initialize() {
        FormRegistry.bootstrap();
    }

    public static FormDefinition current(Player player) {
        PlayerFormData data = SscApi.currentForm(player).orElse(null);
        if (data == null) {
            return FormRegistry.get(FormRegistry.ORIGINAL_BEFORE_ENABLE);
        }
        FormDefinition current = FormRegistry.get(data.getFormId());
        return current == null ? FormRegistry.get(FormRegistry.ORIGINAL_BEFORE_ENABLE) : current;
    }

    public static boolean setForm(Player player, ResourceLocation targetId) {
        return setForm(player, targetId, true);
    }

    /** @param playTransformAnimation whether clients play the transform clip on change. */
    public static boolean setForm(Player player, ResourceLocation targetId, boolean playTransformAnimation) {
        FormDefinition target = FormRegistry.get(targetId);
        if (target == null) {
            return false;
        }

        boolean changed = SscApi.currentForm(player).map(data -> {
            String currentId = data.getFormId();
            boolean differentForm = !target.id().toString().equals(currentId);
            if (differentForm) {
                data.setPreviousFormId(currentId);
                data.setFormId(target.id().toString());
                data.setFormGroupId(target.groupId().toString());
                data.setFormTier(target.stage());
                player.refreshDimensions();
            }
            data.setContentEnabled(!FormRegistry.ORIGINAL_BEFORE_ENABLE.equals(target.id()));
            return differentForm;
        }).orElse(false);

        if (changed) {
            if (player instanceof ServerPlayer serverPlayer) {
                FormActivePowerService.onFormChanged(serverPlayer);
                BatAttachService.clear(serverPlayer);
                PowerAnimationService.stop(serverPlayer);
            }
            FormPowerEvents.onFormChanged(player);
            if (player instanceof ServerPlayer serverPlayer) {
                MissingPowerEvents.onFormChanged(serverPlayer);
            }
            FormPowerSyncGuard.markReconciled(player);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendFormSync(serverPlayer, changed && playTransformAnimation);
            net.onixary.shapeShifterCurseForge.power.FormActivePowerService.synchronizeMana(serverPlayer);
            InstinctService.synchronizeHud(serverPlayer);
            if (changed) {
                SscAdvancementTriggers.ON_TRANSFORM_FORM.triggerForm(serverPlayer, target);
                InstinctService.applyImmediatePowers(serverPlayer);
            }
        }
        return changed;
    }

    public static boolean setForm(Player player, String targetId) {
        ResourceLocation parsed = ResourceLocation.tryParse(targetId);
        return parsed != null && setForm(player, parsed);
    }

    /** Moves within the player's current progression branch to a requested stage. */
    public static boolean moveToStage(Player player, int stage) {
        FormDefinition current = current(player);
        FormDefinition target = FormRegistry.formAtStageInProgression(current, stage);
        return target != null && setForm(player, target.id());
    }

    /** @deprecated Use {@link #moveToStage(Player, int)}. */
    @Deprecated()
    public static boolean moveToTier(Player player, int tier) {
        return moveToStage(player, tier);
    }

    public static boolean next(Player player) {
        FormDefinition current = current(player);
        FormDefinition target = FormRegistry.nextInProgression(current);
        return target != null && setForm(player, target.id());
    }

    public static boolean previous(Player player) {
        FormDefinition current = current(player);
        FormDefinition target = FormRegistry.previousInProgression(current);
        return target != null && setForm(player, target.id());
    }

    public static void applySyncedForm(Player player, String formId, String groupId, int tier, boolean enabled,
                                       java.util.List<ResourceLocation> assignedPowerIds) {
        boolean changed = SscApi.currentForm(player).map(data -> {
            boolean formChanged = !data.getFormId().equals(formId);
            data.setFormId(formId);
            data.setFormGroupId(groupId);
            data.setFormTier(tier);
            data.setContentEnabled(enabled);
            player.refreshDimensions();
            return formChanged;
        }).orElse(false);
        ResourceLocation syncedFormId = ResourceLocation.tryParse(formId);
        boolean powerSetChanged = FormPowerRegistry.applySyncedPowerIds(player, syncedFormId, assignedPowerIds);
        if (changed || powerSetChanged) {
            FormPowerEvents.onFormChanged(player);
        }
    }
}
