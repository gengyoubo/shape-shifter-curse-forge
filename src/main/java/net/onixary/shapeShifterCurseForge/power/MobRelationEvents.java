package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
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
                    || ("shape-shifter-curse:fox_friendly".equals(type) && mob instanceof net.minecraft.world.entity.animal.Fox);
            if (applies && FormPowerRuntime.test(player, mob, power.getAsJsonObject("condition"))) friendly[0] = true;
        });
        if (friendly[0]) event.setCanceled(true);
    }
}
