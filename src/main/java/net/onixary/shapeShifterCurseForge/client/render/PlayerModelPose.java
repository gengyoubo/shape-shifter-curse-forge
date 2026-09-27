package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.onixary.shapeShifterCurseForge.animation.AnimationTransition;

/** Immutable limb pose shared by the form overlay and vanilla equipment layers. */
public record PlayerModelPose(PartPose head, PartPose body, PartPose rightArm,
                               PartPose leftArm, PartPose rightLeg, PartPose leftLeg) {
    public static PlayerModelPose capture(PlayerModel<?> model) {
        return new PlayerModelPose(PartPose.capture(model.head), PartPose.capture(model.body),
                PartPose.capture(model.rightArm), PartPose.capture(model.leftArm),
                PartPose.capture(model.rightLeg), PartPose.capture(model.leftLeg));
    }

    public void apply(PlayerModel<?> model) {
        head.apply(model.head);
        body.apply(model.body);
        rightArm.apply(model.rightArm);
        leftArm.apply(model.leftArm);
        rightLeg.apply(model.rightLeg);
        leftLeg.apply(model.leftLeg);
        model.hat.copyFrom(model.head);
    }

    public static PlayerModelPose lerp(PlayerModelPose from, PlayerModelPose to, float amount) {
        return new PlayerModelPose(PartPose.lerp(from.head, to.head, amount),
                PartPose.lerp(from.body, to.body, amount),
                PartPose.lerp(from.rightArm, to.rightArm, amount),
                PartPose.lerp(from.leftArm, to.leftArm, amount),
                PartPose.lerp(from.rightLeg, to.rightLeg, amount),
                PartPose.lerp(from.leftLeg, to.leftLeg, amount));
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
                    Mth.lerp(amount, from.z, to.z), AnimationTransition.rotation(from.xRot, to.xRot, amount),
                    AnimationTransition.rotation(from.yRot, to.yRot, amount), AnimationTransition.rotation(from.zRot, to.zRot, amount));
        }
    }

}
