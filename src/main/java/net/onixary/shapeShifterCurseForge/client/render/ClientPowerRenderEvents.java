package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

/** Client-only presentation powers that don't require renderer replacement or external libraries. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class ClientPowerRenderEvents {
    private ClientPowerRenderEvents() { }

    @SubscribeEvent
    public static void renderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        final boolean[] hide = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:no_render_arm".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                hide[0] = true;
            }
        });
        if (hide[0]) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
//
//        System.out.println("=== SSC CAMERA DEBUG ===");
//        System.out.println("camera = " + camera);
//        System.out.println("camera class = " + (camera == null ? null : camera.getClass()));
//        System.out.println("AbstractClientPlayer class = " + AbstractClientPlayer.class);
//        System.out.println("instanceof = " + (camera instanceof AbstractClientPlayer));
//        System.out.println("assignable = " + (
//                camera != null
//                        && AbstractClientPlayer.class.isAssignableFrom(camera.getClass())
//        ));
//
//        if (!(camera instanceof AbstractClientPlayer)) {
//            System.out.println("SSC CAMERA DEBUG: RETURN");
//            return;
//        }
//
//        System.out.println("SSC CAMERA DEBUG: PASSED");

        AbstractClientPlayer player = (AbstractClientPlayer) camera;
        final String[] type = {null};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:form_camera_bobbing".equals(FormPowerRegistry.typeOf(power))) {
                type[0] = FormPowerRuntime.stringValue(power, "bobbing_type", "default");
            }
        });
        if (type[0] == null || "none".equals(type[0])) return;
        float partialTick = (float) event.getPartialTick();
        float phase = (player.tickCount + partialTick) * (player.isSprinting() ? 0.75F : 0.45F);
        float swing = Mth.sin(phase) * Math.min(1.0F, player.walkAnimation.speed(partialTick));
        float roll = switch (type[0]) {
            case "feral" -> swing * 2.0F;
            case "bat" -> swing * 1.0F;
            case "float" -> swing * 0.35F;
            default -> swing * 1.5F;
        };
        event.setRoll(event.getRoll() + roll);
    }
}
