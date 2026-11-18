package net.onixary.shapeShifterCurseForge.mixin.integration;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "net.ltxprogrammer.changed.client.renderer.model.ExoskeletonModel", remap = false)
public interface ChangedExoskeletonModelAccessor {
    @Accessor("HeadSupport")
    ModelPart ssc$getHeadSupport();

    @Accessor("Torso")
    ModelPart ssc$getTorso();

    @Accessor("RightArm")
    ModelPart ssc$getRightArm();

    @Accessor("LeftArm")
    ModelPart ssc$getLeftArm();

    @Accessor("RightLeg")
    ModelPart ssc$getRightLeg();

    @Accessor("LeftLeg")
    ModelPart ssc$getLeftLeg();
}
