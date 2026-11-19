package net.onixary.shapeShifterCurseForge.client.render;

import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;

/** Virtual parent for independent extra bones when a clip supplies no dedicated track. */
final class GeoBoneAttachment {
    private GeoBoneAttachment() { }

    // Pixel units throughout; mirrors GeckoLib RenderUtils, including its negated position X.
    private static Matrix4f matrix(GeoBone bone) {
        return new Matrix4f().translation(-bone.getPosX(), bone.getPosY(), bone.getPosZ())
                .translate(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ())
                .rotateZYX(bone.getRotZ(), bone.getRotY(), bone.getRotX())
                .scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ())
                .translate(-bone.getPivotX(), -bone.getPivotY(), -bone.getPivotZ());
    }

    /** Target must have been reset to its baked pose; both bones share the same parent. */
    static BedrockAnimationPlayer.BoneSample follow(GeoBone driver, GeoBone target) {
        Matrix4f transform = matrix(driver).mul(matrix(target));
        Vector3f pivot = new Vector3f(target.getPivotX(), target.getPivotY(), target.getPivotZ());
        Vector3f offset = transform.transformPosition(new Vector3f(pivot)).sub(pivot);
        Vector3f scale = transform.getScale(new Vector3f());
        Vector3f rotation = transform.get3x3(new Matrix3f())
                .scale(inverse(scale.x), inverse(scale.y), inverse(scale.z)).getEulerAnglesZYX(new Vector3f());
        target.setScaleX(scale.x);
        target.setScaleY(scale.y);
        target.setScaleZ(scale.z);
        float toDegrees = 180.0F / (float) Math.PI;
        return new BedrockAnimationPlayer.BoneSample(rotation.x * toDegrees, -rotation.y * toDegrees,
                -rotation.z * toDegrees, offset.x, offset.y, -offset.z);
    }

    private static float inverse(float scale) {
        return scale > 1.0E-6F ? 1.0F / scale : 1.0F;
    }
}
