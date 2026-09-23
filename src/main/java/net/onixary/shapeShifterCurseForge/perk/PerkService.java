package net.onixary.shapeShifterCurseForge.perk;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.api.registry.Perk;
import net.onixary.shapeShifterCurseForge.api.registry.PerkTree;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;
import net.onixary.shapeShifterCurseForge.blockentity.FormAttunerBlockEntity;

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

        var data = SscApi.currentForm(player).orElse(null);
        if (data == null) return UnlockResult.NO_PLAYER_DATA;
        if (data.hasUnlockedPerk(perkId)) return UnlockResult.ALREADY_UNLOCKED;
        if (!tree.formGroupId().toString().equals(data.getFormGroupId())) return UnlockResult.WRONG_FORM_GROUP;

        FormAttunerBlockEntity attuner = FormAttunerBlockEntity.getLastUsed(player);
        if (attuner == null || attuner.getAttunementLevel() < perk.requiredAttunerLevel()) {
            return UnlockResult.ATTUNER_TOO_WEAK;
        }
        if (!SscJavaRegistries.requiredPerks(perkId).stream().allMatch(data::hasUnlockedPerk)) {
            return UnlockResult.MISSING_PREREQUISITE;
        }
        if (!perk.canUnlock(player)) return UnlockResult.CUSTOM_REQUIREMENT_FAILED;
        if (!player.getAbilities().instabuild && player.experienceLevel < perk.experienceLevels()) {
            return UnlockResult.INSUFFICIENT_EXPERIENCE;
        }

        if (!player.getAbilities().instabuild) {
            player.giveExperienceLevels(-perk.experienceLevels());
        }
        data.unlockPerk(perkId);
        perk.onUnlocked(player);
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
        INSUFFICIENT_EXPERIENCE
    }
}
