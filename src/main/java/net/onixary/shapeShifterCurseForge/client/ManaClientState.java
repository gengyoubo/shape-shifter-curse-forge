package net.onixary.shapeShifterCurseForge.client;

/** Client-only snapshot of the server-authoritative active mana pool. */
public final class ManaClientState {
    private static String type = "";
    private static float amount;
    private static float maximum = 20.0F;

    private ManaClientState() {}

    public static void set(String manaType, float mana, float maxMana) {
        type = manaType == null ? "" : manaType;
        amount = Math.max(0.0F, mana);
        maximum = Math.max(1.0F, maxMana);
    }

    public static boolean available() { return !type.isBlank(); }
    public static float amount() { return amount; }
    public static float maximum() { return maximum; }
}
