package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Pose clearance must use the same resized bounds as the player's live collision box. */
@Mixin(Entity.class)
public abstract class EntityPoseDimensionsMixin {
    @Shadow
    private EntityDimensions dimensions;

    @Shadow
    protected abstract float getEyeHeight(Pose pose, EntityDimensions dimensions);

    @SuppressWarnings("removal")
    @Redirect(method = "getBoundingBoxForPose", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;"))
    private EntityDimensions ssc$useResizedPoseDimensions(Entity entity, Pose pose) {
        EntityDimensions base = entity.getDimensions(pose);
        if (!(entity instanceof Player)) {
            return base;
        }
        // getDimensions() alone skips Forge's Size event, although refreshDimensions()
        // uses it. That mismatch forces even one-block forms into SWIMMING on land.
        // Start with the requested pose's pristine bounds, never the already scaled
        // live box, and honor the same SSC and external size handlers as a refresh.
        return ForgeEventFactory.getEntitySizeForge(entity, pose, dimensions, base,
                getEyeHeight(pose, base)).getNewSize();
    }
}
