package net.onixary.shapeShifterCurseForge.mixin;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies {@code apoli:modify_jump} to the return value of
 * {@link LivingEntity#getJumpPower()}, mirroring Apoli's
 * {@code @ModifyReturnValue} injection into Fabric's
 * {@code LivingEntity#getJumpVelocity()}.
 * <p>
 * Vanilla first calculates its normal jump power, including its own
 * jump-related modifiers. SSC then applies Apoli-compatible modifiers
 * to that final value before {@code jumpFromGround()} uses it.
 * <p>
 * Plain {@code @Inject(at = @At("RETURN"), cancellable = true)} is used
 * instead of MixinExtras {@code @ModifyReturnValue}.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getJumpPower", at = @At("RETURN"), cancellable = true)
    private void ssc$modifyJumpPower(CallbackInfoReturnable<Float> cir) {
        System.out.println("SSC getJumpPower mixin fired: " + cir.getReturnValue());
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }final float[] modified = {cir.getReturnValue()};

        System.out.println("before = " + modified[0]);
        System.out.println("form = " + FormManager.current(player).id());

        FormPowerRegistry.visitActive(player, (id, power) -> {
            String type = FormPowerRegistry.typeOf(power);

            if ("apoli:modify_jump".equals(type)) {
                System.out.println("FOUND MODIFY_JUMP: " + id);

                JsonObject condition = power.has("condition")
                        ? power.getAsJsonObject("condition")
                        : null;

                boolean result = FormPowerRuntime.test(player, player, condition);

                System.out.println("condition = " + result);

                if (!result) {
                    return;
                }

                if (power.has("modifier")) {
                    float beforeModifier = modified[0];

                    modified[0] = (float) FormPowerRuntime.applyModifier(
                            modified[0],
                            power.getAsJsonObject("modifier")
                    );

                    System.out.println(
                            "modify_jump: " + beforeModifier
                                    + " -> " + modified[0]
                    );
                }
            }
        });

        System.out.println("after = " + modified[0]);

        cir.setReturnValue(modified[0]);
    }
}
