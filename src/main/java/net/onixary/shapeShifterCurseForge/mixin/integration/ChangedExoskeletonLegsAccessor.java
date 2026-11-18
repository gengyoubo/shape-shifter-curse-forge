package net.onixary.shapeShifterCurseForge.mixin.integration;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "net.ltxprogrammer.changed.client.renderer.model.ExoskeletonModel$ReplacementLimbs", remap = false)
public interface ChangedExoskeletonLegsAccessor {
    @Accessor("Root")
    ModelPart ssc$getReplacementRoot();

    @Accessor("LeftLeg")
    ModelPart ssc$getReplacementLeftLeg();

    @Accessor("RightLeg")
    ModelPart ssc$getReplacementRightLeg();
}
