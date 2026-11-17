package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractClientPlayer.class)
public abstract class PlayerSpeedFovMixin {
    @Redirect(method = "getFieldOfViewModifier", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Abilities;getWalkingSpeed()F", ordinal = 0))
    private float ssc$limitMovementFov(Abilities abilities) {
        float walkingSpeed = abilities.getWalkingSpeed();
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (FormRegistry.ORIGINAL_BEFORE_ENABLE.equals(FormManager.current(player).id())) {
            return walkingSpeed;
        }

        // Fabric limits the denominator of the movement factor before bow and spyglass zoom.
        float movementSpeed = (float) player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        return Math.min(4.0F * movementSpeed, Math.max(0.9F * movementSpeed, walkingSpeed));
    }
}
