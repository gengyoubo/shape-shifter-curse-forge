package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormManager;

import java.util.Comparator;
import java.util.EnumSet;

/** Makes the vanilla species matching a form follow and approach its player. */
public final class FormAffinityGoal extends Goal {
    private static final double SEARCH_RADIUS = 32.0D;
    private static final double STOP_DISTANCE = 2.0D;
    private static final double MAX_DISTANCE = 40.0D;

    private final Mob mob;
    private Player target;
    private int pathRecalculationCooldown;

    public FormAffinityGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!canStart()) {
            target = null;
            return false;
        }

        target = mob.level().getEntitiesOfClass(Player.class,
                        mob.getBoundingBox().inflate(SEARCH_RADIUS), this::isValidTarget)
                .stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
        return target != null && !atFollowPoint(target);
    }

    @Override
    public boolean canContinueToUse() {
        return isValidTarget(target)
                && mob.distanceToSqr(target) <= MAX_DISTANCE * MAX_DISTANCE
                && !atFollowPoint(target);
    }

    @Override
    public void start() {
        pathRecalculationCooldown = 0;
    }

    @Override
    public void stop() {
        target = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (target == null || --pathRecalculationCooldown > 0) {
            return;
        }

        pathRecalculationCooldown = 10;
        FollowPoint followPoint = followPoint(target);
        if (mob.distanceToSqr(followPoint.x, followPoint.y, followPoint.z) <= 0.75D * 0.75D) {
            mob.getNavigation().stop();
            return;
        }

        // Use movement direction while walking, and facing direction while
        // standing still, so the mob queues up behind the player.
        mob.getNavigation().moveTo(followPoint.x, followPoint.y, followPoint.z, movementSpeed());
    }

    private boolean canStart() {
        return mob.isAlive() && !mob.isNoAi() && !mob.isPassenger() && mob.getTarget() == null;
    }

    private boolean isValidTarget(Player player) {
        return player != null
                && player.isAlive()
                && !player.isSpectator()
                && matchesSpecies(player, mob);
    }

    private float movementSpeed() {
        if (mob instanceof Axolotl) {
            return 0.75F;
        }
        if (mob instanceof Bat || mob instanceof Allay) {
            return 0.70F;
        }
        return 0.75F;
    }

    public static boolean supports(Mob mob) {
        return mob instanceof Axolotl
                || mob instanceof Bat
                || mob instanceof Cat
                || mob instanceof Fox
                || mob instanceof Ocelot
                || mob instanceof Wolf
                || mob instanceof Allay
                || mob instanceof Spider;
    }

    private FollowPoint followPoint(Player player) {
        var velocity = player.getDeltaMovement();
        double horizontalLength = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double directionX = horizontalLength > 1.0E-5D ? velocity.x / horizontalLength : 0.0D;
        double directionZ = horizontalLength > 1.0E-5D ? velocity.z / horizontalLength : 0.0D;
        if (horizontalLength <= 1.0E-5D) {
            var facing = player.getLookAngle();
            double facingLength = Math.sqrt(facing.x * facing.x + facing.z * facing.z);
            directionX = facingLength > 1.0E-5D ? facing.x / facingLength : 0.0D;
            directionZ = facingLength > 1.0E-5D ? facing.z / facingLength : 0.0D;
        }
        double followDistance = Math.max(STOP_DISTANCE, mob.getBbWidth() + 1.0D);
        return new FollowPoint(
                player.getX() - directionX * followDistance,
                player.getY(),
                player.getZ() - directionZ * followDistance);
    }

    private boolean atFollowPoint(Player player) {
        FollowPoint point = followPoint(player);
        return mob.distanceToSqr(point.x, point.y, point.z) <= 0.75D * 0.75D;
    }

    private record FollowPoint(double x, double y, double z) {
    }

    public static boolean matchesSpecies(Player player, Mob mob) {
        String group = FormManager.current(player).groupId().getPath();
        return switch (group) {
            case "axolotl_form" -> mob instanceof Axolotl;
            case "bat_form" -> mob instanceof Bat;
            case "ocelot_form", "feral_cat_form" -> mob instanceof Cat || mob instanceof Ocelot;
            case "familiar_fox_form", "snow_fox_form" -> mob instanceof Fox;
            case "anubis_wolf_form" -> mob instanceof Wolf;
            case "spider_form" -> mob instanceof Spider;
            case "allay_form" -> mob instanceof Allay;
            default -> false;
        };
    }
}
