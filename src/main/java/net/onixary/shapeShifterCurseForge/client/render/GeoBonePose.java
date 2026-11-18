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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Binds vanilla equipment parts to Geo form bones. The Geo pre-pass has already posed this
 * player's bones when vanilla layers run, so a bone's current transform is the one drawn
 * this frame.
 */
public final class GeoBonePose {
    private GeoBonePose() {
    }

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
     * Port of Fabric's {@code modifyHeadPart} conversion, usable for any bone: places
     * {@code part} at the bone's pivot with the bone's accumulated rotation. For the biped
     * legs this is the exact inverse of {@code FormGeoModel#copyVanillaPosition/Rotation}.
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
