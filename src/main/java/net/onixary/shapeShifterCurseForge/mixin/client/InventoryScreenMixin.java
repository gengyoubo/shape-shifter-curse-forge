package net.onixary.shapeShifterCurseForge.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 物品栏预览用：绘制前把 {@code yBodyRotO} 临时对齐到 {@code yBodyRot}，
 * 使 SSC 的玩家/形态模型朝向与当前帧一致（无上一帧插值拖尾），
 * 绘制结束后再恢复原值，避免污染实体真实朝向。
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Unique
    private static LivingEntity ssc$entity;
    @Unique
    private static float ssc$prevBodyYaw;

    // 6 参主入口，所有重载最终都走这里
    @Inject(method = "renderEntityInInventory(Lnet/minecraft/client/gui/GuiGraphics;IIILorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"))
    private static void ssc$storePrevBodyYaw(GuiGraphics graphics, int x, int y, int scale,
                                              Quaternionf pose, Quaternionf cameraOrientation,
                                              LivingEntity entity, CallbackInfo ci) {
        if (entity == null) {
            return;
        }
        ssc$entity = entity;
        ssc$prevBodyYaw = entity.yBodyRotO;
        // 对齐到当前体转角，消除物品栏预览的插值偏移；如需固定朝向（如 180°），在此改为 entity.yBodyRot 即可
        entity.yBodyRotO = entity.yBodyRot;
    }

    @Inject(method = "renderEntityInInventory(Lnet/minecraft/client/gui/GuiGraphics;IIILorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("RETURN"))
    private static void ssc$restorePrevBodyYaw(GuiGraphics graphics, int x, int y, int scale,
                                               Quaternionf pose, Quaternionf cameraOrientation,
                                               LivingEntity entity, CallbackInfo ci) {
        if (ssc$entity != null) {
            ssc$entity.yBodyRotO = ssc$prevBodyYaw;
            ssc$entity = null;
        }
    }

    // FollowsMouse 重载也单独兜底（内部转调主入口，但 HEAD/RETURN 仍需各自恢复以防直接调用）
    @Inject(method = "renderEntityInInventoryFollowsMouse", at = @At("HEAD"))
    private static void ssc$storePrevBodyYawFollowsMouse(GuiGraphics graphics, int x, int y, int scale,
                                                          float angleXComponent, float angleYComponent,
                                                          LivingEntity entity, CallbackInfo ci) {
        if (entity == null) {
            return;
        }
        // 若主入口未先触发则在此兜底
        if (ssc$entity != entity) {
            ssc$entity = entity;
            ssc$prevBodyYaw = entity.yBodyRotO;
            entity.yBodyRotO = entity.yBodyRot;
        }
    }

    @Inject(method = "renderEntityInInventoryFollowsMouse", at = @At("RETURN"))
    private static void ssc$restorePrevBodyYawFollowsMouse(GuiGraphics graphics, int x, int y, int scale,
                                                            float angleXComponent, float angleYComponent,
                                                            LivingEntity entity, CallbackInfo ci) {
        if (ssc$entity == entity) {
            entity.yBodyRotO = ssc$prevBodyYaw;
            ssc$entity = null;
        }
    }
}
