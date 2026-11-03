package net.onixary.shapeShifterCurseForge.other.perk;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.api.registry.Perk;
import net.onixary.shapeShifterCurseForge.api.registry.PerkTree;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;
import net.onixary.shapeShifterCurseForge.block.entity.FormAttunerBlockEntity;

import java.util.Objects;

/** Server-authoritative Perk query and purchase API. */
public final class PerkService {
    private PerkService() {
    }

    public static boolean hasUnlocked(ServerPlayer player, ResourceLocation perkId) {
        return player != null && perkId != null
                && SscApi.currentForm(player).map(data -> data.hasUnlockedPerk(perkId)).orElse(false);
    }

    /**
     * Attempts one purchase from the player's last-used Form Attuner. Call only from server-side
     * menu/network handling; clients must never mutate the capability directly.
     */
    public static UnlockResult unlock(ServerPlayer player, ResourceLocation perkId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(perkId, "perkId");
        Perk perk = SscJavaRegistries.perk(perkId).orElse(null);
        PerkTree tree = SscJavaRegistries.perkTreeFor(perkId).orElse(null);
        if (perk == null || tree == null) return UnlockResult.UNKNOWN_PERK;
        if (!perk.implemented()) return UnlockResult.NOT_IMPLEMENTED;

        var data = SscApi.currentForm(player).orElse(null);
        if (data == null) return UnlockResult.NO_PLAYER_DATA;
        if (data.hasUnlockedPerk(perkId)) return UnlockResult.ALREADY_UNLOCKED;
        if (!tree.formGroupId().toString().equals(data.getFormGroupId())) return UnlockResult.WRONG_FORM_GROUP;
        if (!tree.matchesForm(ResourceLocation.tryParse(data.getFormId()))) return UnlockResult.WRONG_FORM_GROUP;

        FormAttunerBlockEntity attuner = FormAttunerBlockEntity.getLastUsed(player);
        if (attuner == null || player.distanceToSqr(attuner.getBlockPos().getX() + 0.5,
                attuner.getBlockPos().getY() + 0.5, attuner.getBlockPos().getZ() + 0.5) > 64
                || attuner.getAttunementLevel() < perk.requiredAttunerLevel()) {
            return UnlockResult.ATTUNER_TOO_WEAK;
        }
        if (!SscJavaRegistries.requiredPerks(perkId).stream().allMatch(data::hasUnlockedPerk)) {
            return UnlockResult.MISSING_PREREQUISITE;
        }
        if (!perk.canUnlock(player)) return UnlockResult.CUSTOM_REQUIREMENT_FAILED;
        if (!player.getAbilities().instabuild && (player.experienceLevel < perk.experienceLevels()
                || player.totalExperience < perk.experiencePoints())) {
            return UnlockResult.INSUFFICIENT_EXPERIENCE;
        }
        var costItem = perk.costItem() == null ? null : net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(perk.costItem());
        int available = 0;
        if (costItem != null) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                var stack = player.getInventory().getItem(i);
                if (stack.is(costItem)) available += stack.getCount();
            }
        }
        if (!player.getAbilities().instabuild && perk.itemCost() > available) return UnlockResult.INSUFFICIENT_ITEMS;

        if (!player.getAbilities().instabuild) {
            player.giveExperienceLevels(-perk.experienceLevels());
            player.giveExperiencePoints(-perk.experiencePoints());
            int remaining = perk.itemCost();
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                var stack = player.getInventory().getItem(i);
                if (costItem != null && stack.is(costItem)) {
                    int amount = Math.min(stack.getCount(), remaining);
                    stack.shrink(amount);
                    remaining -= amount;
                }
            }
            player.getInventory().setChanged();
        }
        data.unlockPerk(perkId);
        perk.onUnlocked(player);
        net.onixary.shapeShifterCurseForge.network.ModNetwork.sendFormSync(player);
        net.onixary.shapeShifterCurseForge.power.FormActivePowerService.synchronizeMana(player);
        return UnlockResult.SUCCESS;
    }

    public enum UnlockResult {
        SUCCESS,
        UNKNOWN_PERK,
        NO_PLAYER_DATA,
        ALREADY_UNLOCKED,
        WRONG_FORM_GROUP,
        ATTUNER_TOO_WEAK,
        MISSING_PREREQUISITE,
        CUSTOM_REQUIREMENT_FAILED,
        INSUFFICIENT_EXPERIENCE,
        INSUFFICIENT_ITEMS,
        NOT_IMPLEMENTED
    }
}
