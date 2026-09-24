package net.onixary.shapeShifterCurseForge.mixin.accessor;

import net.minecraft.world.entity.animal.Ocelot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Ocelot.class)
public interface OcelotTrustingAccessor {
    @Invoker("isTrusting")
    boolean ssc$isTrusting();
}
