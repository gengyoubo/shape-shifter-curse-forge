package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class EntitySlipperinessMixin extends Entity {
    public EntitySlipperinessMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @ModifyVariable(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isOnGround()Z", opcode = Opcodes.GETFIELD, ordinal = 2))
    private float ssc$modifySlipperiness(float original) {
        if ((Object) this instanceof Player player) {
            final float[] modified = {original};
            final boolean[] applied = {false};
            FormPowerRegistry.visitActive(player, (id, power) -> {
                String type = FormPowerRegistry.typeOf(power);
                if ("apoli:modify_slipperiness".equals(type) && !applied[0]
                        && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                    modified[0] = original + FormPowerRuntime.floatValue(power, "slipperiness", 0.0F);
                    applied[0] = true;
                } else if ("shape-shifter-curse:conditioned_modify_slipperiness".equals(type) && !applied[0]) {
                    BlockPos pos = BlockPos.containing(this.getX(), this.getBoundingBox().minY - 1, this.getZ());
                    if (FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                        modified[0] = original + FormPowerRuntime.floatValue(power, "modifier", 0.0F);
                        applied[0] = true;
                    }
                }
            });
            return modified[0];
        }
        return original;
    }
}
