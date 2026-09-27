package net.onixary.shapeShifterCurseForge.animation;

/** Client lifetime measured on the world clock, independently of entity recreation. */
public final class PlaybackClock {
    public static final int HEARTBEAT_TICKS = 100;
    public static final int LOOP_LEASE_TICKS = 120;
    private final double startedAt;
    private double expiresAt;

    public PlaybackClock(double now, int durationTicks) {
        startedAt = now;
        renew(now, durationTicks);
    }

    public void renew(double now, int durationTicks) {
        expiresAt = durationTicks < 0 ? Double.POSITIVE_INFINITY : now + durationTicks;
    }

    public boolean expired(double now) { return now >= expiresAt; }

    public float seconds(double now, float speed) {
        return (float) (Math.max(0, now - startedAt) * speed / 20.0D);
    }
}
