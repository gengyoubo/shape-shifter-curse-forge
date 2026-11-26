package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric {@code ElytraEntityModelMixin}: feral forms wear the wings folded along the back, and
 * bat_3 lowers the wing pivot while grounded to match its animations. Other entities keep
 * vanilla's {@code setupAnim}.
 */
@Mixin(ElytraModel.class)
public abstract class ElytraModelMixin<T extends LivingEntity> {
    @Shadow @Final private ModelPart rightWing;
    @Shadow @Final private ModelPart leftWing;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"), cancellable = true)
    private void ssc$formElytraAngles(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                      float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) return;
        FormDefinition form = FormManager.current(player);
        boolean feral = form.bodyType() == FormBodyType.FERAL;
        boolean bat3 = "bat_3".equals(form.id().getPath());
        if (!feral && !bat3) return;

        // Vanilla targets.
        float pitch = 0.2617994F;
        float roll = -0.2617994F;
        float pivotY = 0.0F;
        float yaw = 0.0F;
        if (player.isFallFlying()) {
            float openness = 1.0F;
            Vec3 velocity = player.getDeltaMovement();
            if (velocity.y < 0.0D) {
                openness = 1.0F - (float) Math.pow(-velocity.normalize().y, 1.5D);
            }
            pitch = openness * 0.34906584F + (1.0F - openness) * pitch;
            roll = openness * -1.5707964F + (1.0F - openness) * roll;
        } else if (player.isCrouching()) {
            pitch = 0.6981317F;
            roll = -0.7853982F;
            pivotY = 3.0F;
            yaw = 0.08726646F;
        }
        // Bat_3's wing pivot moves down to fit its grounded animations.
        if (bat3) {
            if (player.onGround()) {
                if (player.isCrouching()) {
                    pitch += (float) Math.toRadians(30.0D);
                    pivotY = 12.0F;
                } else {
                    pitch += (float) Math.toRadians(120.0D);
                    yaw += (float) Math.toRadians(45.0D);
                    pivotY = 9.0F;
                }
            } else {
                pivotY = 0.0F;
            }
        }

        leftWing.y = pivotY;
        player.elytraRotX += (pitch - player.elytraRotX) * 0.1F;
        player.elytraRotY += (yaw - player.elytraRotY) * 0.1F;
        player.elytraRotZ += (roll - player.elytraRotZ) * 0.1F;
        if (feral) {
            leftWing.xRot = pitch + (float) Math.toRadians(70.0D);
            leftWing.zRot = roll + (float) Math.toRadians(70.0D);
            leftWing.yRot = yaw + (float) Math.toRadians(45.0D);
        } else {
            leftWing.xRot = player.elytraRotX;
            leftWing.yRot = player.elytraRotY;
            leftWing.zRot = player.elytraRotZ;
        }
        rightWing.yRot = -leftWing.yRot;
        rightWing.y = leftWing.y;
        rightWing.xRot = leftWing.xRot;
        rightWing.zRot = -leftWing.zRot;
        ci.cancel();
    }
}
