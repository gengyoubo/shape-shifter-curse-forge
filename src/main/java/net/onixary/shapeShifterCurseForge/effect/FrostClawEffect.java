package net.onixary.shapeShifterCurseForge.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Frosted ice under the holder, including water crossed by a fast downward move. */
public final class FrostClawEffect extends MobEffect {
    public FrostClawEffect() { super(MobEffectCategory.BENEFICIAL, 12185087); }
    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return true; }
    @Override public void applyEffectTick(LivingEntity entity, int amplifier) { freeze(entity, entity.position()); }

    private static void freeze(LivingEntity entity, Vec3 center) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        var ice = Blocks.FROSTED_ICE.defaultBlockState();
        BlockPos origin = BlockPos.containing(center);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -1, -3), origin.offset(3, -1, 3))) {
            if (!pos.closerToCenterThan(center, 3.0) || !level.getBlockState(pos.above()).isAir()
                    || !level.getBlockState(pos).equals(IceBlock.meltsInto())
                    || !ice.canSurvive(level, pos) || !level.isUnobstructed(ice, pos, CollisionContext.empty())
                    || !level.getWorldBorder().isWithinBounds(pos)) continue;
            level.setBlockAndUpdate(pos, ice);
            level.scheduleTick(pos, Blocks.FROSTED_ICE, Mth.nextInt(entity.getRandom(), 60, 120));
        }
    }

    public static void beforeMove(LivingEntity entity, Vec3 movement) {
        if (entity.level().isClientSide) return;
        freeze(entity, entity.position());
        if (movement.y >= 0) return;
        var level = entity.level();
        Vec3 clipped = Entity.collideBoundingBox(entity, movement, entity.getBoundingBox(), level,
                level.getEntityCollisions(entity, entity.getBoundingBox().expandTowards(movement)));
        int steps = Math.min(64, Mth.ceil(clipped.length() * 2));
        for (int i = 1; i <= steps; i++) freeze(entity, entity.position().add(clipped.scale((double) i / steps)));
    }
}
