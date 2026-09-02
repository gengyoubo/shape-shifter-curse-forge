package net.onixary.shapeShifterCurseForge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FormClientRenderEvents {
    private static final Map<ResourceLocation, FormGeoRenderer> RENDERERS = new HashMap<>();

    private FormClientRenderEvents() {
    }

    @SubscribeEvent
    public static void renderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player.isSpectator() || player.isInvisible()) {
            return;
        }

        FormDefinition form = FormManager.current(player);
        if (!form.hasFlag("special_form") && form.tier() <= 0) {
            return;
        }

        ResourceLocation model = resource("geo/form/form_" + form.id().getPath() + ".geo.json");
        ResourceLocation texture = resource("textures/form/form_" + form.id().getPath()
                + "/form_" + form.id().getPath() + ".png");
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getResourceManager().getResource(model).isEmpty()
                || minecraft.getResourceManager().getResource(texture).isEmpty()) {
            return;
        }

        FormGeoRenderer renderer = RENDERERS.computeIfAbsent(form.id(), ignored -> new FormGeoRenderer(model, texture));
        renderer.setPlayer(player);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        // RenderPlayerEvent.Pre runs before LivingEntityRenderer applies the normal player
        // transforms.  Reproduce those transforms before the feature-style form transforms
        // used by the Fabric renderer, otherwise the Gecko model is vertically inverted.
        float bodyYaw = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0D, -1.501D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.translate(0.0D, -1.51D, 0.0D);
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        RenderType renderType = RenderType.entityTranslucent(renderer.getTextureLocation(renderer.getAnimatable()));
        renderer.render(poseStack, renderer.getAnimatable(), event.getMultiBufferSource(), renderType,
                event.getMultiBufferSource().getBuffer(renderType), event.getPackedLight());
        poseStack.popPose();
        event.setCanceled(true);
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(ShapeShifterCurseForge.RESOURCE_NAMESPACE, path);
    }
}
