package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * Port of Fabric's {@code DefaultModelAnimationSystem#modifyHeadPart}: on a long-neck form
 * the vanilla head pivot stays at the shoulders while the Geo head sits at the end of the
 * neck. Helmets (including morphscale headrings) and head items copy the vanilla head, so
 * they are moved onto the neck head before rendering.
 */
public final class LongNeckHeadAlignment {
    private LongNeckHeadAlignment() {
    }

    /** Moves {@code head} onto the form's neck head. Returns false when the form has no long neck. */
    public static boolean apply(Player player, ModelPart head) {
        FormGeoModel model = GeoBonePose.activeModel(player);
        GeoBone neckHead = model == null ? null : model.neckHeadBone().orElse(null);
        if (neckHead == null || head == null) return false;
        GeoBonePose.applyTo(neckHead, head);
        return true;
    }

    /** Head pose saved before a layer borrows the shared player-model head. */
    public record SavedHead(ModelPart part, float x, float y, float z, float xRot, float yRot, float zRot) {
        public static SavedHead of(ModelPart part) {
            return new SavedHead(part, part.x, part.y, part.z, part.xRot, part.yRot, part.zRot);
        }

        public void restore() {
            part.x = x;
            part.y = y;
            part.z = z;
            part.xRot = xRot;
            part.yRot = yRot;
            part.zRot = zRot;
        }
    }
}
