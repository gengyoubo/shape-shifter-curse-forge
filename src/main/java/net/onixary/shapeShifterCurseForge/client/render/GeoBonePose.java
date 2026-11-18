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
    public static final String BODY = "bipedBody";
    public static final String LEFT_ARM = "bipedLeftArm";
    public static final String RIGHT_ARM = "bipedRightArm";
    private static final Set<String> TRACKED = Set.of(LEFT_LEG, RIGHT_LEG, LEFT_HIND_LEG, RIGHT_HIND_LEG,
            BODY, LEFT_ARM, RIGHT_ARM);

    /**
     * The vanilla box an equipment part was designed around, in vanilla part pixels relative
     * to the part pivot (y down, -z front). {@code bottom} aligns the box's lowest edge with
     * the form limb (legs, so feet meet the ground); otherwise the top edge (arms, torso).
     */
    public record PartBox(float x0, float x1, float y0, float y1, float z0, float z1, boolean bottom) {
        public static final PartBox LEG = new PartBox(-2, 2, 0, 12, -2, 2, true);
        public static final PartBox BODY = new PartBox(-4, 4, 0, 12, -2, 2, false);
        public static final PartBox RIGHT_ARM = new PartBox(-3, 1, -2, 10, -2, 2, false);
        public static final PartBox LEFT_ARM = new PartBox(-1, 3, -2, 10, -2, 2, false);
    }

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
        // Exactly the matrix GeckoLib draws this bone's cubes with (absolute Geo coordinates).
        FRAME_MATRICES.put(bone.getName(), new Matrix4f(poseStack.last().pose()));
        FRAME_BONES.put(bone.getName(), bone);
    }

    /** Whether the Geo form is being drawn for {@code player} in the current render. */
    public static boolean isFramePlayer(Player player) {
        return player != null && player == framePlayer;
    }

    /** Whether a Geo form is being drawn in the current player render (any player). */
    public static boolean hasFramePlayer() {
        return framePlayer != null;
    }

    /** Whether the Geo pass for {@code player} has drawn and captured its bones this frame. */
    public static boolean hasCapturedBones(Player player) {
        return player == framePlayer && !FRAME_MATRICES.isEmpty();
    }

    /** Whether this frame drew a four-legged Geo form for {@code player}. */
    public static boolean hasHindLegs(Player player) {
        return player == framePlayer
                && (FRAME_MATRICES.containsKey(LEFT_HIND_LEG) || FRAME_MATRICES.containsKey(RIGHT_HIND_LEG));
    }

    /** Armor legs: a 4x12x4 box whose bottom meets the form's foot. */
    public static Matrix4f legArmorFrame(Player player, String boneName, Matrix4f layerPose) {
        return partFrame(player, boneName, layerPose, PartBox.LEG);
    }

    /**
     * The frame, relative to {@code layerPose}, in which an equipment part drawn with an
     * identity {@code ModelPart} pose covers the captured bone; null when the bone wasn't drawn.
     *
     * <p>The visible limb is a group of cubes that GeckoLib draws with its own cube transform
     * {@code T(pivot)·Rz·Ry·Rx·T(-pivot)} on top of the bone matrix (Spider 3's legs are tilted
     * about 10° and 25° this way). The frame reproduces that transform, moves to the point
     * where {@code box} lines up with the group (centred in x/z, top or bottom edge aligned),
     * and turns from Geo axes (y up, +z front) into vanilla part axes (y down, -z front), a
     * 180° turn about X. A bone without cubes of its own keeps the vanilla mapping (its pivot).</p>
     */
    public static Matrix4f partFrame(Player player, String boneName, Matrix4f layerPose, PartBox box) {
        if (player != framePlayer) return null;
        Matrix4f boneMatrix = FRAME_MATRICES.get(boneName);
        GeoBone bone = FRAME_BONES.get(boneName);
        if (boneMatrix == null || bone == null) return null;
        Matrix4f frame = new Matrix4f(layerPose).invert().mul(boneMatrix);
        CubeGroup group = CUBE_GROUPS.computeIfAbsent(bone, GeoBonePose::mainCubeGroup);
        if (group == CubeGroup.NONE) {
            return frame.translate(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F)
                    .rotateX((float) Math.PI);
        }
        // Box edges in the Geo frame relative to its pivot are (x, -y, -z) of the vanilla box.
        float anchorX = (group.min.x() + group.max.x()) * 0.5F - (box.x0() + box.x1()) * 0.5F;
        float anchorY = box.bottom() ? group.min.y() + box.y1() : group.max.y() + box.y0();
        float anchorZ = (group.min.z() + group.max.z()) * 0.5F + (box.z0() + box.z1()) * 0.5F;
        return frame.translate(group.pivot.x() / 16.0F, group.pivot.y() / 16.0F, group.pivot.z() / 16.0F)
                .rotateZ(group.rotation.z()).rotateY(group.rotation.y()).rotateX(group.rotation.x())
                .translate((anchorX - group.pivot.x()) / 16.0F, (anchorY - group.pivot.y()) / 16.0F,
                        (anchorZ - group.pivot.z()) / 16.0F)
                .rotateX((float) Math.PI);
    }

    /** Multiplies a relative frame onto {@code poseStack}, keeping its normal matrix in step. */
    public static void mulFrame(PoseStack poseStack, Matrix4f frame) {
        PoseStack.Pose last = poseStack.last();
        last.pose().mul(frame);
        last.normal().mul(new Matrix3f(frame).normal());
    }

    /**
     * The limb body: the bone's cubes sharing one cube pivot and rotation with the largest
     * total volume (Spider 3's leg: upper leg, lower leg and knee). Claws and small details
     * with their own rotation are left out, so they don't pull equipment off the limb.
     *
     * @param pivot    cube pivot in absolute baked Geo pixels, as GeckoLib stores it
     * @param rotation cube rotation in radians, as GeckoLib stores it
     * @param min      lowest corner of the group's unrotated cubes, baked Geo pixels
     * @param max      highest corner of the group's unrotated cubes, baked Geo pixels
     */
    private record CubeGroup(Vector3f pivot, Vector3f rotation, Vector3f min, Vector3f max) {
        private static final CubeGroup NONE = new CubeGroup(new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f());
    }

    private static final Map<GeoBone, CubeGroup> CUBE_GROUPS = new WeakHashMap<>();

    private static CubeGroup mainCubeGroup(GeoBone bone) {
        Map<List<Double>, List<GeoCube>> groups = new HashMap<>();
        for (GeoCube cube : bone.getCubes()) {
            List<Double> key = List.of(cube.pivot().x, cube.pivot().y, cube.pivot().z,
                    cube.rotation().x, cube.rotation().y, cube.rotation().z);
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(cube);
        }
        List<GeoCube> body = null;
        double bodyVolume = -1.0D;
        for (List<GeoCube> group : groups.values()) {
            double volume = 0.0D;
            for (GeoCube cube : group) volume += cube.size().x * cube.size().y * cube.size().z;
            if (volume > bodyVolume) {
                bodyVolume = volume;
                body = group;
            }
        }
        if (body == null) return CubeGroup.NONE;
        float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (GeoCube cube : body) addBounds(cube, bounds);
        if (bounds[0] > bounds[3]) return CubeGroup.NONE;
        GeoCube first = body.get(0);
        return new CubeGroup(
                new Vector3f((float) first.pivot().x, (float) first.pivot().y, (float) first.pivot().z),
                new Vector3f((float) first.rotation().x, (float) first.rotation().y, (float) first.rotation().z),
                new Vector3f(bounds[0], bounds[1], bounds[2]), new Vector3f(bounds[3], bounds[4], bounds[5]));
    }

    /** Baked vertices are in blocks, unrotated, in the same (x-negated) Geo space as pivots. */
    private static void addBounds(GeoCube cube, float[] bounds) {
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
