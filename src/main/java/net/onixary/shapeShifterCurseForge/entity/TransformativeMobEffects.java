package net.onixary.shapeShifterCurseForge.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.power.TransformativeEffectService;

/** Shared attack hook for Fabric's transformative mob families. */
final class TransformativeMobEffects {
    private TransformativeMobEffects() { }

    static void tryApply(Entity target, float chance, String targetFormPath) {
        if (target instanceof ServerPlayer player && player.getRandom().nextFloat() < chance) {
            TransformativeEffectService.apply(player, ResourceLocation.fromNamespaceAndPath(
                    ShapeShifterCurseForge.RESOURCE_NAMESPACE, targetFormPath));
        }
    }
}
