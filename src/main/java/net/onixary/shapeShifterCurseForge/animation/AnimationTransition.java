package net.onixary.shapeShifterCurseForge.animation;

/** Transition settings are separate from easing authored inside a clip's keyframes. */
public record AnimationTransition(Easing easing, boolean skipFade) {
    public static final AnimationTransition DEFAULT = new AnimationTransition(Easing.LINEAR, false);

    public AnimationTransition {
        if (easing == null) easing = Easing.LINEAR;
    }

    public float blend(double elapsedTicks, int fadeTicks) {
        if (skipFade || fadeTicks <= 0) return 1;
        float progress = (float) Math.max(0, Math.min(1, elapsedTicks / fadeTicks));
        return easing.apply(progress);
    }

    public static float rotation(float from, float to, float amount) {
        return rotation(from, to, amount, (float) (Math.PI * 2));
    }

    public static float rotationDegrees(float from, float to, float amount) {
        return rotation(from, to, amount, 360);
    }

    private static float rotation(float from, float to, float amount, float turn) {
        float delta = (to - from) % turn;
        if (delta > turn / 2) delta -= turn;
        else if (delta < -turn / 2) delta += turn;
        return from + delta * amount;
    }

    public enum Easing {
        LINEAR, IN_SINE, OUT_SINE, IN_OUT_SINE, IN_QUAD, OUT_QUAD, IN_OUT_QUAD,
        IN_CUBIC, OUT_CUBIC, IN_OUT_CUBIC, IN_QUART, OUT_QUART, IN_OUT_QUART;

        public float apply(float x) {
            return switch (this) {
                case LINEAR -> x;
                case IN_SINE -> (float) (1 - Math.cos(x * Math.PI / 2));
                case OUT_SINE -> (float) Math.sin(x * Math.PI / 2);
                case IN_OUT_SINE -> (float) ((1 - Math.cos(x * Math.PI)) / 2);
                case IN_QUAD -> x * x;
                case OUT_QUAD -> 1 - (1 - x) * (1 - x);
                case IN_OUT_QUAD -> x < .5f ? 2 * x * x : 1 - (float) Math.pow(-2 * x + 2, 2) / 2;
                case IN_CUBIC -> x * x * x;
                case OUT_CUBIC -> 1 - (float) Math.pow(1 - x, 3);
                case IN_OUT_CUBIC -> x < .5f ? 4 * x * x * x : 1 - (float) Math.pow(-2 * x + 2, 3) / 2;
                case IN_QUART -> x * x * x * x;
                case OUT_QUART -> 1 - (float) Math.pow(1 - x, 4);
                case IN_OUT_QUART -> x < .5f ? 8 * x * x * x * x : 1 - (float) Math.pow(-2 * x + 2, 4) / 2;
            };
        }
    }
}
