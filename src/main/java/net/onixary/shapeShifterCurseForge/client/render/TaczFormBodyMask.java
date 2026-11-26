package net.onixary.shapeShifterCurseForge.client.render;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.onixary.shapeShifterCurseForge.animation.AnimationTransition;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormManager;

/** Adapts TaCZ poses to the SSC skeleton without moving its torso or leg pivots. */
public final class TaczFormBodyMask extends AbstractModifier {
    public enum Layer {
        LOWER, UPPER, ROTATION
    }

    private static final float GUN_WALK_THRESHOLD = 0.05F;
    private static final float STANCE_FADE_SPEED = 0.15F;
    private final AbstractClientPlayer player;
    private final Layer layer;

    private TaczFormBodyMask(AbstractClientPlayer player, Layer layer) {
        this.player = player;
        this.layer = layer;
    }

    public static PlayerAnimationFactory wrap(PlayerAnimationFactory factory, Layer layer) {
        return player -> {
            var animation = factory.invoke(player);
            if (animation instanceof ModifierLayer<?> modifiers) {
                modifiers.addModifierBefore(new TaczFormBodyMask(player, layer));
            }
            return animation;
        };
    }

    @Override
    public Vec3f get3DTransform(String part, TransformType type, float partialTick, Vec3f base) {
        if (!GeoBonePose.isFramePlayer(player)) {
            return super.get3DTransform(part, type, partialTick, base);
        }
        // A clip started before transforming can still occupy a TaCZ layer while it fades out.
        if (TaczAnimationCompat.usesSscThirdPerson(player)) {
            return base;
        }
        if (layer == Layer.ROTATION) {
            return super.get3DTransform(part, type, partialTick, base);
        }
        boolean leg = "leftLeg".equals(part) || "rightLeg".equals(part);
        if (layer == Layer.LOWER && leg && type == TransformType.ROTATION) {
            return applyStandingStance(part, type, partialTick, base);
        }
        if (layer == Layer.LOWER || leg || "body".equals(part) || "torso".equals(part)) {
            return base;
        }
        return super.get3DTransform(part, type, partialTick, base);
    }

    private Vec3f applyStandingStance(String part, TransformType type, float partialTick, Vec3f base) {
        if (!player.onGround() || !player.hasPose(Pose.STANDING) || player.isPassenger()
                || player.isInWater() || FormAnimationSystem.isTransitioning(player)
                || FormManager.current(player).bodyType() == FormBodyType.FERAL) {
            return base;
        }
        // Blend back to the form's own stride once TaCZ switches from its standing clip to walking.
        float speed = player.walkAnimation.speed(partialTick);
        float weight = 1.0F - Mth.clamp((speed - GUN_WALK_THRESHOLD) / STANCE_FADE_SPEED, 0.0F, 1.0F);
        if (weight == 0.0F) {
            return base;
        }
        Vec3f stance = super.get3DTransform(part, type, partialTick, base);
        return new Vec3f(AnimationTransition.rotation(base.getX(), stance.getX(), weight),
                AnimationTransition.rotation(base.getY(), stance.getY(), weight),
                AnimationTransition.rotation(base.getZ(), stance.getZ(), weight));
    }
}
