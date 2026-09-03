package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.config.SscClientConfig;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.client.PowerAnimationClientHandler;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FormGeoAnimatable implements GeoAnimatable {
    public static final ResourceLocation AXOLOTL_SURFACE_SPRINT_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE,
                    "player_animation/new/form_axolotl_3_new.animation.json");
    public static final String AXOLOTL_SURFACE_SPRINT_ID = "The Surface Sprint Begins";

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private Player player;
    private PlayerModel<?> vanillaPlayerModel;
    private BedrockAnimationPlayer.BodyTransform bodyTransform = BedrockAnimationPlayer.BodyTransform.IDENTITY;
    private boolean inventoryPreview;
    private final Map<UUID, AnimationTimeline> timelines = new HashMap<>();
    private final Map<UUID, OverlayTimeline> overlayTimelines = new HashMap<>();
    private FormAnimationSystem.Selection extraPrimary;
    private float extraPrimaryTime;
    private boolean extraPrimaryForceLoop;
    private FormAnimationSystem.Selection extraSecondary;
    private float extraSecondaryTime;
    private float extraBlend = 1.0F;

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void setVanillaPlayerModel(PlayerModel<?> vanillaPlayerModel) {
        this.vanillaPlayerModel = vanillaPlayerModel;
    }

    public PlayerModel<?> getVanillaPlayerModel() {
        return vanillaPlayerModel;
    }

    public BedrockAnimationPlayer.BodyTransform getBodyTransform() {
        return bodyTransform;
    }

    /**
     * Forge fires RenderPlayerEvent.Pre before LivingEntityRenderer calls setupAnim.
     * Fabric's form feature runs after that preparation, so recreate the vanilla pose here.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void prepareVanillaPlayerPose(float partialTick) {
        // A renderer instance is reused for the same form. Never let a body transform
        // from a previous render survive a missing/partial player-model render pass.
        bodyTransform = BedrockAnimationPlayer.BodyTransform.IDENTITY;
        if (player == null || vanillaPlayerModel == null) {
            return;
        }
        boolean shouldSit = player.isPassenger() && player.getVehicle() != null && player.getVehicle().shouldRiderSit();
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float netHeadYaw = headYaw - bodyYaw;
        if (shouldSit && player.getVehicle() instanceof net.minecraft.world.entity.LivingEntity vehicle) {
            bodyYaw = Mth.rotLerp(partialTick, vehicle.yBodyRotO, vehicle.yBodyRot);
            netHeadYaw = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -85.0F, 85.0F);
            bodyYaw = headYaw - netHeadYaw;
            if (netHeadYaw * netHeadYaw > 2500.0F) {
                bodyYaw += netHeadYaw * 0.2F;
            }
            netHeadYaw = headYaw - bodyYaw;
        }

        float limbSwingAmount = 0.0F;
        float limbSwing = 0.0F;
        if (!shouldSit && player.isAlive()) {
            limbSwingAmount = Math.min(player.walkAnimation.speed(partialTick), 1.0F);
            limbSwing = player.walkAnimation.position(partialTick);
            if (player.isBaby()) {
                limbSwing *= 3.0F;
            }
        }
        float headPitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
        if (LivingEntityRenderer.isEntityUpsideDown(player)) {
            headPitch *= -1.0F;
            netHeadYaw *= -1.0F;
        }

        float age = player.tickCount + partialTick;
        PlayerModel rawModel = vanillaPlayerModel;
        // Player Animation Lib resets these pivots at PlayerModel#setupAnim HEAD.
        // Reproducing it is essential because Forge reuses the same PlayerModel after
        // a crawl/rush clip has changed limb positions.
        BedrockAnimationPlayer.resetVanillaPivots(rawModel);
        rawModel.attackTime = player.getAttackAnim(partialTick);
        rawModel.riding = shouldSit;
        rawModel.young = player.isBaby();
        rawModel.prepareMobModel(player,
                limbSwing, limbSwingAmount, partialTick);
        rawModel.setupAnim(player,
                limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
        bodyTransform = applySelection(player, rawModel, partialTick);
    }

    private BedrockAnimationPlayer.BodyTransform applySelection(Player player, PlayerModel<?> model,
                                                                float partialTick) {
        PowerAnimationClientHandler.ActiveAnimation powerAnimation =
                inventoryPreview ? null : PowerAnimationClientHandler.active(player, partialTick);
        FormAnimationSystem.Selection selection = powerAnimation == null
                ? FormAnimationSystem.select(player) : powerAnimation.selection();
        if (powerAnimation != null) {
            // Server-synchronised power animations are their own high-priority layer.
            // SSC Fabric replaces the normal layer for these, rather than fading it.
            BedrockAnimationPlayer.BodyTransform transform = BedrockAnimationPlayer.applyToPlayerModel(model, selection,
                    powerAnimation.timeSeconds(), powerAnimation.forceLoop());
            stashExtraContext(selection, powerAnimation.timeSeconds(), powerAnimation.forceLoop(),
                    null, 0.0F, 1.0F);
            return transform;
        }
        return applyFormAnimation(model, selection, partialTick);
    }

    /**
     * Re-applies the current clip onto a vanilla model that someone else just posed
     * (vanilla setupAnim during the real render pass, which would otherwise wipe the
     * Pre-pass clip pose that layers like held items depend on). Shared fade and power
     * state keeps every pass of the same frame identical. Mirrors PAL, which hooks
     * setupAnim itself rather than replacing the renderer.
     */
    public void reapplySelection(Player player, PlayerModel<?> model, float partialTick) {
        if (player == null || model == null) {
            return;
        }
        Player prevPlayer = this.player;
        this.player = player;
        try {
            applySelection(player, model, partialTick);
        } finally {
            this.player = prevPlayer;
        }
    }

    /** A malformed data animation must fall back to vanilla rendering, never hide a player. */
    public boolean hasSafeRenderState() {
        return bodyTransform.isFinite();
    }

    public void setInventoryPreview(boolean inventoryPreview) {
        this.inventoryPreview = inventoryPreview;
    }

    public boolean isInventoryPreview() {
        return inventoryPreview;
    }

    private BedrockAnimationPlayer.BodyTransform applyFormAnimation(PlayerModel<?> model,
                                                                      FormAnimationSystem.Selection selection,
                                                                      float partialTick) {
        if (player == null || selection == null) {
            discardTimeline();
            stashExtraContext(null, 0.0F, false, null, 0.0F, 1.0F);
            return BedrockAnimationPlayer.BodyTransform.IDENTITY;
        }
        // InventoryScreen supplies a stable, manually posed PlayerModel. PAL still
        // applies the selected clip's initial pose there, but does not advance or
        // cross-fade the world animation clock.
        if (inventoryPreview) {
            discardTimeline();
            stashExtraContext(selection, 0.0F, false, null, 0.0F, 1.0F);
            return BedrockAnimationPlayer.applyToPlayerModel(model, selection, 0.0F);
        }
        double now = player.tickCount + partialTick;
        AnimationTimeline timeline = timelines.computeIfAbsent(player.getUUID(), ignored -> new AnimationTimeline());
        if (!selection.equals(timeline.animation)) {
            if (timeline.animation != null) {
                float previousTime = timeline.timeAt(now);
                if (BedrockAnimationPlayer.isActive(timeline.animation, previousTime)) {
                    timeline.previousAnimation = timeline.animation;
                    timeline.previousStartedAt = timeline.startedAt;
                    timeline.fadeStartedAt = now;
                } else {
                    timeline.previousAnimation = null;
                }
            }
            timeline.animation = selection;
            timeline.startedAt = now;
        }

        float currentTime = timeline.timeAt(now);
        float previousTime = timeline.previousAnimation == null ? 0.0F : (float) ((now - timeline.previousStartedAt) / 20.0D
                * timeline.previousAnimation.speed());
        // A just-expired one-shot (dive entry, jump landing) still fades out of its
        // frozen end pose instead of hard-cutting; only long-dead clips cut straight in.
        float previousLength = timeline.previousAnimation == null ? 0.0F
                : BedrockAnimationPlayer.animationLength(timeline.previousAnimation);
        boolean previousUsable = timeline.previousAnimation != null && previousLength > 0.0F
                && (BedrockAnimationPlayer.isActive(timeline.previousAnimation, previousTime)
                    || previousTime - previousLength <= 20.0F);
        if (!previousUsable || selection.fade() <= 0) {
            stashExtraContext(selection, currentTime, false, null, 0.0F, 1.0F);
            return BedrockAnimationPlayer.applyToPlayerModel(model, selection, currentTime);
        }

        float blend = Mth.clamp((float) ((now - timeline.fadeStartedAt) / selection.fade()), 0.0F, 1.0F);
        if (blend >= 1.0F) {
            timeline.previousAnimation = null;
            stashExtraContext(selection, currentTime, false, null, 0.0F, 1.0F);
            return BedrockAnimationPlayer.applyToPlayerModel(model, selection, currentTime);
        }
        stashExtraContext(selection, currentTime, false, timeline.previousAnimation, previousTime, blend);

        // PAL's AbstractFadeModifier samples both players from the same base PlayerModel
        // pose and linearly blends their results. Capture/restore lets us do that without
        // importing the full PAL layer stack.
        PlayerModelPose baseline = PlayerModelPose.capture(model);
        BedrockAnimationPlayer.BodyTransform previousBody = BedrockAnimationPlayer.applyToPlayerModel(
                model, timeline.previousAnimation, previousTime);
        PlayerModelPose previousPose = PlayerModelPose.capture(model);
        baseline.apply(model);
        BedrockAnimationPlayer.BodyTransform currentBody = BedrockAnimationPlayer.applyToPlayerModel(
                model, selection, currentTime);
        PlayerModelPose currentPose = PlayerModelPose.capture(model);
        PlayerModelPose.lerp(previousPose, currentPose, blend).apply(model);
        return BedrockAnimationPlayer.BodyTransform.lerp(previousBody, currentBody, blend);
    }

    private void discardTimeline() {
        if (player != null) timelines.remove(player.getUUID());
    }

    private void stashExtraContext(FormAnimationSystem.Selection primary, float primaryTime, boolean forceLoop,
                                   FormAnimationSystem.Selection secondary, float secondaryTime, float blend) {
        extraPrimary = primary;
        extraPrimaryTime = primaryTime;
        extraPrimaryForceLoop = forceLoop;
        extraSecondary = secondary;
        extraSecondaryTime = secondaryTime;
        extraBlend = blend;
    }

    /**
     * Samples a form-only clip bone using the same layer, clock and cross-fade as the
     * PlayerModel pass, mirroring PAL's {@code get3DTransform} reads in
     * {@code ProcessExtraBone}. Returns null when the current clip does not animate
     * the bone; the caller then leaves the GeoBone at its reset pose.
     */
    public BedrockAnimationPlayer.BoneSample sampleExtraBone(String animBoneName) {
        if (extraPrimary == null) {
            return null;
        }
        BedrockAnimationPlayer.BoneSample primary =
                sampleWithFallback(extraPrimary, animBoneName, extraPrimaryTime, extraPrimaryForceLoop);
        if (primary == null) {
            return null;
        }
        if (extraSecondary == null || extraBlend >= 1.0F) {
            return primary;
        }
        BedrockAnimationPlayer.BoneSample secondary =
                sampleWithFallback(extraSecondary, animBoneName, extraSecondaryTime, false);
        if (secondary == null) {
            return primary;
        }
        return BedrockAnimationPlayer.BoneSample.lerp(secondary, primary, extraBlend);
    }

    private static BedrockAnimationPlayer.BoneSample sampleWithFallback(FormAnimationSystem.Selection selection,
                                                                        String boneName, float time, boolean forceLoop) {
        ResourceLocation resource = selection.resource();
        if (!BedrockAnimationPlayer.hasAnimation(resource, selection.animationId())
                && selection.fallbackResource() != null) {
            resource = selection.fallbackResource();
        }
        return BedrockAnimationPlayer.sampleBone(resource, selection.animationId(), boneName, time, forceLoop);
    }

    /**
     * Returns the time for the surface-sprint Geo clip, or a negative value when it is
     * inactive. The non-looping Bedrock clip holds its final pose while active; the base
     * layer (swim or crawl animation plus the procedural tail chain) stays underneath it.
     * Besides sprinting in water, the crawling base clips layer the same tail motion.
     */
    public float axolotlSurfaceSprintOverlayTime(float partialTick) {
        if (player == null || inventoryPreview || !SscClientConfig.PREFER_NEW_ANIMATIONS.get()) {
            return -1.0F;
        }

        OverlayTimeline overlay = overlayTimelines.computeIfAbsent(
                player.getUUID(),
                ignored -> new OverlayTimeline()
        );

        AnimationTimeline baseTimeline = timelines.get(player.getUUID());
        String animationId = baseTimeline != null && baseTimeline.animation != null
                ? baseTimeline.animation.animationId()
                : null;

        boolean crawling =
                "axolotl_3_crawling_idle".equals(animationId)
                || "axolotl_3_crawling".equals(animationId);

        boolean surfaceSprinting =
                "axolotl_3".equals(FormManager.current(player).id().getPath())
                && player.isSprinting()
                && player.isInWater();

        boolean active = crawling || surfaceSprinting;

        if (!active) {
            overlay.active = false;
            return -1.0F;
        }

        double now = player.tickCount + partialTick;

        if (!overlay.active) {
            overlay.active = true;
            overlay.startedAt = now;
        }

        return (float) ((now - overlay.startedAt) / 20.0D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", state -> {
            state.setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object animatable) {
        return player == null ? 0.0D : player.tickCount + Minecraft.getInstance().getFrameTime();
    }

    private static final class AnimationTimeline {
        private FormAnimationSystem.Selection animation;
        private FormAnimationSystem.Selection previousAnimation;
        private double startedAt;
        private double previousStartedAt;
        private double fadeStartedAt;

        private float timeAt(double now) {
            return animation == null ? 0.0F : (float) ((now - startedAt) / 20.0D * animation.speed());
        }
    }

    /** Minimal mutable PlayerModel pose used for PAL-compatible cross-fades. */
    private record PlayerModelPose(PartPose head, PartPose body, PartPose rightArm,
                                   PartPose leftArm, PartPose rightLeg, PartPose leftLeg) {
        private static PlayerModelPose capture(PlayerModel<?> model) {
            return new PlayerModelPose(PartPose.capture(model.head), PartPose.capture(model.body),
                    PartPose.capture(model.rightArm), PartPose.capture(model.leftArm),
                    PartPose.capture(model.rightLeg), PartPose.capture(model.leftLeg));
        }

        private void apply(PlayerModel<?> model) {
            head.apply(model.head);
            body.apply(model.body);
            rightArm.apply(model.rightArm);
            leftArm.apply(model.leftArm);
            rightLeg.apply(model.rightLeg);
            leftLeg.apply(model.leftLeg);
        }

        private static PlayerModelPose lerp(PlayerModelPose from, PlayerModelPose to, float amount) {
            return new PlayerModelPose(PartPose.lerp(from.head, to.head, amount),
                    PartPose.lerp(from.body, to.body, amount),
                    PartPose.lerp(from.rightArm, to.rightArm, amount),
                    PartPose.lerp(from.leftArm, to.leftArm, amount),
                    PartPose.lerp(from.rightLeg, to.rightLeg, amount),
                    PartPose.lerp(from.leftLeg, to.leftLeg, amount));
        }
    }

    private record PartPose(float x, float y, float z, float xRot, float yRot, float zRot) {
        private static PartPose capture(net.minecraft.client.model.geom.ModelPart part) {
            return new PartPose(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot);
        }

        private void apply(net.minecraft.client.model.geom.ModelPart part) {
            part.x = x;
            part.y = y;
            part.z = z;
            part.xRot = xRot;
            part.yRot = yRot;
            part.zRot = zRot;
        }

        private static PartPose lerp(PartPose from, PartPose to, float amount) {
            return new PartPose(Mth.lerp(amount, from.x, to.x), Mth.lerp(amount, from.y, to.y),
                    Mth.lerp(amount, from.z, to.z), Mth.lerp(amount, from.xRot, to.xRot),
                    Mth.lerp(amount, from.yRot, to.yRot), Mth.lerp(amount, from.zRot, to.zRot));
        }
    }

    private static final class OverlayTimeline {
        private boolean active;
        private double startedAt;
    }
}
