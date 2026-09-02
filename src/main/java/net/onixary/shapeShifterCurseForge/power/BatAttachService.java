package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Forge-native state for the tier-three bat wall/ceiling attachment power. */
public final class BatAttachService {
    private static final Map<UUID, Attachment> ATTACHMENTS = new HashMap<>();

    private BatAttachService() {
    }

    public static boolean toggleOrAttach(ServerPlayer player, BlockPos blockPos, Direction hitSide) {
        Attachment current = ATTACHMENTS.get(player.getUUID());
        if (current != null) {
            detach(player, false);
            return true;
        }
        if (player.onGround()) {
            return false;
        }

        JsonObject power = findPower(player);
        if (power == null || (power.has("attach_condition")
                && !FormPowerRuntime.test(player, player, power.getAsJsonObject("attach_condition")))) {
            return false;
        }
        if (player.level().getBlockState(blockPos).canBeReplaced()) {
            return false;
        }

        boolean bottom = hitSide == Direction.DOWN;
        if (bottom && power.has("enable_bottom_attach") && !power.get("enable_bottom_attach").getAsBoolean()) {
            return false;
        }
        if (!bottom && !hitSide.getAxis().isHorizontal()) {
            return false;
        }

        Attachment attachment = new Attachment(blockPos, hitSide, bottom,
                power.getAsJsonObject("side_attach_action"), power.getAsJsonObject("bottom_attach_action"),
                Math.max(1, FormPowerRuntime.intValue(power, "bottom_attach_interval", 20)), 0);
        ATTACHMENTS.put(player.getUUID(), attachment);
        lockToAttachment(player, attachment);
        return true;
    }

    public static void tick(Player player) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Attachment attachment = ATTACHMENTS.get(player.getUUID());
        if (attachment == null) {
            return;
        }
        if (player.level().getBlockState(attachment.pos()).isAir()) {
            detach(serverPlayer, false);
            return;
        }

        lockToAttachment(player, attachment);
        if (attachment.bottom() && attachment.bottomAction() != null) {
            int next = attachment.ticks() + 1;
            if (next >= attachment.interval()) {
                FormPowerRuntime.execute(player, player, attachment.bottomAction());
                next = 0;
            }
            ATTACHMENTS.put(player.getUUID(), attachment.withTicks(next));
        }
    }

    public static boolean detachForJump(Player player) {
        if (!ATTACHMENTS.containsKey(player.getUUID()) || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        detach(serverPlayer, true);
        return true;
    }

    public static void detach(ServerPlayer player, boolean jump) {
        Attachment attachment = ATTACHMENTS.remove(player.getUUID());
        if (attachment == null) {
            return;
        }
        if (!attachment.bottom() && attachment.sideAction() != null) {
            FormPowerRuntime.execute(player, player, attachment.sideAction());
        }
        player.setOnGround(false);
        player.setDeltaMovement(Vec3.ZERO);
        if (jump) {
            Vec3 forward = player.getLookAngle();
            Vec3 horizontal = new Vec3(forward.x, 0.0D, forward.z).normalize();
            player.push(horizontal.x * 1.25D, 0.8D, horizontal.z * 1.25D);
        } else {
            player.push(0.0D, 0.4D, 0.0D);
        }
        player.hurtMarked = true;
    }

    private static void lockToAttachment(Player player, Attachment attachment) {
        Vec3 center = Vec3.atCenterOf(attachment.pos());
        Vec3 target = attachment.bottom()
                ? center.add(0.0D, -1.5D, 0.0D)
                : center.add(attachment.side().getStepX(), -0.5D, attachment.side().getStepZ());
        player.setPos(target.x, target.y, target.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.setOnGround(true);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
    }

    private static JsonObject findPower(Player player) {
        final JsonObject[] found = {null};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (found[0] == null && "shape-shifter-curse:bat_block_attach".equals(FormPowerRegistry.typeOf(power))) {
                found[0] = power;
            }
        });
        return found[0];
    }

    private record Attachment(BlockPos pos, Direction side, boolean bottom, JsonObject sideAction,
                              JsonObject bottomAction, int interval, int ticks) {
        private Attachment withTicks(int ticks) {
            return new Attachment(pos, side, bottom, sideAction, bottomAction, interval, ticks);
        }
    }
}
