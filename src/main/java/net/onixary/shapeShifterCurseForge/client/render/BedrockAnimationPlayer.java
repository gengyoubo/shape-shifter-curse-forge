package net.onixary.shapeShifterCurseForge.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import software.bernie.geckolib.model.GeoModel;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;

/** Small runtime for the Bedrock/Azure-style animation JSON shipped by SSC. */
public final class BedrockAnimationPlayer {
    private static final float DEG_TO_RAD = (float) Math.PI / 180.0F;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<CacheKey, AnimationDefinition> CACHE = new HashMap<>();

    private BedrockAnimationPlayer() {
    }

    /**
     * Small, dependency-free equivalent of the Player Animation Lib path used by the
     * Fabric build. The library's Gecko JSON serializer writes animation values to a
     * {@link PlayerModel}, including its canonical limb pivots; SSC then copies that
     * completed model pose into its Geo model.
     *
     * <p>This intentionally covers only SSC's player-animation format. It is not a
     * replacement for Player Animation Lib's animation stack, networking, emotes, or
     * first-person API.</p>
     */
    public static BodyTransform applyToPlayerModel(PlayerModel<?> model, FormAnimationSystem.Selection selection,
                                                    float timeSeconds) {
        return applyToPlayerModel(model, selection, timeSeconds, false);
    }

    /** Applies an animation to PlayerModel, optionally repeating a non-looping source clip. */
    public static BodyTransform applyToPlayerModel(PlayerModel<?> model, FormAnimationSystem.Selection selection,
                                                    float timeSeconds, boolean forceLoop) {
        if (selection == null) {
            return BodyTransform.IDENTITY;
        }
        AnimationDefinition definition = load(selection.resource(), selection.animationId());
        if (definition == null && selection.fallbackResource() != null) {
            definition = load(selection.fallbackResource(), selection.animationId());
        }
        if (definition == null) {
            return BodyTransform.IDENTITY;
        }
        float time = animationTime(definition, timeSeconds, forceLoop);
        BodyTransform bodyTransform = BodyTransform.IDENTITY;
        for (Map.Entry<String, BoneAnimation> entry : definition.bones.entrySet()) {
            BoneAnimation animation = entry.getValue();
            Vec3 rotation = sample(animation.rotation, time);
            Vec3 position = sample(animation.position, time);
            switch (entry.getKey()) {
                case "head" -> applyPart(model.head, rotation, position, Pivot.HEAD);
                case "torso" -> applyPart(model.body, rotation, position, Pivot.TORSO);
                case "rightArm" -> applyPart(model.rightArm, rotation, position, Pivot.RIGHT_ARM);
                case "leftArm" -> applyPart(model.leftArm, rotation, position, Pivot.LEFT_ARM);
                case "rightLeg" -> applyPart(model.rightLeg, rotation, position, Pivot.RIGHT_LEG);
                case "leftLeg" -> applyPart(model.leftLeg, rotation, position, Pivot.LEFT_LEG);
                // PAL treats `body` as a renderer-level transform, distinct from
                // PlayerModel's `torso` part.
                case "body" -> bodyTransform = bodyTransform(rotation, position);
                default -> { }
            }
        }
        return bodyTransform;
    }

    /**
     * Mirrors PAL's PlayerModelMixin#setDefaultPivot. PlayerModel instances are reused
     * between renders, so vanilla setupAnim alone does not reliably clear positions
     * written by the previous SSC animation. This is especially visible when a crawl
     * pose changes directly into a sprint jump.
     */
    public static void resetVanillaPivots(PlayerModel<?> model) {
        resetPart(model.head, Pivot.HEAD);
        resetPart(model.body, Pivot.TORSO);
        resetPart(model.rightArm, Pivot.RIGHT_ARM);
        resetPart(model.leftArm, Pivot.LEFT_ARM);
        resetPart(model.rightLeg, Pivot.RIGHT_LEG);
        resetPart(model.leftLeg, Pivot.LEFT_LEG);
    }

