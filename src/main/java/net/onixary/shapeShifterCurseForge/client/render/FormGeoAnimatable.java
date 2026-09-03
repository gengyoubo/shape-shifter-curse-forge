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
        rawModel.attackTime = player.getAttackAnim(partialTick);
        rawModel.riding = shouldSit;
        rawModel.young = player.isBaby();
        rawModel.prepareMobModel(player,
                limbSwing, limbSwingAmount, partialTick);
        rawModel.setupAnim(player,
                limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
        FormAnimationSystem.Selection selection = FormAnimationSystem.select(player);
        bodyTransform = BedrockAnimationPlayer.applyToPlayerModel(rawModel, selection,
                animationTime(selection, partialTick));
    }

    public void setInventoryPreview(boolean inventoryPreview) {
        this.inventoryPreview = inventoryPreview;
    }

    public boolean isInventoryPreview() {
        return inventoryPreview;
    }

    public float animationTime(FormAnimationSystem.Selection selection, float partialTick) {
        if (player == null || selection == null || inventoryPreview) {
            return 0.0F;
        }
        double now = player.tickCount + partialTick;
        AnimationTimeline timeline = timelines.computeIfAbsent(player.getUUID(), ignored -> new AnimationTimeline());
        if (!selection.id().equals(timeline.animationId)) {
            timeline.animationId = selection.id();
            timeline.startedAt = now;
        }
        return (float) ((now - timeline.startedAt) / 20.0D * selection.speed());
    }

    /**
     * Returns the time for the surface-sprint Geo clip, or a negative value when it is
     * inactive. The non-looping Bedrock clip holds its final pose until sprinting in
     * water ends; the normal swim animation remains the base layer underneath it.
     */
    public float axolotlSurfaceSprintOverlayTime(float partialTick) {
        if (player == null || inventoryPreview || !SscClientConfig.PREFER_NEW_ANIMATIONS.get()) {
            return -1.0F;
        }

        OverlayTimeline timeline = overlayTimelines.computeIfAbsent(player.getUUID(), ignored -> new OverlayTimeline());
        boolean surfaceSprinting = FormManager.current(player).id().getPath().equals("axolotl_3")
                && player.isSprinting() && player.isInWater();
        if (!surfaceSprinting) {
            timeline.surfaceSprinting = false;
            return -1.0F;
        }

        double now = player.tickCount + partialTick;
        if (!timeline.surfaceSprinting) {
            timeline.surfaceSprinting = true;
            timeline.startedAt = now;
        }
        return (float) ((now - timeline.startedAt) / 20.0D);
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
        private String animationId;
        private double startedAt;
    }

    private static final class OverlayTimeline {
        private boolean surfaceSprinting;
        private double startedAt;
    }
}
