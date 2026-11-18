package net.onixary.shapeShifterCurseForge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Binds vanilla equipment to Geo form bones.
 *
 * <p>Preferred path (legs): while the Geo form renders, the complete accumulated render
 * matrix of each tracked bone is captured. A vanilla layer later expresses it relative to
 * its own {@code PoseStack} ({@code layer⁻¹ · bone}); the camera, entity rotations, form
 * scale and every root transform cancel out, so the equipment is drawn in exactly the
 * frame the bone was drawn in. Nothing is decomposed into a {@link ModelPart}.</p>
 *
 * <p>{@link #applyTo} remains for layers that read a {@code ModelPart} directly.</p>
 */
public final class GeoBonePose {
    public static final String LEFT_LEG = "bipedLeftLeg";
    public static final String RIGHT_LEG = "bipedRightLeg";
    public static final String LEFT_HIND_LEG = "bipedLeftHindLeg";
    public static final String RIGHT_HIND_LEG = "bipedRightHindLeg";
    private static final Set<String> TRACKED = Set.of(LEFT_LEG, RIGHT_LEG, LEFT_HIND_LEG, RIGHT_HIND_LEG);

    private static final float ARMOR_LEG_LENGTH = 12.0F;
    private static final Map<GeoBone, Vector3f> LEG_ANCHORS = new WeakHashMap<>();

    private static Player framePlayer;
    private static final Map<String, Matrix4f> FRAME_MATRICES = new HashMap<>();
    private static final Map<String, GeoBone> FRAME_BONES = new HashMap<>();

    private GeoBonePose() {
    }

    // ---- per-frame capture (Geo pass) ------------------------------------------------------

    /** Called before the Geo form renders; matrices stay valid until the player's Post event. */
    public static void beginFrame(Player player) {
        framePlayer = player;
        FRAME_MATRICES.clear();
        FRAME_BONES.clear();
    }

    public static void endFrame() {
        framePlayer = null;
        FRAME_MATRICES.clear();
        FRAME_BONES.clear();
    }

    /**
     * Called from {@code renderCubesOfBone}, where GeckoLib's {@code PoseStack} holds the bone's
     * full accumulated transform followed by {@code translate(-pivot)}. Undoing that last
     * translation gives the bone's own frame at its pivot.
     */
    static void capture(Player player, GeoBone bone, PoseStack poseStack) {
        if (player == null || player != framePlayer || !TRACKED.contains(bone.getName())) return;
        Matrix4f atPivot = new Matrix4f(poseStack.last().pose())
                .translate(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
        FRAME_MATRICES.put(bone.getName(), atPivot);
        FRAME_BONES.put(bone.getName(), bone);
    }

    /** Whether this frame drew a four-legged Geo form for {@code player}. */
    public static boolean hasHindLegs(Player player) {
        return player == framePlayer
                && (FRAME_MATRICES.containsKey(LEFT_HIND_LEG) || FRAME_MATRICES.containsKey(RIGHT_HIND_LEG));
    }

    /**
     * The armor-leg frame for a captured leg bone, relative to {@code layerPose}, or null.
     * A 4x12x4 vanilla armor leg drawn at the origin of this frame (with an identity
     * {@code ModelPart} pose) covers the form leg: the frame is moved to the centre of the
     * leg's cubes, 12px above its lowest point, then turned from Geo axes (y up, +z front)
     * into vanilla part axes (y down, -z front), which is a 180° turn about X.
     */
    public static Matrix4f legArmorFrame(Player player, String boneName, Matrix4f layerPose) {
        if (player != framePlayer) return null;
        Matrix4f boneMatrix = FRAME_MATRICES.get(boneName);
        GeoBone bone = FRAME_BONES.get(boneName);
        if (boneMatrix == null || bone == null) return null;
        Vector3f anchor = LEG_ANCHORS.computeIfAbsent(bone, GeoBonePose::legAnchor);
        return new Matrix4f(layerPose).invert().mul(boneMatrix)
                .translate(anchor.x() / 16.0F, anchor.y() / 16.0F, anchor.z() / 16.0F)
                .rotateX((float) Math.PI);
    }

    /** Multiplies a relative frame onto {@code poseStack}, keeping its normal matrix in step. */
    public static void mulFrame(PoseStack poseStack, Matrix4f frame) {
        PoseStack.Pose last = poseStack.last();
        last.pose().mul(frame);
        last.normal().mul(new Matrix3f(frame).normal());
    }

    /** Leg anchor in the bone's local Geo frame (pixels, relative to the bone pivot). */
    private static Vector3f legAnchor(GeoBone bone) {
        float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        collectBounds(bone, bounds);
        if (bounds[0] > bounds[3]) return new Vector3f(0.0F, 0.0F, 0.0F);
        return new Vector3f((bounds[0] + bounds[3]) * 0.5F - bone.getPivotX(),
                bounds[1] + ARMOR_LEG_LENGTH - bone.getPivotY(),
                (bounds[2] + bounds[5]) * 0.5F - bone.getPivotZ());
    }

    /** Baked vertices are in blocks, in the same (x-negated) Geo space as the bone pivots. */
    private static void collectBounds(GeoBone bone, float[] bounds) {
        for (GeoCube cube : bone.getCubes()) {
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) continue;
                for (GeoVertex vertex : quad.vertices()) {
                    Vector3f p = vertex.position();
                    bounds[0] = Math.min(bounds[0], p.x() * 16.0F);
                    bounds[1] = Math.min(bounds[1], p.y() * 16.0F);
                    bounds[2] = Math.min(bounds[2], p.z() * 16.0F);
                    bounds[3] = Math.max(bounds[3], p.x() * 16.0F);
                    bounds[4] = Math.max(bounds[4], p.y() * 16.0F);
                    bounds[5] = Math.max(bounds[5], p.z() * 16.0F);
                }
            }
        }
        for (GeoBone child : bone.getChildBones()) collectBounds(child, bounds);
    }

    // ---- ModelPart path (only where a layer reads the part itself) -------------------------

    /** The Geo model drawn for this player this frame, or null when the vanilla model is drawn. */
    public static FormGeoModel activeModel(Player player) {
        FormDefinition form = FormManager.current(player);
        if (!form.hasFlag("special_form") && form.stage() <= 0) return null;
        FormGeoRenderer renderer = FormClientRenderEvents.rendererFor(form);
        if (renderer == null) return null;
        FormGeoAnimatable animatable = renderer.getAnimatable();
        if (animatable == null || !animatable.hasPreparedPose(player) || animatable.hasSafeRenderState()) return null;
        return renderer.getGeoModel() instanceof FormGeoModel geoModel ? geoModel : null;
    }

    /**
     * Port of Fabric's {@code modifyHeadPart} conversion: places {@code part} at the bone's
     * pivot with the bone's accumulated rotation (model-space chain only).
     */
    public static void applyTo(GeoBone bone, ModelPart part) {
        Matrix4f matrix = computeModelMatrix(bone).last().pose();
        Vector4f pivot = matrix.transform(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
        part.x = pivot.x;
        part.y = -pivot.y + 24.0F;
        part.z = -pivot.z;
        Vector3f euler = matrix.get3x3(new Matrix3f()).getEulerAnglesZYX(new Vector3f());
        part.xRot = euler.x();
        part.yRot = -euler.y();
        part.zRot = -euler.z();
    }

    /** Verbatim port of Fabric's {@code FormRenderUtils#computeModelMatrixStack}. */
    private static PoseStack computeModelMatrix(GeoBone bone) {
        PoseStack matrices = new PoseStack();
        List<GeoBone> chain = new ArrayList<>();
        for (GeoBone b = bone; b != null; b = b.getParent()) {
            chain.add(b);
        }
        Collections.reverse(chain);
        for (int i = 0; i < chain.size(); i++) {
            GeoBone b = chain.get(i);
            matrices.translate(-b.getPosX(), b.getPosY(), b.getPosZ());
            matrices.translate(b.getPivotX(), b.getPivotY(), b.getPivotZ());
            matrices.mulPose(Axis.ZP.rotation(b.getRotZ()));
            matrices.mulPose(Axis.YP.rotation(b.getRotY()));
            matrices.mulPose(Axis.XP.rotation(b.getRotX()));
            matrices.scale(b.getScaleX(), b.getScaleY(), b.getScaleZ());
            if (i < chain.size() - 1) {
                matrices.translate(-b.getPivotX(), -b.getPivotY(), -b.getPivotZ());
            }
        }
        return matrices;
    }
}
