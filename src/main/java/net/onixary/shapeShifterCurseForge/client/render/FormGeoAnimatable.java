package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.Minecraft;
import net.onixary.shapeShifterCurseForge.animation.AnimationTransition;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.client.PowerAnimationClientHandler;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.WeakHashMap;
import java.util.Map;

public final class FormGeoAnimatable implements GeoAnimatable {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private Player player;
    private PlayerModel<?> vanillaPlayerModel;
    private BedrockAnimationPlayer.BodyTransform bodyTransform = BedrockAnimationPlayer.BodyTransform.IDENTITY;
    private boolean inventoryPreview;
    private ResourceLocation animationResource;
    private boolean fullyCustomModel;
    private final Map<Player, AnimationTimeline> timelines = new WeakHashMap<>();
    private FormAnimationSystem.Selection extraPrimary;
    private float extraPrimaryTime;
    private boolean extraPrimaryForceLoop;
    private FormAnimationSystem.Selection extraSecondary;
    private float extraSecondaryTime;
    private boolean extraSecondaryForceLoop;
    private float extraBlend = 1.0F;
    private boolean preparingVanillaPlayerPose;
    private Player preparedPlayer;
    private PlayerModelPose preparedPose;
    private PlayerModelPose preparedBasePose;
    private float preparingPartialTick;
    private BedrockAnimationPlayer.BodyTransform externalBodyTransform = BedrockAnimationPlayer.BodyTransform.IDENTITY;

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

    public BedrockAnimationPlayer.BodyTransform getExternalBodyTransform() {
        return externalBodyTransform;
    }

    public boolean isPreparingVanillaPlayerPose() {
        return preparingVanillaPlayerPose;
    }

