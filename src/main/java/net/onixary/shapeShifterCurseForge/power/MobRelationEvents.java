package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

/** Prevents the native hostile AI from selecting forms it considers friendly. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class MobRelationEvents {
    private MobRelationEvents() { }

    @SubscribeEvent
    public static void changeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewTarget() instanceof Player player) || !(event.getEntity() instanceof Mob mob)) return;
        final boolean[] friendly = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            String type = FormPowerRegistry.typeOf(power);
            boolean applies = ("shape-shifter-curse:witch_friendly".equals(type) && mob instanceof Witch)
                    || ("shape-shifter-curse:pillager_friendly".equals(type) && mob instanceof Raider)
                    || ("shape-shifter-curse:fox_friendly".equals(type) && mob instanceof net.minecraft.world.entity.animal.Fox)
                    || ("shape-shifter-curse:t_wolf_friendly".equals(type) && mob instanceof Wolf);
            if ("apoli:simple".equals(type)) {
                applies = ("scare_creepers".equals(id.getPath()) && mob instanceof Creeper)
                        || ("scare_skeleton".equals(id.getPath()) && mob instanceof Skeleton)
                        || ("cat_friendly".equals(id.getPath()) && mob instanceof net.minecraft.world.entity.animal.Cat)
                        || ("spider_friendly".equals(id.getPath()) && mob instanceof net.minecraft.world.entity.monster.Spider);
            }
            if (applies && FormPowerRuntime.test(player, mob, power.getAsJsonObject("condition"))) friendly[0] = true;
        });
        if (friendly[0]) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void villagerFear(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || player.tickCount % 10 != 0 || !hasScareVillagerPower(player)) return;
        for (Villager villager : player.level().getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(8.0D), candidate -> candidate.isAlive())) {
            villager.getBrain().setMemoryWithExpiry(MemoryModuleType.NEAREST_HOSTILE, player, 20L);
        }
    }

    private static boolean hasScareVillagerPower(Player player) {
        final boolean[] result = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:scare_villager".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                result[0] = true;
            }
        });
        return result[0];
    }
}
