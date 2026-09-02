package net.onixary.shapeShifterCurseForge.form;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.capability.IPlayerFormData;
import net.onixary.shapeShifterCurseForge.capability.ModCapabilities;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;

public final class FormManager {
    private FormManager() {
    }

    public static void initialize() {
        FormRegistry.bootstrap();
    }

    public static FormDefinition current(Player player) {
        IPlayerFormData data = player.getCapability(ModCapabilities.PLAYER_FORM).orElse(null);
        if (data == null) {
            return FormRegistry.get(FormRegistry.ORIGINAL_BEFORE_ENABLE);
        }
        FormDefinition current = FormRegistry.get(data.getFormId());
        return current == null ? FormRegistry.get(FormRegistry.ORIGINAL_BEFORE_ENABLE) : current;
    }

    public static boolean setForm(Player player, ResourceLocation targetId) {
        FormDefinition target = FormRegistry.get(targetId);
        if (target == null) {
            return false;
        }

        boolean changed = player.getCapability(ModCapabilities.PLAYER_FORM).map(data -> {
            String currentId = data.getFormId();
            if (!target.id().toString().equals(currentId)) {
                data.setPreviousFormId(currentId);
                data.setFormId(target.id().toString());
                data.setFormGroupId(target.groupId().toString());
                data.setFormTier(target.tier());
            }
            data.setContentEnabled(!FormRegistry.ORIGINAL_BEFORE_ENABLE.equals(target.id()));
            return !target.id().toString().equals(currentId);
        }).orElse(false);

        if (player instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendFormSync(serverPlayer);
        }
        return changed;
    }

    public static boolean setForm(Player player, String targetId) {
        ResourceLocation parsed = ResourceLocation.tryParse(targetId);
        return parsed != null && setForm(player, parsed);
    }

    public static boolean moveToTier(Player player, int tier) {
        FormDefinition current = current(player);
        FormGroup group = FormRegistry.getGroup(current.groupId());
        if (group == null) {
            return false;
        }
        FormDefinition target = group.firstAtTier(tier);
        return target != null && setForm(player, target.id());
    }

    public static boolean next(Player player) {
        FormDefinition current = current(player);
        FormGroup group = FormRegistry.getGroup(current.groupId());
        if (group == null) {
            return false;
        }
        FormDefinition target = group.firstAtTier(current.tier() + 1);
        return target != null && setForm(player, target.id());
    }

    public static boolean previous(Player player) {
        FormDefinition current = current(player);
        FormGroup group = FormRegistry.getGroup(current.groupId());
        if (group == null) {
            return false;
        }
        FormDefinition target = group.firstAtTier(current.tier() - 1);
        return target != null && setForm(player, target.id());
    }

    public static void applySyncedForm(Player player, String formId, String groupId, int tier, boolean enabled) {
        player.getCapability(ModCapabilities.PLAYER_FORM).ifPresent(data -> {
            data.setFormId(formId);
            data.setFormGroupId(groupId);
            data.setFormTier(tier);
            data.setContentEnabled(enabled);
        });
    }
}
