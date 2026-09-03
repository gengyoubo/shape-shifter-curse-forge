package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
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
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private Player player;
    private PlayerModel<?> vanillaPlayerModel;
    private boolean inventoryPreview;
    private final Map<UUID, AnimationTimeline> timelines = new HashMap<>();

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

    /**
     * Forge fires RenderPlayerEvent.Pre before LivingEntityRenderer calls setupAnim.
     * Fabric's form feature runs after that preparation, so recreate the vanilla pose here.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void prepareVanillaPlayerPose(float partialTick) {
        if (player == null || vanillaPlayerModel == null) {
            return;
        }
        float limbSwing = player.walkAnimation.position(partialTick);
        float limbSwingAmount = player.walkAnimation.speed(partialTick);
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float headPitch = player.getViewXRot(partialTick);
        float age = player.tickCount + partialTick;
        PlayerModel rawModel = vanillaPlayerModel;
        rawModel.prepareMobModel(player,
                limbSwing, limbSwingAmount, partialTick);
        rawModel.setupAnim(player,
                limbSwing, limbSwingAmount, age, Mth.wrapDegrees(headYaw - bodyYaw), headPitch);
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
}
