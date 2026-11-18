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

    /** Whether this frame drew a four-legged Geo form for {@code player}. */
    public static boolean hasHindLegs(Player player) {
        return player == framePlayer
                && (FRAME_MATRICES.containsKey(LEFT_HIND_LEG) || FRAME_MATRICES.containsKey(RIGHT_HIND_LEG));
    }

    /**
     * The armor-leg frame for a captured leg bone, relative to {@code layerPose}, or null.
     *
     * <p>The visible leg is a group of cubes that GeckoLib draws with its own cube transform
     * {@code T(pivot)·Rz·Ry·Rx·T(-pivot)} on top of the bone matrix (Spider 3's legs are tilted
     * about 10° and 25° this way). The frame reproduces that transform, moves to the centre of
     * the group's cubes 12px above their lowest point, and turns from Geo axes (y up, +z front)
     * into vanilla part axes (y down, -z front), a 180° turn about X. A 4x12x4 armor leg drawn
     * at its origin with an identity {@code ModelPart} pose then covers the leg.</p>
     */
    public static Matrix4f legArmorFrame(Player player, String boneName, Matrix4f layerPose) {
        if (player != framePlayer) return null;
        Matrix4f boneMatrix = FRAME_MATRICES.get(boneName);
        GeoBone bone = FRAME_BONES.get(boneName);
        if (boneMatrix == null || bone == null) return null;
        LegFit fit = LEG_FITS.computeIfAbsent(bone, GeoBonePose::fitLeg);
        if (fit == null) return null;
        return new Matrix4f(layerPose).invert().mul(boneMatrix)
                .translate(fit.pivot.x() / 16.0F, fit.pivot.y() / 16.0F, fit.pivot.z() / 16.0F)
                .rotateZ(fit.rotation.z()).rotateY(fit.rotation.y()).rotateX(fit.rotation.x())
                .translate((fit.anchor.x() - fit.pivot.x()) / 16.0F, (fit.anchor.y() - fit.pivot.y()) / 16.0F,
                        (fit.anchor.z() - fit.pivot.z()) / 16.0F)
                .rotateX((float) Math.PI);
    }

    /** Multiplies a relative frame onto {@code poseStack}, keeping its normal matrix in step. */
    public static void mulFrame(PoseStack poseStack, Matrix4f frame) {
        PoseStack.Pose last = poseStack.last();
        last.pose().mul(frame);
        last.normal().mul(new Matrix3f(frame).normal());
    }

    /**
     * The leg body: the cubes sharing one cube pivot and rotation with the largest total volume
     * (Spider 3: upper leg, lower leg and knee). Claw cubes have their own rotation and are
     * left out, so they no longer pull the armor off the leg.
     *
     * @param pivot    cube pivot in absolute baked Geo pixels, as GeckoLib stores it
     * @param rotation cube rotation in radians, as GeckoLib stores it
     * @param anchor   armor pivot in absolute baked Geo pixels, in the group's unrotated space
     */
    private record LegFit(Vector3f pivot, Vector3f rotation, Vector3f anchor) { }

    private static final Map<GeoBone, LegFit> LEG_FITS = new WeakHashMap<>();

    private static LegFit fitLeg(GeoBone bone) {
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
        if (body == null) return null;
        float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (GeoCube cube : body) addBounds(cube, bounds);
        GeoCube first = body.get(0);
        return new LegFit(
                new Vector3f((float) first.pivot().x, (float) first.pivot().y, (float) first.pivot().z),
                new Vector3f((float) first.rotation().x, (float) first.rotation().y, (float) first.rotation().z),
                new Vector3f((bounds[0] + bounds[3]) * 0.5F, bounds[1] + ARMOR_LEG_LENGTH,
                        (bounds[2] + bounds[5]) * 0.5F));
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
