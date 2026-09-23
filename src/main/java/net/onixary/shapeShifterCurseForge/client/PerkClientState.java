package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

/** Client prediction copy of the local player's server-authoritative unlocked Perk ids. */
public final class PerkClientState {
    private static Set<ResourceLocation> unlocked = Set.of();

    private PerkClientState() {
    }

    public static boolean hasUnlocked(ResourceLocation perkId) {
        return perkId != null && unlocked.contains(perkId);
    }

    public static void replace(Set<ResourceLocation> perkIds) {
        unlocked = perkIds == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(perkIds));
    }
}
