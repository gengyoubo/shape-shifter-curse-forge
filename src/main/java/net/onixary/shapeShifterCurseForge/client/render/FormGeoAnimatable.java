package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.Minecraft;
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
    private boolean inventoryPreview;
    private final Map<UUID, AnimationTimeline> timelines = new HashMap<>();

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
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
