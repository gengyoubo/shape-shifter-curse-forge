package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;

/** Forge-native fallback for the Fabric wolf-minion entity: tamed wolves with the same owner/target policy. */
public final class AnubisMinionService {
    private AnubisMinionService() { }

    public static void summon(Player player, LivingEntity target, JsonObject action) {
        if (!(player.level() instanceof ServerLevel level)) return;
        int maximum = FormPowerRuntime.intValue(action, "max_minion_count", Integer.MAX_VALUE);
        long existing = level.getEntitiesOfClass(Wolf.class, player.getBoundingBox().inflate(96.0D),
                wolf -> wolf.isTame() && player.getUUID().equals(wolf.getOwnerUUID())).size();
        int count = Math.max(1, Math.min(FormPowerRuntime.intValue(action, "count", 1), (int) Math.max(0, maximum - existing)));
        for (int index = 0; index < count; index++) {
            Wolf wolf = net.minecraft.world.entity.EntityType.WOLF.create(level);
            if (wolf == null) continue;
            wolf.moveTo(target.getX() + (player.getRandom().nextDouble() - 0.5D) * 3.0D,
                    target.getY(), target.getZ() + (player.getRandom().nextDouble() - 0.5D) * 3.0D,
                    player.getYRot(), 0.0F);
            wolf.setOwnerUUID(player.getUUID());
            wolf.setTame(true);
            wolf.setOrderedToSit(false);
            wolf.setTarget(target == player ? null : target);
            wolf.setHealth(wolf.getMaxHealth());
            level.addFreshEntity(wolf);
        }
    }
}
