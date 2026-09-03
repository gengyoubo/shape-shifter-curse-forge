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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
    private final Map<UUID, NeckState> neckStates = new HashMap<>();
    private final Map<UUID, NeckState> inventoryNeckStates = new HashMap<>();

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

        applyDefaultModelAnimationSystem(config, animatable, player, partialTick, age,
                inventoryPreview ? 0.0F : player.walkAnimation.position(partialTick),
                inventoryPreview ? 0.0F : Math.min(player.walkAnimation.speed(partialTick), 1.0F),
                inventoryPreview, headYaw, headPitch);

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
    private void applyDefaultModelAnimationSystem(ModelAnimationConfig config, FormGeoAnimatable animatable,
                                                   Player player, float partialTick, float age,
                                                   float limbAngle, float limbDistance,
                                                   boolean inventoryPreview, float headYawDeg, float headPitchDeg) {
        if (config.isEmpty()) {
            return;
        }
        applyExtraBones(config, animatable);
        TailState tail = tailStates.computeIfAbsent(player.getUUID(), ignored -> new TailState());
        tail.advance(player);
        float horizontalDrag = tail.currentHorizontal(partialTick);
        float verticalDrag = tail.currentVertical(partialTick);
        boolean feral = FormManager.current(player).bodyType() == FormBodyType.FERAL;

        for (List<String> chain : config.tailChains) {
            applyTailChain(chain, feral, age, limbAngle, limbDistance, horizontalDrag, verticalDrag);
        }
        // Fabric's head-tail controller uses the smoothed neck yaw when a neck exists.
        // Axolotl does not have a long-neck configuration, so the prepared head yaw is its exact input.
        float headAngle = Mth.wrapDegrees(player.getYHeadRot() - player.yBodyRot) * DEG_TO_RAD;
        float[] neckAngles = null;
        if (config.neck != null) {
            neckAngles = smoothedNeckAngles(config.neck, player, partialTick, inventoryPreview,
                    headYawDeg, headPitchDeg);
            applyNeckBones(config.neck, neckAngles[0], neckAngles[1]);
            headAngle = neckAngles[0] * DEG_TO_RAD;
        }
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

    /**
     * Forge counterpart of Fabric's {@code ProcessExtraBone}. The selected clip can animate
     * form-only bones (calves, tail/neck mounts, hind legs, ...); PAL exposes them through
     * {@code get3DTransform} and the config maps each animated bone onto a GeoBone.
     * Rotation arrives in degrees and is converted with the same Y/Z inversion used for
     * the vanilla copy; position is stored with all axes negated, matching PAL's Gecko
     * serializer conventions documented in {@code ProcessExtraBone}.
     */
    private void applyExtraBones(ModelAnimationConfig config, FormGeoAnimatable animatable) {
        for (ExtraBoneMapping mapping : config.extraBones) {
            BedrockAnimationPlayer.BoneSample sample = animatable.sampleExtraBone(mapping.animBone());
            getBone(mapping.geoBone()).ifPresent(bone -> {
                bone.setPosX(0.0F);
                bone.setPosY(0.0F);
                bone.setPosZ(0.0F);
                bone.setRotX(0.0F);
                bone.setRotY(0.0F);
                bone.setRotZ(0.0F);
                if (sample == null) {
                    return;
                }
                bone.setPosX(-sample.posX());
                bone.setPosY(-sample.posY());
                bone.setPosZ(-sample.posZ());
                bone.setRotX(sample.rotX() * DEG_TO_RAD);
                bone.setRotY(-sample.rotY() * DEG_TO_RAD);
                bone.setRotZ(-sample.rotZ() * DEG_TO_RAD);
            });
        }
    }

    /**
     * Smoothed long-neck look angles in degrees, mirroring Fabric's
     * {@code getLongNeckAngles}. The smoothing state is per player so neck motion lags
     * the head naturally; inventory previews use an isolated state like Fabric's
     * virtual-data map.
     */
    private float[] smoothedNeckAngles(NeckConfig neck, Player player, float partialTick,
                                       boolean inventoryPreview, float fallbackYawDeg, float fallbackPitchDeg) {
        Map<UUID, NeckState> states = inventoryPreview ? inventoryNeckStates : neckStates;
        NeckState state = states.computeIfAbsent(player.getUUID(),
                ignored -> new NeckState(fallbackYawDeg, fallbackPitchDeg));
        float viewYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float targetPitch = inventoryPreview ? player.getXRot() : player.getViewXRot(partialTick);
        float bodyYaw = lerpAngle(partialTick, player.yBodyRotO, player.yBodyRot);
        float targetYaw = Mth.wrapDegrees(viewYaw - bodyYaw);
        double renderTick = player.tickCount + partialTick;
        float deltaTicks = Mth.clamp((float) (renderTick - state.lastRenderTick), 0.0F, 1.0F);
        state.lastRenderTick = renderTick;
        if (deltaTicks > 0.0F) {
            float yawLerp = Mth.clamp(deltaTicks * 0.45F, 0.0F, 1.0F);
            float pitchLerp = Mth.clamp(deltaTicks * 0.35F, 0.0F, 1.0F);
            // Fabric routes yaw through lerpAngleAwayFrom(..., 180) to avoid twisting
            // past backwards, but combined with angle wrapping that detour fires exactly
            // when facing backwards (backpedal): crossing +/-180 sends the neck on a
            // near-full-rotation spin. The chain weights clamp to maxYawDeg downstream
            // anyway, so take the plain shortest path like pitch does.
            state.headYaw = lerpAngle(yawLerp, state.headYaw, targetYaw);
            state.headPitch = Mth.lerp(pitchLerp, state.headPitch, targetPitch);
            if (!Float.isFinite(state.headYaw)) {
                state.headYaw = fallbackYawDeg;
            }
            if (!Float.isFinite(state.headPitch)) {
                state.headPitch = fallbackPitchDeg;
            }
        }
        return new float[]{state.headYaw, state.headPitch};
    }

    /** Distributes clamped yaw/pitch across the neck chain and head, mirroring Fabric. */
    private void applyNeckBones(NeckConfig neck, float headYawDeg, float headPitchDeg) {
        float yawDeg = Mth.clamp(headYawDeg, -neck.maxYawDeg, neck.maxYawDeg);
        float pitchDeg = Mth.clamp(headPitchDeg, -neck.maxPitchDegU, neck.maxPitchDegD);
        float yawRad = yawDeg * DEG_TO_RAD;
        float pitchRad = pitchDeg * DEG_TO_RAD;
        for (int index = 0; index <= neck.chain.size(); index++) {
            String boneName = index < neck.chain.size() ? neck.chain.get(index) : neck.headBone;
            float yaw = index < neck.yawWeights.length ? yawRad * neck.yawWeights[index] : 0.0F;
            float pitch = index < neck.pitchWeights.length ? pitchRad * neck.pitchWeights[index] : 0.0F;
            getBone(boneName).ifPresent(bone -> {
                bone.setRotX(0.0F);
                bone.setRotY(0.0F);
                bone.setRotZ(0.0F);
                setAxisRotation(bone, neck.yawAxis, yaw);
                setAxisRotation(bone, neck.pitchAxis, pitch);
            });
        }
    }

    private static void setAxisRotation(software.bernie.geckolib.cache.object.GeoBone bone, int axis, float value) {
        switch (axis) {
            case 0 -> bone.setRotX(value);
            case 1 -> bone.setRotX(-value);
            case 2 -> bone.setRotY(value);
            case 3 -> bone.setRotY(-value);
            case 4 -> bone.setRotZ(value);
            case 5 -> bone.setRotZ(-value);
            default -> { }
        }
    }

    private static float lerpAngle(float delta, float start, float end) {
        return start + Mth.wrapDegrees(end - start) * delta;
    }

    private static final class NeckState {
        private float headYaw;
        private float headPitch;
        private double lastRenderTick = -1.0D;

        private NeckState(float headYaw, float headPitch) {
            this.headYaw = headYaw;
            this.headPitch = headPitch;
        }
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

    /** Exposes the parsed form config to the first-person arm renderer. */
    public ModelAnimationConfig renderConfig() {
        return animationConfig();
    }

    /** Geo resource id, used to bake/resolve bones for the first-person arm pass. */
    public ResourceLocation modelResource() {
        return model;
    }

    /** Whether the form hides a vanilla player part (hat/head/body/jacket/arms/...). */
    public boolean isVanillaPartHidden(String partName) {
        return renderConfig().hiddenParts.contains(partName);
    }

    /**
     * GeoBone rendered as the first-person arm. Fabric defaults to the biped arm bones
     * and only overrides them through {@code first_person_render}.
     */
    public String firstPersonArmBone(boolean right) {
        ModelAnimationConfig config = renderConfig();
        String override = right ? config.firstPersonRightArm : config.firstPersonLeftArm;
        if (override != null) {
            return override;
        }
        return right ? "bipedRightArm" : "bipedLeftArm";
    }

    private record ExtraBoneMapping(String animBone, String geoBone) { }

    /**
     * Long-neck look distribution, mirroring Fabric's {@code NeckConfig}.
     * Axis ids: -1 none, 0 +x, 1 -x, 2 +y, 3 -y, 4 +z, 5 -z.
     */
    private record NeckConfig(List<String> chain, String headBone, int yawAxis, int pitchAxis,
                              float[] yawWeights, float[] pitchWeights,
                              float maxYawDeg, float maxPitchDegU, float maxPitchDegD) {
        private static NeckConfig of(JsonObject json) {
            List<String> chain = parseChain(json);
            String head = parseHead(json);
            int size = chain.size() + 1;
            return new NeckConfig(chain, head, parseAxis(json, "yaw_axis"), parseAxis(json, "pitch_axis"),
                    parseWeights(json, "yaw_weights", "yaw_total", size),
                    parseWeights(json, "pitch_weights", "pitch_total", size),
                    ModelAnimationConfig.number(json, "max_yaw_deg", 180.0F),
                    ModelAnimationConfig.number(json, "max_pitch_up_deg", 180.0F),
                    ModelAnimationConfig.number(json, "max_pitch_down_deg", 180.0F));
        }

        private static List<String> parseChain(JsonObject json) {
            if (json == null || !json.has("chain") || !json.get("chain").isJsonObject()) {
                throw new IllegalStateException("neck_config chain is missing");
            }
            List<String> chain = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("chain").entrySet()) {
                if (!entry.getValue().isJsonArray()) {
                    continue;
                }
                for (JsonElement index : entry.getValue().getAsJsonArray()) {
                    chain.add(entry.getKey() + "_" + index.getAsString());
                }
            }
            if (chain.isEmpty()) {
                throw new IllegalStateException("neck_config chain is empty");
            }
            return List.copyOf(chain);
        }

        private static String parseHead(JsonObject json) {
            if (json == null || !json.has("head")) {
                throw new IllegalStateException("neck_config head is missing");
            }
            return json.get("head").getAsString();
        }

        private static int parseAxis(JsonObject json, String key) {
            if (json == null || !json.has(key)) {
                return -1;
            }
            return switch (json.get(key).getAsString()) {
                case "x" -> 0;
                case "-x" -> 1;
                case "y" -> 2;
                case "-y" -> 3;
                case "z" -> 4;
                case "-z" -> 5;
                default -> -1;
            };
        }

        private static float[] parseWeights(JsonObject json, String key, String totalKey, int size) {
            Float total = json != null && json.has(totalKey) ? json.get(totalKey).getAsFloat() : null;
            float[] weights = new float[size];
            JsonArray array = json != null && json.has(key) && json.get(key).isJsonArray()
                    ? json.getAsJsonArray(key) : null;
            if (array == null) {
                Arrays.fill(weights, (total == null ? 1.0F : total) / size);
                return weights;
            }
            float realTotal = 0.0F;
            for (int index = 0; index < size; index++) {
                float weight = index < array.size() ? array.get(index).getAsFloat() : 0.0F;
                weights[index] = weight;
                realTotal += weight;
            }
            if (total != null && realTotal > 0.0001F) {
                float scale = total / realTotal;
                for (int index = 0; index < size; index++) {
                    weights[index] *= scale;
                }
            }
            return weights;
        }
    }

    private record ModelAnimationConfig(List<List<String>> tailChains, List<List<String>> headTailChains,
                                        List<List<String>> leftWingChains, List<List<String>> rightWingChains,
                                        String eyeBone, float openEyeScale, float closedEyeScale,
                                        Set<String> hiddenParts, List<ExtraBoneMapping> extraBones,
                                        String firstPersonLeftArm, String firstPersonRightArm,
                                        NeckConfig neck) {
        private static final ModelAnimationConfig EMPTY = new ModelAnimationConfig(List.of(), List.of(), List.of(),
                List.of(), null, 1.0F, 0.01F, Set.of(), List.of(), null, null, null);

        private static ModelAnimationConfig parse(JsonObject root) {
            Set<String> hidden = new HashSet<>();
            if (root.has("hidden") && root.get("hidden").isJsonArray()) {
                for (JsonElement entry : root.getAsJsonArray("hidden")) {
                    if (entry.isJsonPrimitive()) {
                        hidden.add(entry.getAsString());
                    }
                }
            }
            if (!root.has("animation_system_config") || !root.get("animation_system_config").isJsonObject()) {
                return new ModelAnimationConfig(List.of(), List.of(), List.of(), List.of(), null, 1.0F, 0.01F,
                        Set.copyOf(hidden), List.of(), null, null, null);
            }
            JsonObject config = root.getAsJsonObject("animation_system_config");
            JsonObject blink = object(config, "eye_blink");
            List<ExtraBoneMapping> extraBones = new ArrayList<>();
            JsonObject extraParts = object(config, "extra_parts_map");
            if (extraParts != null) {
                for (Map.Entry<String, JsonElement> entry : extraParts.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        extraBones.add(new ExtraBoneMapping(entry.getKey(), entry.getValue().getAsString()));
                    }
                }
            }
            String firstPersonLeft = null;
            String firstPersonRight = null;
            JsonObject firstPerson = object(config, "first_person_render");
            if (firstPerson != null) {
                if (firstPerson.has("left_arm")) {
                    firstPersonLeft = firstPerson.get("left_arm").getAsString();
                }
                if (firstPerson.has("right_arm")) {
                    firstPersonRight = firstPerson.get("right_arm").getAsString();
                }
            }
            NeckConfig neck = null;
            if (config.has("neck_config") && config.get("neck_config").isJsonObject()) {
                try {
                    neck = NeckConfig.of(config.getAsJsonObject("neck_config"));
                } catch (RuntimeException ignored) {
                    neck = null;
                }
            }
            return new ModelAnimationConfig(chains(object(config, "tail")), chains(object(config, "head_tail")),
                    chains(object(config, "wing_l")), chains(object(config, "wing_r")),
                    blink == null || !blink.has("eye") ? null : blink.get("eye").getAsString(),
                    number(blink, "open_scale", 1.0F), number(blink, "close_scale", 0.01F),
                    Set.copyOf(hidden), List.copyOf(extraBones), firstPersonLeft, firstPersonRight, neck);
        }

        private boolean isEmpty() {
            return tailChains.isEmpty() && headTailChains.isEmpty() && leftWingChains.isEmpty()
                    && rightWingChains.isEmpty() && eyeBone == null && extraBones.isEmpty() && neck == null;
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
