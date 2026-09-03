package net.onixary.shapeShifterCurseForge.client.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(
        modid = "shape_shifter_curse",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class PlayerBoundsDebug {

    private static DebugSnapshot last;

    private static final double SUSPICIOUS_WIDTH = 0.1D;
    private static final double SUSPICIOUS_HEIGHT = 0.1D;

    private PlayerBoundsDebug() {
    }

    //@SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || mc.level == null) {
            last = null;
            return;
        }

        DebugSnapshot now = DebugSnapshot.capture(player);

        boolean first = last == null;

        boolean poseChanged =
                !first && now.pose != last.pose;

        boolean dimensionChanged =
                !first
                        && (Math.abs(now.bbWidth - last.bbWidth) > 0.00001F
                        || Math.abs(now.bbHeight - last.bbHeight) > 0.00001F);

        boolean boxSizeChanged =
                !first
                        && (Math.abs(now.boxWidth - last.boxWidth) > 0.00001D
                        || Math.abs(now.boxHeight - last.boxHeight) > 0.00001D
                        || Math.abs(now.boxDepth - last.boxDepth) > 0.00001D);

        boolean movementStateChanged =
                !first
                        && (now.swimming != last.swimming
                        || now.crouching != last.crouching
                        || now.shift != last.shift
                        || now.crawling != last.crawling
                        || now.inWater != last.inWater
                        || now.onGround != last.onGround
                        || now.fallFlying != last.fallFlying);

        boolean suspicious =
                now.bbWidth < SUSPICIOUS_WIDTH
                        || now.bbHeight < SUSPICIOUS_HEIGHT
                        || now.boxWidth < SUSPICIOUS_WIDTH
                        || now.boxHeight < SUSPICIOUS_HEIGHT
                        || now.boxDepth < SUSPICIOUS_WIDTH;

        if (first
                || poseChanged
                || dimensionChanged
                || boxSizeChanged
                || movementStateChanged
                || suspicious) {

            log(last, now, suspicious);
        }

        last = now;
    }

    private static void log(
            DebugSnapshot old,
            DebugSnapshot now,
            boolean suspicious
    ) {
        System.out.println();
        System.out.println(
                suspicious
                        ? "========== SSC BOUNDS SUSPICIOUS =========="
                        : "========== SSC BOUNDS CHANGE =========="
        );

        if (old != null) {
            System.out.printf(
                    Locale.ROOT,
                    "OLD pose=%s dimensions=%.6f x %.6f box=%.6f x %.6f x %.6f%n",
                    old.pose,
                    old.bbWidth,
                    old.bbHeight,
                    old.boxWidth,
                    old.boxHeight,
                    old.boxDepth
            );
        }

        System.out.printf(
                Locale.ROOT,
                "NEW pose=%s dimensions=%.6f x %.6f box=%.6f x %.6f x %.6f%n",
                now.pose,
                now.bbWidth,
                now.bbHeight,
                now.boxWidth,
                now.boxHeight,
                now.boxDepth
        );

        System.out.println(
                "state:"
                        + " swimming=" + now.swimming
                        + " crawling=" + now.crawling
                        + " crouching=" + now.crouching
                        + " shift=" + now.shift
                        + " fallFlying=" + now.fallFlying
                        + " inWater=" + now.inWater
                        + " onGround=" + now.onGround
        );

        System.out.printf(
                Locale.ROOT,
                "motion=(%.6f, %.6f, %.6f)%n",
                now.motionX,
                now.motionY,
                now.motionZ
        );

        System.out.println("form=" + now.form);

        System.out.println("==========================================");
    }

    private record DebugSnapshot(
            Pose pose,
            float bbWidth,
            float bbHeight,

            double boxWidth,
            double boxHeight,
            double boxDepth,

            boolean swimming,
            boolean crawling,
            boolean crouching,
            boolean shift,
            boolean fallFlying,
            boolean inWater,
            boolean onGround,

            double motionX,
            double motionY,
            double motionZ,

            String form
    ) {
        static DebugSnapshot capture(Player player) {
            AABB box = player.getBoundingBox();

            return new DebugSnapshot(
                    player.getPose(),
                    player.getBbWidth(),
                    player.getBbHeight(),

                    box.getXsize(),
                    box.getYsize(),
                    box.getZsize(),

                    player.isSwimming(),
                    player.isVisuallyCrawling(),
                    player.isCrouching(),
                    player.isShiftKeyDown(),
                    player.isFallFlying(),
                    player.isInWater(),
                    player.onGround(),

                    player.getDeltaMovement().x,
                    player.getDeltaMovement().y,
                    player.getDeltaMovement().z,

                    String.valueOf(FormManager.current(player).id())
            );
        }
    }
}