    public void clearPreparedPose() {
        preparedPlayer = null;
        preparedPose = null;
        preparedBasePose = null;
        externalBodyTransform = BedrockAnimationPlayer.BodyTransform.IDENTITY;
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
        clearPreparedPose();
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
        externalBodyTransform = PlayerAnimatorCompat.prepare(player, partialTick);
        preparingPartialTick = partialTick;
        preparingVanillaPlayerPose = true;
        try {
            rawModel.setupAnim(player,
                    limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
        } finally {
            preparingVanillaPlayerPose = false;
        }
        // The setupAnim hook applied SSC before PlayerAnimator. Capture only after
        // setupAnim returns, so the Geo overlay sees the external layers as well.
        preparedPose = PlayerModelPose.capture(rawModel);
        preparedPlayer = player;
    }

    public void prepareSelection(Player player, PlayerModel<?> model) {
        if (!preparingVanillaPlayerPose || player != this.player) return;
        bodyTransform = applySelection(player, model, preparingPartialTick);
        preparedBasePose = PlayerModelPose.capture(model);
    }

    private BedrockAnimationPlayer.BodyTransform applySelection(Player player, PlayerModel<?> model,
                                                                float partialTick) {
        // SSC's pre-process (transform) wins over power, then ordinary locomotion.
        FormAnimationSystem.Selection transition = FormAnimationSystem.transitionAnimation(player);
        PowerAnimationClientHandler.ActiveAnimation powerAnimation = inventoryPreview || transition != null
                ? null : PowerAnimationClientHandler.active(player, partialTick);
        FormAnimationSystem.Selection selection = transition != null ? transition : powerAnimation == null
                ? FormAnimationSystem.select(player) : powerAnimation.selection();
        return applyFormAnimation(model, selection, partialTick, powerAnimation);
    }

    /** Restore SSC's base once; PlayerAnimator applies its layers afterwards, once per pass. */
    public void reapplySelection(Player player, PlayerModel<?> model, float partialTick) {
        if (model != null && hasPreparedPose(player) && preparedBasePose != null) preparedBasePose.apply(model);
    }

    public boolean hasPreparedPose(Player player) {
        return player != null && preparedPlayer == player && preparedPose != null;
    }

    /** A malformed data animation must fall back to vanilla rendering, never hide a player. */
    public boolean hasSafeRenderState() {
        return !bodyTransform.isFinite() || !externalBodyTransform.isFinite();
    }

    public void setInventoryPreview(boolean inventoryPreview) {
        this.inventoryPreview = inventoryPreview;
    }

    public void setAnimationResource(ResourceLocation animationResource, boolean fullyCustomModel) {
        this.animationResource = animationResource;
        this.fullyCustomModel = fullyCustomModel;
    }

    public boolean isInventoryPreview() {
        return inventoryPreview;
    }

    /**
     * The axolotl crawl clips contain their own 90-degree {@code body} transform.
     * A visually-crawling player must not receive Minecraft's swim rotation on top
     * of that same root transform.
     */
    public boolean usesAxolotlCrawlBodyTransform() {
        return extraPrimary != null
                && ("axolotl_3_crawling".equals(extraPrimary.id())
                || "axolotl_3_crawling_idle".equals(extraPrimary.id()));
    }

    /**
     * Attachment power clips and axolotl crawl clips author their own body pose.
     * Vanilla's interpolated swimming/crawling rotation must not be stacked on top.
     */
    public boolean suppressesVanillaSwimRotation() {
        return usesAxolotlCrawlBodyTransform()
                || extraPrimary != null && ("axolotl_2_crawling_jump".equals(extraPrimary.id())
                || "bat_3_attach_bottom".equals(extraPrimary.id())
                || "bat_3_attach_side".equals(extraPrimary.id())
                || "avali_attach_side".equals(extraPrimary.id())
                || "form_feral_common_climb".equals(extraPrimary.id())
                || "form_feral_common_climb_idle".equals(extraPrimary.id()));
    }

    private BedrockAnimationPlayer.BodyTransform applyFormAnimation(PlayerModel<?> model,
                                                                      FormAnimationSystem.Selection selection,
                                                                      float partialTick,
                                                                      PowerAnimationClientHandler.ActiveAnimation power) {
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
        double now = player.level().getGameTime() + partialTick;
        AnimationTimeline timeline = timelines.computeIfAbsent(player, ignored -> new AnimationTimeline());
        Object playbackKey = power == null ? null : power.clock();
        boolean forceLoop = power != null && power.forceLoop();
        if (!selection.equals(timeline.animation) || timeline.playbackKey != playbackKey) {
            if (timeline.animation != null) {
                float previousTime = timeline.timeAt(now);
                if (timeline.forceLoop || BedrockAnimationPlayer.isActive(timeline.animation, previousTime)) {
                    timeline.previousAnimation = timeline.animation;
                    timeline.previousStartedAt = timeline.startedAt;
                    timeline.previousForceLoop = timeline.forceLoop;
                    timeline.fadeStartedAt = now;
                } else {
                    timeline.previousAnimation = null;
                }
            }
            timeline.fadeStartedAt = now;
            timeline.animation = selection;
            timeline.startedAt = power == null ? now : now - power.timeSeconds() * 20.0D / selection.speed();
            timeline.forceLoop = forceLoop;
            timeline.playbackKey = playbackKey;
        }

        float currentTime = timeline.timeAt(now);
        float previousTime = timeline.previousAnimation == null ? 0.0F : (float) ((now - timeline.previousStartedAt) / 20.0D
                * timeline.previousAnimation.speed());
        // A missing previous clip means fade from the vanilla base pose.
        // Completed non-looping clips stop contributing, as in the PAL modifier chain.
        if (timeline.previousAnimation != null && !timeline.previousForceLoop
                && !BedrockAnimationPlayer.isActive(timeline.previousAnimation, previousTime)) {
            timeline.previousAnimation = null;
        }
        if (selection.fade() <= 0 || selection.transition().skipFade()) {
            stashExtraContext(selection, currentTime, forceLoop, null, 0.0F, 1.0F);
            return BedrockAnimationPlayer.applyToPlayerModel(model, selection, currentTime, forceLoop);
        }

        float blend = selection.transition().blend(now - timeline.fadeStartedAt, selection.fade());
        if (blend >= 1.0F) {
            timeline.previousAnimation = null;
            stashExtraContext(selection, currentTime, forceLoop, null, 0.0F, 1.0F);
            return BedrockAnimationPlayer.applyToPlayerModel(model, selection, currentTime, forceLoop);
        }
        stashExtraContext(selection, currentTime, forceLoop, timeline.previousAnimation, previousTime, blend);
        extraSecondaryForceLoop = timeline.previousForceLoop;

        // PAL's AbstractFadeModifier samples both players from the same base PlayerModel
        // pose and linearly blends their results. Capture/restore lets us do that without
        // importing the full PAL layer stack.
        PlayerModelPose baseline = PlayerModelPose.capture(model);
        BedrockAnimationPlayer.BodyTransform previousBody = BedrockAnimationPlayer.applyToPlayerModel(
                model, timeline.previousAnimation, previousTime, timeline.previousForceLoop);
        PlayerModelPose previousPose = PlayerModelPose.capture(model);
        baseline.apply(model);
        BedrockAnimationPlayer.BodyTransform currentBody = BedrockAnimationPlayer.applyToPlayerModel(
                model, selection, currentTime, forceLoop);
        PlayerModelPose currentPose = PlayerModelPose.capture(model);
        PlayerModelPose.lerp(previousPose, currentPose, blend).apply(model);
        return BedrockAnimationPlayer.BodyTransform.lerp(previousBody, currentBody, blend);
    }

    private void discardTimeline() {
        if (player != null) timelines.remove(player);
    }

    private void stashExtraContext(FormAnimationSystem.Selection primary, float primaryTime, boolean forceLoop,
                                   FormAnimationSystem.Selection secondary, float secondaryTime, float blend) {
        extraPrimary = primary;
        extraPrimaryTime = primaryTime;
        extraPrimaryForceLoop = forceLoop;
        extraSecondary = secondary;
        extraSecondaryTime = secondaryTime;
        extraSecondaryForceLoop = false;
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
        if (extraBlend >= 1.0F) {
            return primary;
        }
        BedrockAnimationPlayer.BoneSample secondary =
                sampleWithFallback(extraSecondary, animBoneName, extraSecondaryTime, extraSecondaryForceLoop);
        if (primary == null && secondary == null) return null;
        BedrockAnimationPlayer.BoneSample identity = new BedrockAnimationPlayer.BoneSample(0, 0, 0, 0, 0, 0);
        return BedrockAnimationPlayer.BoneSample.lerp(secondary == null ? identity : secondary,
                primary == null ? identity : primary, extraBlend);
    }

    private static BedrockAnimationPlayer.BoneSample sampleWithFallback(FormAnimationSystem.Selection selection,
                                                                        String boneName, float time, boolean forceLoop) {
        if (selection == null) return null;
        ResourceLocation resource = selection.resource();
        if (!BedrockAnimationPlayer.hasAnimation(resource, selection.animationId())
                && selection.fallbackResource() != null) {
            resource = selection.fallbackResource();
        }
        return BedrockAnimationPlayer.sampleBone(resource, selection.animationId(), boneName, time, forceLoop);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", state -> {
            // Normal forms are driven by the copied vanilla PlayerModel pose plus SSC's
            // Bedrock animation system. Fully custom forms explicitly opt into a GeckoLib
            // animation resource and must provide the conventional "idle" clip.
            if (fullyCustomModel && animationResource != null) {
                state.setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
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
        private Object playbackKey;
        private boolean forceLoop;
        private boolean previousForceLoop;
        private double startedAt;
        private double previousStartedAt;
        private double fadeStartedAt;

        private float timeAt(double now) {
            return animation == null ? 0.0F : (float) ((now - startedAt) / 20.0D * animation.speed());
        }
    }

}
