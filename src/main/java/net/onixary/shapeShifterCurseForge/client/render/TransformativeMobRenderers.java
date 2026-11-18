package net.onixary.shapeShifterCurseForge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.AxolotlModel;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ambient.Bat;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.entity.TransformativeAxolotlEntity;
import net.onixary.shapeShifterCurseForge.entity.TransformativeOcelotEntity;
import net.onixary.shapeShifterCurseForge.entity.TransformativeSpiderEntity;

/**
 * Forge ports of Fabric's transformative mob renderers ({@code TAxolotlEntityRenderer},
 * {@code BatEntityRenderer}, {@code TOcelotEntityRenderer}, {@code TSpiderEntityRenderer}):
 * vanilla models with SSC's {@code textures/entity/mob/t_*.png}, Fabric's shadow radii and,
 * for the spider, half scale to match its half-size hitbox. The wolf keeps vanilla's renderer,
 * as on Fabric.
 */
public final class TransformativeMobRenderers {
    private TransformativeMobRenderers() {
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE,
                "textures/entity/mob/" + name + ".png");
    }

    public static final class Axolotl extends MobRenderer<TransformativeAxolotlEntity, AxolotlModel<TransformativeAxolotlEntity>> {
        private static final ResourceLocation TEXTURE = texture("t_axolotl");

        public Axolotl(EntityRendererProvider.Context context) {
            super(context, new AxolotlModel<>(context.bakeLayer(ModelLayers.AXOLOTL)), 0.5F);
        }

        @Override
        public ResourceLocation getTextureLocation(TransformativeAxolotlEntity entity) {
            return TEXTURE;
        }
    }

    /** Fabric's bat renderer repeats vanilla's 0.35 scale and resting/flying bob. */
    public static final class TransformativeBat extends BatRenderer {
        private static final ResourceLocation TEXTURE = texture("t_bat");

        public TransformativeBat(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(Bat entity) {
            return TEXTURE;
        }
    }

    public static final class Ocelot extends MobRenderer<TransformativeOcelotEntity, OcelotModel<TransformativeOcelotEntity>> {
        private static final ResourceLocation TEXTURE = texture("t_ocelot");

        public Ocelot(EntityRendererProvider.Context context) {
            super(context, new OcelotModel<>(context.bakeLayer(ModelLayers.OCELOT)), 0.4F);
        }

        @Override
        public ResourceLocation getTextureLocation(TransformativeOcelotEntity entity) {
            return TEXTURE;
        }
    }

    /** No glowing-eyes layer on Fabric; drawn at half size. */
    public static final class Spider extends MobRenderer<TransformativeSpiderEntity, SpiderModel<TransformativeSpiderEntity>> {
        private static final ResourceLocation TEXTURE = texture("t_spider");

        public Spider(EntityRendererProvider.Context context) {
            super(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)), 0.8F);
        }

        @Override
        protected void scale(TransformativeSpiderEntity entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }

        @Override
        public ResourceLocation getTextureLocation(TransformativeSpiderEntity entity) {
            return TEXTURE;
        }
    }
}
