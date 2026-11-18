package net.onixary.shapeShifterCurseForge.client;

import java.util.HashMap;
import java.util.Map;

/** Client-only snapshot of the server-authoritative active mana pool. */
public final class ManaClientState {
    private static String type = "";
    private static float amount;
    private static float maximum = 20.0F;
    /** Completed charge tier per {@code charge_power_id}; 0 while not charging. */
    private static final Map<String, Integer> CHARGE_TIERS = new HashMap<>();

    private ManaClientState() {}

    public static void set(String manaType, float mana, float maxMana) {
        String newType = manaType == null ? "" : manaType;
        if (!newType.equals(type)) CHARGE_TIERS.clear();
        type = newType;
        amount = Math.max(0.0F, mana);
        maximum = Math.max(1.0F, maxMana);
    }

    public static void setChargeTier(String chargeKey, int tier) {
        if (chargeKey == null || chargeKey.isBlank()) return;
        if (tier <= 0) CHARGE_TIERS.remove(chargeKey);
        else CHARGE_TIERS.put(chargeKey, tier);
    }

    public static int chargeTier(String chargeKey) {
        return CHARGE_TIERS.getOrDefault(chargeKey, 0);
    }

    public static boolean available() { return !type.isBlank(); }
    public static String type() { return type; }
    public static float amount() { return amount; }
    public static float maximum() { return maximum; }
}
