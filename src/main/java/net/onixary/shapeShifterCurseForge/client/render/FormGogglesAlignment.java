package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.MovementPowerService;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Translate the copied helmet in head-local coordinates, including when the head turns. */
public final class FormGogglesAlignment {
    private FormGogglesAlignment() { }

    public static void apply(HumanoidModel<?> model, Player player) {
        boolean sneaking = MovementPowerService.isSneakingOrForced(player);
        float down = sneaking ? 1.0F : 2.0F;
        float forward = sneaking ? -0.5F : -2.0F;
        move(model.head, down, forward);
        move(model.hat, down, forward);
    }

    private static void move(ModelPart part, float down, float forward) {
        var offset = new Vector3f(0.0F, down, forward);
        new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot).transform(offset);
        part.x += offset.x;
        part.y += offset.y;
        part.z += offset.z;
    }
}
