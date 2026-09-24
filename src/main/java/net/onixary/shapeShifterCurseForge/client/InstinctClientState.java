package net.onixary.shapeShifterCurseForge.client;

/** Server snapshot used to render the player's instinct meter. */
public final class InstinctClientState {
    private static float value;
    private static float rate;
    private static boolean visible;
    private static boolean locked;

    private InstinctClientState() {
    }

    public static void set(float nextValue, float nextRate, boolean nextVisible, boolean nextLocked) {
        value = Math.max(0.0F, Math.min(100.0F, nextValue));
        rate = nextRate;
        visible = nextVisible;
        locked = nextLocked;
    }

    public static float value() { return value; }
    public static float rate() { return rate; }
    public static boolean visible() { return visible; }
    public static boolean locked() { return locked; }
}
