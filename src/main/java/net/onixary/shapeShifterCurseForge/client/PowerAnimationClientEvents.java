package net.onixary.shapeShifterCurseForge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.network.RequestPowerAnimationPacket;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class PowerAnimationClientEvents {
    private static final Set<Player> REQUESTED = Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        PowerAnimationClientHandler.ensureLevel(minecraft.level);
        if (minecraft.level == null || minecraft.getConnection() == null) {
            REQUESTED.clear();
            return;
        }
        for (Player player : minecraft.level.players()) {
            if (REQUESTED.add(player)) {
                ModNetwork.CHANNEL.sendToServer(new RequestPowerAnimationPacket(player.getId()));
            }
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide && event.getEntity() instanceof Player player) {
            REQUESTED.remove(player);
            PowerAnimationClientHandler.remove(player.getId());
        }
    }
}
