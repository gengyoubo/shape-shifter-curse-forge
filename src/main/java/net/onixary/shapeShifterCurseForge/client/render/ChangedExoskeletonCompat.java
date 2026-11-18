package net.onixary.shapeShifterCurseForge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

/**
 * Keeps a worn Changed exoskeleton bound to the form model (issue #21).
 *
 * <p>The exoskeleton copies its limbs from the vanilla {@link PlayerModel}, which already
 * carries the SSC pose, so its limbs match the Geo bones. Changed also adds a renderer-level
 * root lift in {@code LivingEntityRenderer#setupRotations}, though. Only vanilla layers
 * (exoskeleton, armor, held items) receive it, so the whole suit sat above the Geo form.
 * This mirrors the lift into the Geo root at the same point in the transform order, like the
 * PlayerAnimator root bridge from issue #6.</p>
 */
public final class ChangedExoskeletonCompat {
    private static final boolean CHANGED_LOADED = ModList.get() != null && ModList.get().isLoaded("changed");

    private ChangedExoskeletonCompat() {
    }

    /** Call after the form scale and before the vanilla rotations, matching PlayerRenderer. */
    public static void applyWearerPose(Player player, PlayerModel<?> vanillaModel, PoseStack poseStack) {
        if (!CHANGED_LOADED) return;
        try {
            Bridge.applyWearerPose(player, vanillaModel, poseStack);
        } catch (LinkageError | RuntimeException ignored) {
            // An incompatible Changed build must never stop the form from rendering.
        }
    }

    /** Only loaded when Changed is present. */
    private static final class Bridge {
        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void applyWearerPose(Player player, PlayerModel<?> model, PoseStack poseStack) {
            PlayerModel vanillaModel = model;
            net.ltxprogrammer.changed.entity.robot.Exoskeleton.getEntityExoskeleton(player).ifPresent(pair ->
                    net.ltxprogrammer.changed.client.renderer.layers.AccessoryLayer.getRenderer(pair.getSecond())
                            .ifPresent(renderer -> {
                                if (renderer instanceof net.ltxprogrammer.changed.client.renderer.accessory
                                        .WornExoskeletonRenderer exoRenderer && exoRenderer.getModel() != null) {
                                    exoRenderer.getModel().animateWearerPose(player, vanillaModel, poseStack,
                                            pair.getFirst());
                                }
                            }));
        }
    }
}
