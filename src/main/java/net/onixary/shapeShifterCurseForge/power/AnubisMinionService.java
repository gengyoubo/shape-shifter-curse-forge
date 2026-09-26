package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluids;
import net.onixary.shapeShifterCurseForge.entity.AnubisWolfMinionEntity;
import net.onixary.shapeShifterCurseForge.registry.ModEntities;

import java.util.UUID;

/** Fabric-style owner registry, cooldown, and dedicated Anubis wolf summons. */
public final class AnubisMinionService {
    private static final String MINIONS_KEY = "SscAnubisWolfMinions";
    private static final String COOLDOWN_KEY = "SscAnubisWolfMinionLastSummon";

    private AnubisMinionService() { }

    public static boolean isRegistered(Player player, UUID minionId) {
        return minions(player).contains(StringTag.valueOf(minionId.toString()));
    }

    public static void unregister(Player player, UUID minionId) {
        ListTag list = minions(player);
        list.remove(StringTag.valueOf(minionId.toString()));
        player.getPersistentData().put(MINIONS_KEY, list);
    }

    private static ListTag minions(Player player) {
        return player.getPersistentData().getList(MINIONS_KEY, Tag.TAG_STRING);
    }

    public static void summon(Player player, LivingEntity target, JsonObject action) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (FormPowerRuntime.booleanValue(action, "reverse", false)) {
            if (!(target instanceof Player reversedOwner)) return;
            target = player;
            player = reversedOwner;
        }
        int cooldown = FormPowerRuntime.intValue(action, "cooldown", 0);
        CompoundTag data = player.getPersistentData();
        long lastSummon = data.getLong(COOLDOWN_KEY);
        if (lastSummon > player.tickCount) {
            data.putLong(COOLDOWN_KEY, 0L);
            lastSummon = 0L;
        }
        if (cooldown > 0 && lastSummon != 0L && lastSummon + cooldown >= player.tickCount) return;

        ListTag registered = minions(player);
        registered.removeIf(entry -> {
            try {
                return level.getEntity(UUID.fromString(entry.getAsString())) == null;
            } catch (IllegalArgumentException ignored) {
                return true;
            }
        });
        data.put(MINIONS_KEY, registered);
        int maximum = FormPowerRuntime.intValue(action, "max_minion_count", Integer.MAX_VALUE);
        int count = Math.min(FormPowerRuntime.intValue(action, "count", 1), maximum - registered.size());
        if (count <= 0) return;
        boolean spawned = false;
        for (int index = 0; index < count; index++) {
            AnubisWolfMinionEntity wolf = ModEntities.ANUBIS_WOLF_MINION.get().create(level);
            if (wolf == null) continue;
            BlockPos pos = nearbyEmptySpace(level, target.blockPosition(), player);
            wolf.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot(), 0.0F);
            wolf.setTame(true);
            wolf.setOwnerUUID(player.getUUID());
            wolf.setOrderedToSit(false);
            wolf.setMinionLevel(FormPowerRuntime.intValue(action, "minion_level", 1));
            registered.add(StringTag.valueOf(wolf.getUUID().toString()));
            data.put(MINIONS_KEY, registered);
            if (level.addFreshEntity(wolf)) spawned = true;
            else registered.remove(StringTag.valueOf(wolf.getUUID().toString()));
        }
        if (!spawned) return;
        data.putLong(COOLDOWN_KEY, player.tickCount);
        FormPowerRuntime.execute(player, player, action.getAsJsonObject("owner_action"));
        FormPowerRuntime.execute(player, target, action.getAsJsonObject("target_action"));
        level.playSound(null, player.blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.PLAYERS, 1.0F, 1.5F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX() + 0.5D,
                player.getY() + 0.5D, player.getZ() + 0.5D, 8, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static BlockPos nearbyEmptySpace(ServerLevel level, BlockPos target, Player player) {
        for (int attempt = 0; attempt < 4; attempt++) {
            BlockPos pos = target.offset(player.getRandom().nextInt(7) - 3,
                    player.getRandom().nextInt(3) - 1, player.getRandom().nextInt(7) - 3);
            if (!isSpaceEmpty(level, pos)) pos = pos.above();
            else if (isSpaceEmpty(level, pos.below())) pos = pos.below();
            if (isSpaceEmpty(level, pos)) return pos;
        }
        return target;
    }

    private static boolean isSpaceEmpty(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() || level.getFluidState(pos).is(Fluids.WATER);
    }
}
