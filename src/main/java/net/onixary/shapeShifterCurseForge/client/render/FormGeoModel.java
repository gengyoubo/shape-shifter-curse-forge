package net.onixary.shapeShifterCurseForge.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.core.animation.AnimationState;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class FormGeoModel extends GeoModel<FormGeoAnimatable> {
    private static final float DEG_TO_RAD = (float) Math.PI / 180.0F;
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;
    private final ResourceLocation animationConfigResource;
    private ModelAnimationConfig animationConfig;
    private boolean animationConfigLoaded;
    private final Map<UUID, TailState> tailStates = new HashMap<>();
    private final Map<UUID, BlinkState> blinkStates = new HashMap<>();

    public FormGeoModel(ResourceLocation model, ResourceLocation texture, ResourceLocation animationConfigResource) {
        this.model = model;
        this.texture = texture;
        this.animation = ResourceLocation.fromNamespaceAndPath(model.getNamespace(), "animations/missing.animation.json");
        this.animationConfigResource = animationConfigResource;
    }

    @Override
    public ResourceLocation getModelResource(FormGeoAnimatable animatable) {
        return model;
    }

    @Override
    public ResourceLocation getModelResource(FormGeoAnimatable animatable, GeoRenderer<FormGeoAnimatable> renderer) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(FormGeoAnimatable animatable) {
        return texture;
    }

    @Override
    public ResourceLocation getTextureResource(FormGeoAnimatable animatable, GeoRenderer<FormGeoAnimatable> renderer) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(FormGeoAnimatable animatable) {
        return animation;
    }

    /**
     * The Fabric implementation copies the current PlayerModel pose to the form bones every frame.
     * GeckoLib provides the model renderer; the original Bedrock/Azure animation files are applied
     * by the Forge-side animation state machine after the vanilla player pose is copied.
     */
    @Override
    public void setCustomAnimations(FormGeoAnimatable animatable, long instanceId,
                                    AnimationState<FormGeoAnimatable> animationState) {
        Player player = animatable.getPlayer();
        if (player == null) {
            return;
        }

        resetTransform("bipedHead");
        resetTransform("bipedBody");
        resetTransform("bipedLeftArm");
        resetTransform("bipedRightArm");
        resetTransform("bipedLeftLeg");
        resetTransform("bipedRightLeg");
        ModelAnimationConfig config = animationConfig();
        // Tail/head-tail/wing bones are form-only.  They must begin from their baked pose on
        // every render, otherwise GeckoLib's shared model cache makes their previous frame leak
        // into the next player or render pass.
        config.resetDynamicBones(this);

        float partialTick = animationState.getPartialTick();
        boolean inventoryPreview = animatable.isInventoryPreview();
        // InventoryScreen temporarily writes the current rotations only.  Its previous-frame
        // rotations still belong to the world player, so interpolation tears the head away from
        // the body.  The preview must use the values set for this render call verbatim.
        float bodyYaw = inventoryPreview ? player.yBodyRot : Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float headYaw = inventoryPreview ? player.yHeadRot : Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float headPitch = inventoryPreview ? player.getXRot() : player.getViewXRot(partialTick);
        float age = player.tickCount + partialTick;
        float movement = inventoryPreview ? 0.0F : (float) Math.min(1.0D,
                Math.sqrt(player.getDeltaMovement().horizontalDistanceSqr()) * 8.0D);
        if (!player.onGround() || player.isFallFlying() || player.isSwimming()) {
            movement = 0.0F;
        }

        PlayerModel<?> vanillaModel = animatable.getVanillaPlayerModel();
        if (vanillaModel != null) {
            // Fabric copies the prepared PlayerModel pose, then inverts its Y/Z axes for
            // GeoBone space. This preserves swimming, crouching, attack and item poses.
            copyVanillaRotation("bipedHead", vanillaModel.head, true, true);
            copyVanillaRotation("bipedBody", vanillaModel.body, true, false);
            copyVanillaRotation("bipedRightArm", vanillaModel.rightArm, true, true);
            copyVanillaRotation("bipedLeftArm", vanillaModel.leftArm, true, true);
            copyVanillaRotation("bipedRightLeg", vanillaModel.rightLeg, true, true);
            copyVanillaRotation("bipedLeftLeg", vanillaModel.leftLeg, true, true);

            // ModelPart#getTransform().pivot is copied as a negated translation by the
            // Fabric renderer.  Forge 1.20.1 exposes the same pivot as x/y/z.  Keep the
            // vanilla biped offsets for arms and legs so the GeoBone origin matches the
            // PlayerModel origin before form animations are applied.
            copyVanillaPosition("bipedHead", vanillaModel.head, 0.0F, 0.0F, 0.0F);
            copyVanillaPosition("bipedBody", vanillaModel.body, 0.0F, 0.0F, 0.0F);
            copyVanillaPosition("bipedRightArm", vanillaModel.rightArm, -5.0F, 2.0F, 0.0F);
            copyVanillaPosition("bipedLeftArm", vanillaModel.leftArm, 5.0F, 2.0F, 0.0F);
            copyVanillaPosition("bipedRightLeg", vanillaModel.rightLeg, -2.0F, 12.0F, 0.0F);
            copyVanillaPosition("bipedLeftLeg", vanillaModel.leftLeg, 2.0F, 12.0F, 0.0F);
        } else {
            // Safe fallback for non-player preview callers that do not provide a renderer model.
            setRotation("bipedHead", headPitch * DEG_TO_RAD,
                    Mth.wrapDegrees(headYaw - bodyYaw) * DEG_TO_RAD, 0.0F);
            setRotation("bipedBody", !inventoryPreview && player.isCrouching() ? 0.50F : 0.0F, 0.0F, 0.0F);
            setRotation("bipedRightArm", -movement * 0.95F, 0.0F, 0.0F);
            setRotation("bipedLeftArm", movement * 0.95F, 0.0F, 0.0F);
        }

        applyDefaultModelAnimationSystem(config, player, partialTick, age,
                inventoryPreview ? 0.0F : player.walkAnimation.position(partialTick),
                inventoryPreview ? 0.0F : Math.min(player.walkAnimation.speed(partialTick), 1.0F));

        // Player Animator applies the selected SSC animation to PlayerModel during
        // setupAnim. The final PlayerModel pose was copied above. Form-only clips are
        // the exception: they are additive GeoBone layers and never replace that pose.
        float surfaceSprintTime = animatable.axolotlSurfaceSprintOverlayTime(partialTick);
        if (surfaceSprintTime >= 0.0F) {
            BedrockAnimationPlayer.applyAdditiveGeoRotation(this,
                    FormGeoAnimatable.AXOLOTL_SURFACE_SPRINT_ANIMATION,
                    FormGeoAnimatable.AXOLOTL_SURFACE_SPRINT_ID, surfaceSprintTime);
        }
    }

    private void setRotation(String boneName, float x, float y, float z) {
        getBone(boneName).ifPresent(bone -> {
            bone.setRotX(x);
            bone.setRotY(y);
            bone.setRotZ(z);
        });
    }

    private void copyVanillaRotation(String boneName, ModelPart part, boolean invertGeoY, boolean invertGeoZ) {
        float y = invertGeoY ? -part.yRot : part.yRot;
        float z = invertGeoZ ? -part.zRot : part.zRot;
        setRotation(boneName, part.xRot, y, z);
    }

    private void copyVanillaPosition(String boneName, ModelPart part, float offsetX, float offsetY, float offsetZ) {
        setPosition(boneName, -part.x + offsetX, -part.y + offsetY, -part.z + offsetZ);
    }

    private void setPosition(String boneName, float x, float y, float z) {
        getBone(boneName).ifPresent(bone -> {
            bone.setPosX(x);
            bone.setPosY(y);
            bone.setPosZ(z);
        });
    }

    private void resetTransform(String boneName) {
        getBone(boneName).ifPresent(bone -> {
            bone.setPosX(0.0F);
            bone.setPosY(0.0F);
            bone.setPosZ(0.0F);
            bone.setRotX(0.0F);
            bone.setRotY(0.0F);
            bone.setRotZ(0.0F);
            bone.setScaleX(1.0F);
            bone.setScaleY(1.0F);
            bone.setScaleZ(1.0F);
            bone.setHidden(false);
        });
    }

    private void resetRotation(String boneName) {
        getBone(boneName).ifPresent(bone -> {
            var initial = bone.getInitialSnapshot();
            bone.setRotX(initial.getRotX());
            bone.setRotY(initial.getRotY());
            bone.setRotZ(initial.getRotZ());
        });
    }

    /**
     * The original form renderer has a second, model-driven animation system in addition to
     * Player Animation Lib.  The form metadata already ships in this Forge project under
     * {@code ssc_form_model}; load it lazily because model instances are created on the render
     * thread only after the resource manager is available.
     */
    private ModelAnimationConfig animationConfig() {
        if (animationConfigLoaded) {
            return animationConfig;
        }
        animationConfigLoaded = true;
        animationConfig = ModelAnimationConfig.EMPTY;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return animationConfig;
        }
        try {
            var resource = minecraft.getResourceManager().getResource(animationConfigResource);
            if (resource.isEmpty()) {
                return animationConfig;
            }
            try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                animationConfig = ModelAnimationConfig.parse(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (RuntimeException | java.io.IOException ignored) {
            // A malformed cosmetic model must never prevent the owning player from rendering.
            animationConfig = ModelAnimationConfig.EMPTY;
        }
        return animationConfig;
    }

    /** Direct Forge counterpart of Fabric DefaultModelAnimationSystem's tail/head-tail/wing pass. */
    private void applyDefaultModelAnimationSystem(ModelAnimationConfig config, Player player, float partialTick,
                                                   float age, float limbAngle, float limbDistance) {
        if (config.isEmpty()) {
            return;
        }
        TailState tail = tailStates.computeIfAbsent(player.getUUID(), ignored -> new TailState());
        tail.advance(player);
        float horizontalDrag = tail.currentHorizontal(partialTick);
        float verticalDrag = tail.currentVertical(partialTick);
        boolean feral = FormManager.current(player).bodyType() == FormBodyType.FERAL;

        for (List<String> chain : config.tailChains) {
            applyTailChain(chain, feral, age, limbAngle, limbDistance, horizontalDrag, verticalDrag);
        }
        // Fabric's head-tail controller uses the smoothed neck yaw when one exists.  Axolotl
        // does not have a long-neck configuration, so the prepared head yaw is its exact input.
        float headAngle = Mth.wrapDegrees(player.getYHeadRot() - player.yBodyRot) * DEG_TO_RAD;
        for (List<String> chain : config.headTailChains) {
            applyHeadTailChain(chain, headAngle, age, horizontalDrag, verticalDrag);
        }
        for (List<String> chain : config.leftWingChains) {
            applyWingChain(chain, true, age, limbAngle, limbDistance, verticalDrag);
        }
        for (List<String> chain : config.rightWingChains) {
            applyWingChain(chain, false, age, limbAngle, limbDistance, verticalDrag);
        }
        applyBlink(config, player, partialTick);
    }

    private void applyTailChain(List<String> chain, boolean feral, float age, float limbAngle, float limbDistance,
                                float horizontalDrag, float verticalDrag) {
        if (chain.isEmpty()) {
            return;
        }
        float swayRate = 0.33333334F * 0.5F;
        float swayScale = 0.05F;
        float sway = swayScale * Mth.cos(age * swayRate + ((float) Math.PI / 3.0F * 0.75F));
        float balance = Mth.cos(limbAngle * 0.6662F) * 0.325F * limbDistance;
        setChainRotation(chain.get(0), feral, Mth.lerp(limbDistance, sway, balance), horizontalDrag,
                -verticalDrag * 0.75F);
        float offset = 0.0F;
        for (int index = 1; index < chain.size(); index++) {
            float childSway = swayScale * Mth.cos(age * swayRate - ((float) Math.PI / 3.0F * offset));
            setChainRotation(chain.get(index), feral, Mth.lerp(limbDistance, childSway, 0.0F), horizontalDrag,
                    -verticalDrag * 0.75F * (offset + 0.75F));
            offset += 0.75F;
        }
    }

    private void applyHeadTailChain(List<String> chain, float headAngle, float age,
                                    float horizontalDrag, float verticalDrag) {
        if (chain.isEmpty()) {
            return;
        }
        float swayRate = 0.33333334F * 0.5F;
        float swayScale = 0.05F;
        float sway = swayScale * Mth.cos(age * swayRate + ((float) Math.PI / 3.0F * 0.75F));
        float balance = Mth.cos(headAngle * 0.6662F) * 0.325F * 0.1F;
        setChainRotation(chain.get(0), false, Mth.lerp(0.1F, sway, balance), horizontalDrag,
                -verticalDrag * 0.75F);
        float offset = 0.0F;
        for (int index = 1; index < chain.size(); index++) {
            float childSway = swayScale * Mth.cos(age * swayRate - ((float) Math.PI / 3.0F * offset));
            setChainRotation(chain.get(index), false, Mth.lerp(0.1F, childSway, 0.0F), horizontalDrag,
                    -verticalDrag * 0.75F * (offset + 0.75F));
            offset += 0.75F;
        }
    }

    private void setChainRotation(String boneName, boolean feral, float sway, float horizontalDrag, float pitch) {
        getBone(boneName).ifPresent(bone -> {
            if (feral) {
                bone.setRotZ(sway + horizontalDrag * 0.75F);
            } else {
                bone.setRotY(-sway - horizontalDrag * 0.75F);
            }
            bone.setRotX(pitch);
        });
    }

    private void applyWingChain(List<String> chain, boolean left, float age, float limbAngle,
                                float limbDistance, float verticalDrag) {
        if (chain.isEmpty()) {
            return;
        }
        float swayBase = Mth.cos(age * 20.0F * DEG_TO_RAD + limbAngle) * (float) Math.PI * 0.15F + limbDistance;
        getBone(chain.get(0)).ifPresent(bone -> {
            bone.setRotY((left ? -(float) Math.PI / 4.0F : (float) Math.PI / 4.0F)
                    + (left ? swayBase : -swayBase));
            bone.setRotX(-verticalDrag * 0.35F);
        });
        float offset = 0.0F;
        for (int index = 1; index < chain.size(); index++) {
            float childOffset = offset;
            getBone(chain.get(index)).ifPresent(bone -> bone.setRotX(-verticalDrag * 0.75F * childOffset));
            offset += 0.75F;
        }
    }

    private void applyBlink(ModelAnimationConfig config, Player player, float partialTick) {
        if (config.eyeBone == null) {
            return;
        }
        BlinkState blink = blinkStates.computeIfAbsent(player.getUUID(), ignored -> new BlinkState());
        float scale = blink.scale(player, partialTick, config.openEyeScale, config.closedEyeScale);
        getBone(config.eyeBone).ifPresent(bone -> bone.setScaleY(scale));
    }

    private static final class TailState {
        private int lastTick = Integer.MIN_VALUE;
        private float horizontal;
        private float horizontalOld;
        private float vertical;
        private float verticalOld;
        private float smoothHorizontal;
        private float smoothVertical;

        private void advance(Player player) {
            if (lastTick == player.tickCount) {
                return;
            }
            horizontalOld = horizontal;
            horizontal *= 0.75F;
            horizontal -= Mth.wrapDegrees(player.yBodyRot - player.yBodyRotO) * DEG_TO_RAD * 0.55F;
            horizontal = Mth.clamp(horizontal, -1.6F, 1.6F);
            verticalOld = vertical;
            float targetVerticalDrag = Mth.clamp((float) player.getDeltaMovement().y * 1.5F, -1.6F, 1.6F);
            vertical = Mth.clamp(vertical * 0.8F + targetVerticalDrag * 0.15F, -1.6F, 1.6F);
            lastTick = player.tickCount;
        }

        private float currentHorizontal(float partialTick) {
            smoothHorizontal = Mth.lerp(0.04F, smoothHorizontal, Mth.lerp(partialTick, horizontalOld, horizontal));
            return smoothHorizontal;
        }

        private float currentVertical(float partialTick) {
            smoothVertical = Mth.lerp(0.04F, smoothVertical, Mth.lerp(partialTick, verticalOld, vertical));
            return smoothVertical;
        }
    }

    private static final class BlinkState {
        private final Random random = new Random();
        private int lastTick = Integer.MIN_VALUE;
        private int waitTicks = -1;
        private int blinkTicks;
        private boolean blinking;
        private boolean wasSleeping;

        private float scale(Player player, float partialTick, float openScale, float closedScale) {
            if (player.isSleeping()) {
                lastTick = player.tickCount;
                wasSleeping = true;
                blinking = false;
                waitTicks = -1;
                blinkTicks = 0;
                return closedScale;
            }
            if (lastTick == Integer.MIN_VALUE || wasSleeping) {
                schedule();
                lastTick = player.tickCount;
                wasSleeping = false;
            }
            int elapsed = Mth.clamp(player.tickCount - lastTick, 0, 100);
            for (int i = 0; i < elapsed; i++) {
                advance();
            }
            lastTick = player.tickCount;
            if (!blinking) {
                return openScale;
            }
            float progress = Mth.clamp((blinkTicks + partialTick) / 4.0F, 0.0F, 1.0F);
            float closeAmount = progress <= 0.5F ? progress * 2.0F : (1.0F - progress) * 2.0F;
            return Mth.lerp(closeAmount, openScale, closedScale);
        }

        private void advance() {
            if (blinking) {
                if (++blinkTicks >= 4) {
                    schedule();
                }
                return;
            }
            if (--waitTicks <= 0) {
                blinking = true;
                blinkTicks = 0;
            }
        }

        private void schedule() {
            waitTicks = 60 + random.nextInt(81);
            blinking = false;
            blinkTicks = 0;
        }
    }

    private record ModelAnimationConfig(List<List<String>> tailChains, List<List<String>> headTailChains,
                                        List<List<String>> leftWingChains, List<List<String>> rightWingChains,
                                        String eyeBone, float openEyeScale, float closedEyeScale) {
        private static final ModelAnimationConfig EMPTY = new ModelAnimationConfig(List.of(), List.of(), List.of(),
                List.of(), null, 1.0F, 0.01F);

        private static ModelAnimationConfig parse(JsonObject root) {
            if (!root.has("animation_system_config") || !root.get("animation_system_config").isJsonObject()) {
                return EMPTY;
            }
            JsonObject config = root.getAsJsonObject("animation_system_config");
            JsonObject blink = object(config, "eye_blink");
            return new ModelAnimationConfig(chains(object(config, "tail")), chains(object(config, "head_tail")),
                    chains(object(config, "wing_l")), chains(object(config, "wing_r")),
                    blink == null || !blink.has("eye") ? null : blink.get("eye").getAsString(),
                    number(blink, "open_scale", 1.0F), number(blink, "close_scale", 0.01F));
        }

        private boolean isEmpty() {
            return tailChains.isEmpty() && headTailChains.isEmpty() && leftWingChains.isEmpty()
                    && rightWingChains.isEmpty() && eyeBone == null;
        }

        private void resetDynamicBones(FormGeoModel model) {
            for (List<String> chain : tailChains) resetChain(model, chain);
            for (List<String> chain : headTailChains) resetChain(model, chain);
            for (List<String> chain : leftWingChains) resetChain(model, chain);
            for (List<String> chain : rightWingChains) resetChain(model, chain);
            if (eyeBone != null) {
                model.getBone(eyeBone).ifPresent(bone -> bone.setScaleY(bone.getInitialSnapshot().getScaleY()));
            }
        }

        private static void resetChain(FormGeoModel model, List<String> chain) {
            for (String bone : chain) model.resetRotation(bone);
        }

        private static List<List<String>> chains(JsonObject definition) {
            if (definition == null || !definition.has("chain") || !definition.get("chain").isJsonObject()) {
                return List.of();
            }
            List<List<String>> chains = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : definition.getAsJsonObject("chain").entrySet()) {
                if (!entry.getValue().isJsonArray()) continue;
                JsonArray indexes = entry.getValue().getAsJsonArray();
                List<String> chain = new ArrayList<>(indexes.size());
                for (JsonElement index : indexes) chain.add(entry.getKey() + "_" + index.getAsString());
                if (!chain.isEmpty()) chains.add(List.copyOf(chain));
            }
            return List.copyOf(chains);
        }

        private static JsonObject object(JsonObject parent, String key) {
            return parent != null && parent.has(key) && parent.get(key).isJsonObject()
                    ? parent.getAsJsonObject(key) : null;
        }

        private static float number(JsonObject object, String key, float fallback) {
            return object != null && object.has(key) ? object.get(key).getAsFloat() : fallback;
        }
    }
}
