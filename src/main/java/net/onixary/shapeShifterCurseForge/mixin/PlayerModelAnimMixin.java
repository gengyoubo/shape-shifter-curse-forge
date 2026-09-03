package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.client.render.FormClientRenderEvents;
import net.onixary.shapeShifterCurseForge.client.render.FormGeoAnimatable;
import net.onixary.shapeShifterCurseForge.client.render.FormGeoRenderer;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mirrors PAL's {@code PlayerModelMixin#setEmote} injection point (first
 * {@code ModelPart#copyFrom} in {@code setupAnim}): vanilla just finished the base
 * pose, and the accessory copies below propagate whatever is current. Re-applying the
 * form clip here keeps vanilla layers (held items, visible partial-form parts)
 * following the animation instead of the wiped vanilla pose.
 *
 * <p>Selection, fade and power-animation state are shared with the Pre pass, so both
 * passes of a frame apply bit-identical poses. Player models only exist on the
 * client, so this mixin never activates on a dedicated server.</p>
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelAnimMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;copyFrom(Lnet/minecraft/client/model/geom/ModelPart;)V",
                    ordinal = 0))
    private void ssc$reapplyFormClip(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                     float ageInTicks, float netHeadYaw, float headPitch,
                                     CallbackInfo ci) {
        if (!(entity instanceof Player player)) {
            return;
        }
        FormDefinition form = FormManager.current(player);
        if (!form.hasFlag("special_form") && form.tier() <= 0) {
            return;
        }
        FormGeoRenderer renderer = FormClientRenderEvents.rendererFor(form);
        if (renderer == null) {
            return;
        }
        // getAnimatable() is overridden to never go null (see FormGeoRenderer);
        // player association is handled explicitly inside reapplySelection.
        FormGeoAnimatable animatable = renderer.getAnimatable();
        if (animatable == null || animatable.isInventoryPreview()) {
            return;
        }
        animatable.reapplySelection(player, (PlayerModel<?>)(Object) this,
                Minecraft.getInstance().getFrameTime());
    }
}
