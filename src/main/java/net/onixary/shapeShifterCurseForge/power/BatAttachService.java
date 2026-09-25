package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.network.BatAttachStatePacket;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Forge-native state for the tier-three bat wall/ceiling attachment power. */
public final class BatAttachService {
    private static final Map<UUID, Attachment> ATTACHMENTS = new HashMap<>();
    // An integrated server shares a JVM with its client, so prediction needs a separate map.
    private static final Map<UUID, Attachment> CLIENT_ATTACHMENTS = new HashMap<>();
    private static Level clientLevel;

    private BatAttachService() {
    }

    public static boolean toggleOrAttach(ServerPlayer player, BlockPos blockPos, Direction hitSide) {
        Attachment current = ATTACHMENTS.get(player.getUUID());
        if (current != null) {
            detach(player, false);
            return true;
        }
        if (player.onGround() || hitSide == null) {
            return false;
        }

        JsonObject power = findPower(player);
        if (power == null || (power.has("attach_condition")
                && !FormPowerRuntime.test(player, player, power.getAsJsonObject("attach_condition")))) {
            return false;
        }
        if (power.has("block_condition") && !FormPowerRuntime.matchesBlockState(
                player.level(), blockPos, power.getAsJsonObject("block_condition"))) {
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
        initialAttachPosition(player, attachment);
        ModNetwork.sendBatAttachState(player, attachment.pos(), attachment.side());
        PowerAnimationService.playLoop(player, bottom
                ? PowerAnimationService.ATTACH_BOTTOM : PowerAnimationService.ATTACH_SIDE);
        return true;
    }

    public static void tick(Player player) {
        if (player.level().isClientSide) {
            Attachment clientAttachment = getAttachment(player);
            if (clientAttachment != null) lockToAttachment(player, clientAttachment);
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Attachment attachment = ATTACHMENTS.get(player.getUUID());
        if (attachment == null) {
            return;
        }
        if (!player.isAlive() || findPower(player) == null) {
            clear(serverPlayer);
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

    public static boolean isAttached(Player player) {
        return getAttachment(player) != null;
    }

    public static void applyClientState(UUID playerId, Level level, BlockPos pos, Direction side) {
        if (level == null || !level.isClientSide) return;
        if (clientLevel != level) {
            CLIENT_ATTACHMENTS.clear();
            clientLevel = level;
        }
        if (pos == null || side == null) {
            CLIENT_ATTACHMENTS.remove(playerId);
        } else {
            Attachment attachment = new Attachment(pos, side, side == Direction.DOWN, null, null, 20, 0);
            CLIENT_ATTACHMENTS.put(playerId, attachment);
            level.players().stream().filter(player -> player.getUUID().equals(playerId)).findFirst()
                    .ifPresent(player -> initialAttachPosition(player, attachment));
        }
    }

    public static void synchronizeTo(ServerPlayer target, ServerPlayer receiver) {
        Attachment attachment = ATTACHMENTS.get(target.getUUID());
        if (attachment != null) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver),
                    new BatAttachStatePacket(target.getUUID(), attachment.pos(), attachment.side()));
        }
    }

    public static void clear(ServerPlayer player) {
        if (ATTACHMENTS.remove(player.getUUID()) == null) return;
        PowerAnimationService.stopIfMatches(player, PowerAnimationService.ATTACH_SIDE,
                PowerAnimationService.ATTACH_BOTTOM);
        ModNetwork.sendBatAttachState(player, null, null);
    }

    public static void forget(ServerPlayer player) {
        ATTACHMENTS.remove(player.getUUID());
    }

    public static void detach(ServerPlayer player, boolean jump) {
        Attachment attachment = ATTACHMENTS.remove(player.getUUID());
        if (attachment == null) {
            return;
        }
        PowerAnimationService.stopIfMatches(player, PowerAnimationService.ATTACH_SIDE,
                PowerAnimationService.ATTACH_BOTTOM);
        ModNetwork.sendBatAttachState(player, null, null);
        if (!attachment.bottom() && attachment.sideAction() != null) {
            FormPowerRuntime.execute(player, player, attachment.sideAction());
        }
        player.setOnGround(false);
        player.setDeltaMovement(Vec3.ZERO);
        if (jump) {
            Vec3 forward = player.getLookAngle();
            double len2 = forward.x*forward.x + forward.z*forward.z;
            Vec3 horizontal = len2 < 1e-8 ? new Vec3(0,0,1) : new Vec3(forward.x, 0.0D, forward.z).normalize();
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
                : center.add(attachment.side().getStepX(), 0.0D, attachment.side().getStepZ());
        player.setPos(target.x, target.y, target.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.setOnGround(true);
        player.fallDistance = 0.0F;
        player.walkDist = 0.0F;
        player.moveDist = 0.0F;
        player.flyDist = 0.0F;
        player.xxa = 0.0F;
        player.yya = 0.0F;
        player.zza = 0.0F;
        if (!attachment.bottom()) {
            float yaw = switch (attachment.side()) {
                case NORTH -> 0.0F;
                case SOUTH -> 180.0F;
                case WEST -> -90.0F;
                case EAST -> 90.0F;
                default -> player.getYRot();
            };
            player.setYBodyRot(yaw);
            player.yBodyRotO = yaw;
        }
        player.hurtMarked = true;
    }

    private static void initialAttachPosition(Player player, Attachment attachment) {
        Vec3 center = Vec3.atCenterOf(attachment.pos());
        Vec3 target = attachment.bottom()
                ? center.add(0.0D, -1.5D, 0.0D)
                : center.add(attachment.side().getStepX() * 0.75D, -0.5D,
                        attachment.side().getStepZ() * 0.75D);
        player.setPos(target.x, target.y, target.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.setOnGround(true);
        player.hasImpulse = true;
        player.hurtMarked = true;
    }

    private static Attachment getAttachment(Player player) {
        if (!player.level().isClientSide) return ATTACHMENTS.get(player.getUUID());
        if (clientLevel != player.level()) {
            CLIENT_ATTACHMENTS.clear();
            clientLevel = player.level();
        }
        return CLIENT_ATTACHMENTS.get(player.getUUID());
    }

    private static JsonObject findPower(Player player) {
        final JsonObject[] found = {null};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (found[0] == null && "shape-shifter-curse:bat_block_attach".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
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
