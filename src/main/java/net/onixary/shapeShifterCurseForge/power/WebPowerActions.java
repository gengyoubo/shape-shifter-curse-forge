package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.onixary.shapeShifterCurseForge.entity.WebBulletEntity;
import net.onixary.shapeShifterCurseForge.registry.ModBlocks;

/** Forge implementations of the spider-specific actions retained in the original power JSON. */
public final class WebPowerActions {
    private WebPowerActions() {
    }

    public static void fireBullet(Player player, JsonObject action) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        WebBulletEntity bullet = new WebBulletEntity(level, player,
                FormPowerRuntime.intValue(action, "tier", 1),
                !action.has("enable_top_block_build") || action.get("enable_top_block_build").getAsBoolean());
        bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                FormPowerRuntime.floatValue(action, "speed", 1.5F),
                FormPowerRuntime.floatValue(action, "divergence", 1.0F));
        level.addFreshEntity(bullet);
    }

    public static void buildBridge(Player player, JsonObject action) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos origin = player.blockPosition().below();
        if (player.isCrouching()) {
            origin = origin.above();
        }
        int length = Math.max(1, FormPowerRuntime.intValue(action, "web_bridge_length", 16));
        int width = Math.max(0, FormPowerRuntime.intValue(action, "web_bridge_width", 0));
        Direction direction = player.getDirection();
        Direction side = direction.getClockWise();
        for (int forward = 0; forward < length; forward++) {
            for (int sideways = -width; sideways <= width; sideways++) {
                place(level, origin.relative(direction, forward).relative(side, sideways), direction);
            }
        }
    }

    public static void buildLadder(ServerLevel level, BlockPos hit, Direction side, int tier, boolean buildTop) {
        int sideLength = switch (tier) { case 2 -> 14; case 3 -> 18; default -> 10; };
        int bottomLength = switch (tier) { case 2 -> 18; case 3 -> 24; default -> 14; };
        int topLength = buildTop ? switch (tier) { case 2 -> 12; case 3 -> 16; default -> 8; } : 0;
        if (side == Direction.UP) {
            buildLine(level, hit.above(), Direction.UP, topLength);
        } else if (side == Direction.DOWN) {
            buildLine(level, hit.below(), Direction.DOWN, bottomLength);
        } else {
            buildLine(level, hit.relative(side), Direction.DOWN, sideLength);
        }
    }

    private static void buildLine(ServerLevel level, BlockPos start, Direction direction, int length) {
        for (int index = 0; index < length; index++) {
            if (!place(level, start.relative(direction, index), Direction.NORTH)) {
                break;
            }
        }
    }

    private static boolean place(ServerLevel level, BlockPos pos, Direction facing) {
        BlockState current = level.getBlockState(pos);
        Block block = ModBlocks.TEMP_WEB_BRIDGE.get();
        if (!current.isAir() && !current.is(block)) {
            return false;
        }
        level.setBlock(pos, block.defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, facing), 3);
        return true;
    }
}
