package net.onixary.shapeShifterCurseForge.mixin.integration;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "net.ltxprogrammer.changed.client.renderer.model.ExoskeletonModel$VisorModel", remap = false)
public interface ChangedExoskeletonVisorAccessor {
    @Accessor("Head")
    ModelPart ssc$getVisorHead();
}