    /** Returns whether PAL would still consider this clip playable at the supplied time. */
    public static boolean isActive(FormAnimationSystem.Selection selection, float timeSeconds) {
        if (selection == null) return false;
        AnimationDefinition definition = load(selection.resource(), selection.animationId());
        if (definition == null && selection.fallbackResource() != null) {
            definition = load(selection.fallbackResource(), selection.animationId());
        }
        return definition != null && (definition.loop || definition.holdLastFrame || timeSeconds < definition.length);
    }

    /**
     * Applies a Bedrock clip as an additive layer to an already prepared Geo model.
     *
     * <p>SSC's ordinary form clips target Player Animation Lib's PlayerModel bones and
     * must therefore run before {@link FormGeoModel} copies the final vanilla pose.
     * A few new clips instead target form-only bones (for example an axolotl tail), so
     * replacing that pose would be incorrect. The caller resets and prepares the Geo model
     * for each pass, so the clip is applied to the current prepared pose. This preserves the
     * form's procedural tail/wing motion and makes the JSON values true additive offsets.</p>
     */
    public static void applyAdditiveGeoRotation(GeoModel<?> model, ResourceLocation resource,
                                                String animationId, float timeSeconds) {
        AnimationDefinition definition = load(resource, animationId);
        if (definition == null) {
            return;
        }
        float time = animationTime(definition, timeSeconds);
        for (Map.Entry<String, BoneAnimation> entry : definition.bones.entrySet()) {
            Vec3 rotation = sample(entry.getValue().rotation, time);
            if (rotation == null) {
                continue;
            }
            model.getBone(entry.getKey()).ifPresent(bone -> {
                // FormGeoRenderer converts Bedrock model space to Forge Geo space with
                // a 180-degree X-axis turn. Form-only tail clips therefore need their
                // pitch inverted; Y/Z retain their native Geo direction.
                bone.setRotX(bone.getRotX() - rotation.x * DEG_TO_RAD);
                bone.setRotY(bone.getRotY() + rotation.y * DEG_TO_RAD);
                bone.setRotZ(bone.getRotZ() + rotation.z * DEG_TO_RAD);
            });
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * Samples an arbitrary named bone from a clip, mirroring PAL's
     * {@code AnimationApplier#get3DTransform}. Fabric's {@code ProcessExtraBone} reads
     * exactly these values for form-only bones (tail mounts, calves, hind legs, ...).
     * Rotation is returned in degrees and position in raw JSON units; callers convert
     * to radians and apply PAL's sign conventions themselves.
     */
    public static BoneSample sampleBone(ResourceLocation resource, String animationId,
                                        String boneName, float timeSeconds) {
        return sampleBone(resource, animationId, boneName, timeSeconds, false);
    }

    /** Samples a named bone, optionally repeating a non-looping source clip. */
    public static BoneSample sampleBone(ResourceLocation resource, String animationId,
                                        String boneName, float timeSeconds, boolean forceLoop) {
        AnimationDefinition definition = load(resource, animationId);
        if (definition == null) {
            return null;
        }
        BoneAnimation animation = definition.bones.get(boneName);
        if (animation == null) {
            return null;
        }
        float time = animationTime(definition, timeSeconds, forceLoop);
        Vec3 rotation = sample(animation.rotation, time);
        Vec3 position = sample(animation.position, time);
        if (rotation == null) {
            rotation = Vec3.ZERO;
        }
        if (position == null) {
            position = Vec3.ZERO;
        }
        return new BoneSample(rotation.x, rotation.y, rotation.z, position.x, position.y, position.z);
    }

    /** Raw per-bone sample: rotation in degrees, position in raw JSON units. */
    public record BoneSample(float rotX, float rotY, float rotZ, float posX, float posY, float posZ) {
        public static BoneSample lerp(BoneSample from, BoneSample to, float amount) {
            return new BoneSample(Mth.lerp(amount, from.rotX, to.rotX), Mth.lerp(amount, from.rotY, to.rotY),
                    Mth.lerp(amount, from.rotZ, to.rotZ), Mth.lerp(amount, from.posX, to.posX),
                    Mth.lerp(amount, from.posY, to.posY), Mth.lerp(amount, from.posZ, to.posZ));
        }
    }

    private static AnimationDefinition load(ResourceLocation resource, String animationId) {
        CacheKey cacheKey = new CacheKey(resource, animationId);
        AnimationDefinition cached = CACHE.get(cacheKey);
        if (cached != null) return cached;
        try {
            Optional<net.minecraft.server.packs.resources.Resource> optional =
                    Minecraft.getInstance().getResourceManager().getResource(resource);
            if (optional.isEmpty()) return null;
            try (InputStreamReader reader = new InputStreamReader(optional.get().open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject animations = root.getAsJsonObject("animations");
                if (animations == null) return null;
                JsonObject animation = animations.getAsJsonObject(animationId);
                if (animation == null && animations.size() == 1) {
                    animation = animations.entrySet().iterator().next().getValue().getAsJsonObject();
                }
                if (animation == null) return null;
                AnimationDefinition parsed = parse(animation);
                CACHE.put(cacheKey, parsed);
                return parsed;
            }
        } catch (Exception exception) {
            LOGGER.warn("Unable to load form animation {}", resource, exception);
            return null;
        }
    }

    /** Used by the selector so a malformed or incomplete new-animation file can fall back safely. */
    public static boolean hasAnimation(ResourceLocation resource, String animationId) {
        return load(resource, animationId) != null;
    }

    /** Returns a selected clip's source duration, including its legacy fallback. */
    public static float animationLength(FormAnimationSystem.Selection selection) {
        if (selection == null) return 0.0F;
        AnimationDefinition definition = load(selection.resource(), selection.animationId());
        if (definition == null && selection.fallbackResource() != null) {
            definition = load(selection.fallbackResource(), selection.animationId());
        }
        return definition == null ? 0.0F : definition.length;
    }

    private static float animationTime(AnimationDefinition definition, float timeSeconds) {
        return animationTime(definition, timeSeconds, false);
    }

    private static float animationTime(AnimationDefinition definition, float timeSeconds, boolean forceLoop) {
        float time = definition.length <= 0.0F ? 0.0F : timeSeconds;
        if (definition.loop || forceLoop) {
            time %= definition.length;
            if (time < 0.0F) time += definition.length;
        } else {
            time = Math.min(time, definition.length);
        }
        return time;
    }

    private static void applyPart(ModelPart part, Vec3 rotation, Vec3 position, Pivot pivot) {
        // GeckoLibSerializer calls fullyEnableParts: a bone with any animation channel
        // also resets the other channels to their canonical PlayerModel defaults.
        Vec3 rot = rotation == null ? Vec3.ZERO : rotation;
        float xRot = rot.x * DEG_TO_RAD;
        float yRot = rot.y * DEG_TO_RAD;
        float zRot = rot.z * DEG_TO_RAD;
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;

        Vec3 pos = position == null ? Vec3.ZERO : position;
        // PAL's Gecko parser stores normal limb position as (x, -y, z) plus the
        // default pivot. This is why raw JSON position must never directly replace a
        // GeoBone position.
        part.x = pivot.x + pos.x;
        part.y = pivot.y - pos.y;
        part.z = pivot.z + pos.z;
    }

    private static void resetPart(ModelPart part, Pivot pivot) {
        part.x = pivot.x;
        part.y = pivot.y;
        part.z = pivot.z;
        part.xRot = 0.0F;
        part.yRot = 0.0F;
        part.zRot = 0.0F;
    }

    private static BodyTransform bodyTransform(Vec3 rotation, Vec3 position) {
        Vec3 rot = rotation == null ? Vec3.ZERO : rotation;
        Vec3 pos = position == null ? Vec3.ZERO : position;
        // This is the special `body` branch in Player Animation Lib's
        // GeckoLibSerializer: translation is in model units and pitch/yaw are inverted.
        return new BodyTransform(-pos.x / 16.0F, pos.y / 16.0F, pos.z / 16.0F,
                -rot.x * DEG_TO_RAD, -rot.y * DEG_TO_RAD, rot.z * DEG_TO_RAD);
    }

    private static AnimationDefinition parse(JsonObject animation) {
        float length = animation.has("animation_length") ? animation.get("animation_length").getAsFloat() : 1.0F;
        Map<String, BoneAnimation> bones = new HashMap<>();
        JsonObject bonesJson = animation.getAsJsonObject("bones");
        if (bonesJson != null) {
            for (Map.Entry<String, JsonElement> entry : bonesJson.entrySet()) {
                JsonObject boneJson = entry.getValue().getAsJsonObject();
                BoneAnimation bone = new BoneAnimation(
                        channel(boneJson, "rotation"),
                        channel(boneJson, "position"),
                        channel(boneJson, "scale"));
                length = Math.max(length, bone.maxTime());
                bones.put(entry.getKey(), bone);
            }
        }
        boolean loop = false;
        boolean holdLastFrame = false;
        if (animation.has("loop")) {
            JsonElement loopValue = animation.get("loop");
            if (loopValue.isJsonPrimitive() && loopValue.getAsJsonPrimitive().isBoolean()) {
                loop = loopValue.getAsBoolean();
            } else if (loopValue.isJsonPrimitive() && loopValue.getAsJsonPrimitive().isString()) {
                // PAL maps this Gecko convention to an infinite animation whose
                // return tick is the final frame: keep the final pose, never wrap.
                holdLastFrame = "hold_on_last_frame".equals(loopValue.getAsString());
            }
        }
        return new AnimationDefinition(length, loop, holdLastFrame, bones);
    }

    private static Channel channel(JsonObject bone, String name) {
        if (!bone.has(name)) return Channel.EMPTY;
        JsonElement element = bone.get(name);
        if (element.isJsonArray()) return new Channel(List.of(new Keyframe(0.0F, vector(element), null)));
        if (!element.isJsonObject()) return Channel.EMPTY;
        JsonObject object = element.getAsJsonObject();
        if (object.has("vector")) return new Channel(List.of(new Keyframe(0.0F, vector(object), null)));
        List<Keyframe> frames = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            try {
                JsonElement value = entry.getValue();
                String easing = null;
                if (value.isJsonObject()) {
                    JsonObject frame = value.getAsJsonObject();
                    if (frame.has("easing")) easing = frame.get("easing").getAsString();
                    else if (frame.has("lerp_mode")) easing = frame.get("lerp_mode").getAsString();
                }
                if (value.isJsonObject() && value.getAsJsonObject().has("post")) value = value.getAsJsonObject().get("post");
                frames.add(new Keyframe(Float.parseFloat(entry.getKey()), vector(value), easing));
            } catch (RuntimeException ignored) {
                // Ignore malformed individual keyframes and keep the rest usable.
            }
        }
        frames.sort(Comparator.comparingDouble(Keyframe::time));
        return new Channel(frames);
    }

    private static Vec3 vector(JsonElement element) {
        JsonElement actual = element;
        if (actual.isJsonObject() && actual.getAsJsonObject().has("vector")) actual = actual.getAsJsonObject().get("vector");
        var array = actual.getAsJsonArray();
        return new Vec3(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    private static Vec3 sample(Channel channel, float time) {
        if (channel.frames.isEmpty()) return null;
        if (channel.frames.size() == 1 || time <= channel.frames.get(0).time) return channel.frames.get(0).value;
        for (int i = 1; i < channel.frames.size(); i++) {
            Keyframe right = channel.frames.get(i);
            if (time <= right.time) {
                Keyframe left = channel.frames.get(i - 1);
                float span = right.time - left.time;
                float amount = span <= 0.0F ? 1.0F : (time - left.time) / span;
                return left.value.lerp(right.value, applyEasing(right.easing, amount));
            }
        }
        return channel.frames.get(channel.frames.size() - 1).value;
    }

    private static float applyEasing(String easing, float amount) {
        return switch (easing == null ? "linear" : easing) {
            case "easeInSine" -> 1.0F - Mth.cos(amount * ((float) Math.PI / 2.0F));
            case "easeInQuad" -> amount * amount;
            case "easeOutQuad" -> 1.0F - (1.0F - amount) * (1.0F - amount);
            case "easeInQuart" -> amount * amount * amount * amount;
            case "easeOutCubic" -> {
                float inverse = 1.0F - amount;
                yield 1.0F - inverse * inverse * inverse;
            }
            // GeckoLibSerializer converts catmullrom to PAL's INOUTSINE easing.
            case "catmullrom", "easeInOutSine" -> -(Mth.cos((float) Math.PI * amount) - 1.0F) / 2.0F;
            case "EASEINOUTQUAD" -> amount < 0.5F ? 2.0F * amount * amount
                    : 1.0F - (float) Math.pow(-2.0F * amount + 2.0F, 2.0F) / 2.0F;
            default -> amount;
        };
    }

    private record AnimationDefinition(float length, boolean loop, boolean holdLastFrame,
                                       Map<String, BoneAnimation> bones) { }

    private record CacheKey(ResourceLocation resource, String animationId) { }

    private record BoneAnimation(Channel rotation, Channel position, Channel scale) {
        private float maxTime() {
            return Math.max(rotation.maxTime(), Math.max(position.maxTime(), scale.maxTime()));
        }
    }

    private record Channel(List<Keyframe> frames) {
        private static final Channel EMPTY = new Channel(List.of());

        private float maxTime() {
            return frames.isEmpty() ? 0.0F : frames.get(frames.size() - 1).time;
        }
    }

    private record Keyframe(float time, Vec3 value, String easing) { }

    public record BodyTransform(float x, float y, float z, float pitch, float yaw, float roll) {
        public static final BodyTransform IDENTITY = new BodyTransform(0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F);

        public boolean isIdentity() {
            return x == 0.0F && y == 0.0F && z == 0.0F
                    && pitch == 0.0F && yaw == 0.0F && roll == 0.0F;
        }

        public boolean isFinite() {
            return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)
                    && Float.isFinite(pitch) && Float.isFinite(yaw) && Float.isFinite(roll);
        }

        public static BodyTransform lerp(BodyTransform from, BodyTransform to, float amount) {
            return new BodyTransform(Mth.lerp(amount, from.x, to.x), Mth.lerp(amount, from.y, to.y),
                    Mth.lerp(amount, from.z, to.z), Mth.lerp(amount, from.pitch, to.pitch),
                    Mth.lerp(amount, from.yaw, to.yaw), Mth.lerp(amount, from.roll, to.roll));
        }
    }

    private enum Pivot {
        HEAD(0.0F, 0.0F, 0.0F),
        TORSO(0.0F, 0.0F, 0.0F),
        RIGHT_ARM(-5.0F, 2.0F, 0.0F),
        LEFT_ARM(5.0F, 2.0F, 0.0F),
        RIGHT_LEG(-1.9F, 12.0F, 0.1F),
        LEFT_LEG(1.9F, 12.0F, 0.1F);

        private final float x;
        private final float y;
        private final float z;

        Pivot(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private record Vec3(float x, float y, float z) {
        private static final Vec3 ZERO = new Vec3(0.0F, 0.0F, 0.0F);

        private Vec3 lerp(Vec3 other, float amount) {
            return new Vec3(x + (other.x - x) * amount, y + (other.y - y) * amount, z + (other.z - z) * amount);
        }
    }
}
