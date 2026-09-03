package net.onixary.shapeShifterCurseForge.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import software.bernie.geckolib.cache.object.GeoBone;
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
     * BASE_POSE replaces a GeoBone channel with the SSC keyframe value. ADDITIVE applies
     * the keyframe on top of the pose already copied from Minecraft's PlayerModel.
     */
    public enum PoseMode {
        BASE_POSE,
        ADDITIVE
    }

    public static void apply(GeoModel<?> model, FormAnimationSystem.Selection selection, float timeSeconds) {
        apply(model, selection, timeSeconds, selection.poseMode());
    }

    public static void apply(GeoModel<?> model, FormAnimationSystem.Selection selection, float timeSeconds,
                             PoseMode poseMode) {
        // Selection.id is the logical/profile key; the file may expose a different
        // Bedrock animation key (for example bat_3_sprint -> bat_3_walk).
        AnimationDefinition definition = load(selection.resource(), selection.animationId());
        if (definition == null) return;

        float time = definition.length <= 0.0F ? 0.0F : timeSeconds;
        if (definition.loop) {
            time %= definition.length;
            if (time < 0.0F) time += definition.length;
        } else {
            time = Math.min(time, definition.length);
        }

        for (Map.Entry<String, BoneAnimation> entry : definition.bones.entrySet()) {
            Optional<GeoBone> optionalBone = model.getBone(resolveBoneName(entry.getKey()));
            if (optionalBone.isEmpty()) continue;
            GeoBone bone = optionalBone.get();
            BoneAnimation animation = entry.getValue();
            Vec3 rotation = sample(animation.rotation, time);
            Vec3 position = sample(animation.position, time);
            Vec3 scale = sample(animation.scale, time);
            if (rotation != null) {
                float x = rotation.x * DEG_TO_RAD;
                float y = rotation.y * DEG_TO_RAD;
                float z = rotation.z * DEG_TO_RAD;
                if (poseMode == PoseMode.ADDITIVE) {
                    x += bone.getRotX();
                    y += bone.getRotY();
                    z += bone.getRotZ();
                }
                bone.setRotX(x);
                bone.setRotY(y);
                bone.setRotZ(z);
            }
            if (position != null) {
                float x = position.x;
                float y = position.y;
                float z = position.z;
                if (poseMode == PoseMode.ADDITIVE) {
                    x += bone.getPosX();
                    y += bone.getPosY();
                    z += bone.getPosZ();
                }
                bone.setPosX(x);
                bone.setPosY(y);
                bone.setPosZ(z);
            }
            if (scale != null) {
                if (poseMode == PoseMode.ADDITIVE) {
                    // [1, 1, 1] is the neutral scale in Bedrock files, so multiply it
                    // with the already-copied base scale instead of replacing it.
                    bone.setScaleX(bone.getScaleX() * scale.x);
                    bone.setScaleY(bone.getScaleY() * scale.y);
                    bone.setScaleZ(bone.getScaleZ() * scale.z);
                } else {
                    bone.setScaleX(scale.x);
                    bone.setScaleY(scale.y);
                    bone.setScaleZ(scale.z);
                }
            }
        }
    }

    public static void clearCache() {
        CACHE.clear();
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
        return new AnimationDefinition(length, animation.has("loop") && animation.get("loop").getAsBoolean(), bones);
    }

    private static Channel channel(JsonObject bone, String name) {
        if (!bone.has(name)) return Channel.EMPTY;
        JsonElement element = bone.get(name);
        if (element.isJsonArray()) return new Channel(List.of(new Keyframe(0.0F, vector(element))));
        if (!element.isJsonObject()) return Channel.EMPTY;
        JsonObject object = element.getAsJsonObject();
        if (object.has("vector")) return new Channel(List.of(new Keyframe(0.0F, vector(object))));
        List<Keyframe> frames = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            try {
                JsonElement value = entry.getValue();
                if (value.isJsonObject() && value.getAsJsonObject().has("post")) value = value.getAsJsonObject().get("post");
                frames.add(new Keyframe(Float.parseFloat(entry.getKey()), vector(value)));
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
                return left.value.lerp(right.value, amount);
            }
        }
        return channel.frames.get(channel.frames.size() - 1).value;
    }

    private static String resolveBoneName(String name) {
        return switch (name) {
            case "head" -> "bipedHead";
            case "body", "torso" -> "bipedBody";
            case "leftArm" -> "bipedLeftArm";
            case "rightArm" -> "bipedRightArm";
            case "leftLeg" -> "bipedLeftLeg";
            case "rightLeg" -> "bipedRightLeg";
            default -> name;
        };
    }

    private record AnimationDefinition(float length, boolean loop, Map<String, BoneAnimation> bones) { }

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

    private record Keyframe(float time, Vec3 value) { }

    private record Vec3(float x, float y, float z) {
        private Vec3 lerp(Vec3 other, float amount) {
            return new Vec3(x + (other.x - x) * amount, y + (other.y - y) * amount, z + (other.z - z) * amount);
        }
    }
}
