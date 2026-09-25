package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

@Mixin(targets = "net.minecraft.world.entity.animal.Ocelot$OcelotAvoidEntityGoal")
public abstract class OcelotFleeGoalMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/goal/AvoidEntityGoal;<init>(Lnet/minecraft/world/entity/PathfinderMob;Ljava/lang/Class;FDDLjava/util/function/Predicate;)V"), index = 5)
    private static Predicate<LivingEntity> ssc$catFriendly(Predicate<LivingEntity> original) {
        return original.and(entity -> !(entity instanceof Player player &&
                FormPowerRegistry.has(player, ResourceLocation.fromNamespaceAndPath("shape-shifter-curse", "cat_friendly"))));
    }
}
