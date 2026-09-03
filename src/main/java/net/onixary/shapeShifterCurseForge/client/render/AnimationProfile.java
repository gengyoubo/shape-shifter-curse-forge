package net.onixary.shapeShifterCurseForge.client.render;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes how a logical form animation is stored and played.
 *
 * <p>The Fabric animation system stores an animation identifier separately from its
 * controller settings.  Forge must also keep the resource file and the Bedrock
 * animation key separate: a file is allowed to contain several animations, and a
 * file name is not required to match its animation key.</p>
 */
public final class AnimationProfile {
    private final Map<String, Clip> clips;
    private final AnimationProfile fallback;

    private AnimationProfile(Map<String, Clip> clips, AnimationProfile fallback) {
        this.clips = Collections.unmodifiableMap(new LinkedHashMap<>(clips));
        this.fallback = fallback;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Clip get(String logicalId) {
        Clip clip = clips.get(logicalId);
        if (clip != null) {
            return clip;
        }
        return fallback == null ? null : fallback.get(logicalId);
    }

    public Map<String, Clip> clips() {
        return clips;
    }

    public record Clip(String resourceFile, String animationId, float speed, int fade) {
        public Clip {
            if (resourceFile == null || resourceFile.isBlank()) {
                throw new IllegalArgumentException("Animation resource file cannot be blank");
            }
            if (animationId == null || animationId.isBlank()) {
                throw new IllegalArgumentException("Animation id cannot be blank");
            }
            if (!Float.isFinite(speed) || speed <= 0.0F) {
                throw new IllegalArgumentException("Animation speed must be positive and finite");
            }
            if (fade < 0) {
                throw new IllegalArgumentException("Animation fade cannot be negative");
            }
        }
    }

    public static final class Builder {
        private final Map<String, Clip> clips = new LinkedHashMap<>();
        private AnimationProfile fallback;

        /** Maps a logical id to a same-named resource and animation key. */
        public Builder animation(String logicalId, String animationId) {
            return animation(logicalId, logicalId, animationId, 1.0F, 2);
        }

        /** Maps a logical id to a resource file while keeping the default fade. */
        public Builder animation(String logicalId, String resourceFile, String animationId, float speed) {
            return animation(logicalId, resourceFile, animationId, speed, 2);
        }

        /**
         * Maps a logical id to an explicit file, internal animation key, speed and fade.
         * Resource files are relative to assets/shape-shifter-curse/player_animation.
         */
        public Builder animation(String logicalId, String resourceFile, String animationId,
                                 float speed, int fade) {
            if (logicalId == null || logicalId.isBlank()) {
                throw new IllegalArgumentException("Animation logical id cannot be blank");
            }
            clips.put(logicalId, new Clip(resourceFile, animationId, speed, fade));
            return this;
        }

        public Builder alias(String logicalId, String targetAnimationId, float speed, int fade) {
            return animation(logicalId, targetAnimationId, targetAnimationId, speed, fade);
        }

        public Builder fallback(AnimationProfile fallback) {
            this.fallback = fallback;
            return this;
        }

        public AnimationProfile build() {
            return new AnimationProfile(clips, fallback);
        }
    }
}
