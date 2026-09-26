package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.entity.AnubisWolfMinionEntity;

/** Uses the Fabric minion's 64x32 wolf geometry and texture. */
public final class AnubisWolfMinionRenderer extends MobRenderer<AnubisWolfMinionEntity,
        WolfModel<AnubisWolfMinionEntity>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "anubis_wolf_minion"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/entity/mob/anubis_wolf_minion.png");

    public AnubisWolfMinionRenderer(EntityRendererProvider.Context context) {
        super(context, new FabricWolfModel(context.bakeLayer(LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(AnubisWolfMinionEntity entity) {
        return TEXTURE;
    }

    @Override
    protected float getBob(AnubisWolfMinionEntity entity, float partialTick) {
        return entity.getTailAngle();
    }

    private static final class FabricWolfModel extends WolfModel<AnubisWolfMinionEntity> {
        private final ModelPart realHead;
        private final ModelPart realTail;

        private FabricWolfModel(ModelPart root) {
            super(root);
            realHead = root.getChild("head").getChild("real_head");
            realTail = root.getChild("tail").getChild("real_tail");
        }

        @Override
        public void prepareMobModel(AnubisWolfMinionEntity entity, float walkPosition,
                                    float walkSpeed, float partialTick) {
            super.prepareMobModel(entity, walkPosition, walkSpeed, partialTick);
            // These two shake rotations are disabled in Fabric's minion model.
            realHead.zRot = 0.0F;
            realTail.zRot = 0.0F;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(),
                PartPose.offset(-1.0F, 13.5F, -7.0F));
        head.addOrReplaceChild("real_head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -3.0F, -2.0F, 6.0F, 6.0F, 4.0F)
                .texOffs(16, 14).addBox(-3.4F, -5.0F, 0.0F, 2.0F, 3.0F, 1.0F)
                .texOffs(38, 14).addBox(-2.3F, -6.0F, -0.2F, 1.0F, 3.0F, 1.0F)
                .texOffs(16, 14).addBox(1.4F, -5.0F, 0.0F, 2.0F, 3.0F, 1.0F)
                .texOffs(38, 14).addBox(1.3F, -6.0F, -0.2F, 1.0F, 3.0F, 1.0F)
                .texOffs(0, 10).addBox(-1.5F, 0.9844F, -5.0F, 3.0F, 2.0F, 4.0F), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(18, 14)
                .addBox(-4.0F, -3.0F, -3.0F, 6.0F, 10.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 14.0F, 2.0F, 1.5708F, 0.0F, 0.0F));
        root.addOrReplaceChild("upper_body", CubeListBuilder.create().texOffs(21, 0)
                .addBox(-4.0F, -3.0F, -3.0F, 8.0F, 5.0F, 7.0F)
                .texOffs(43, 18).addBox(-1.0F, -5.3F, 2.2F, 2.0F, 10.0F, 2.0F),
                PartPose.offsetAndRotation(-1.0F, 14.0F, -3.0F, 1.5708F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(0, 18)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F)
                .texOffs(52, 18).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 4.0F, 1.0F),
                PartPose.offset(-2.5F, 16.0F, 7.0F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(0, 18)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F)
                .texOffs(52, 18).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 4.0F, 1.0F),
                PartPose.offset(0.5F, 16.0F, 7.0F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 18)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F)
                .texOffs(52, 6).addBox(-1.6F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F),
                PartPose.offset(-2.5F, 16.0F, -4.0F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(0, 18)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F)
                .texOffs(52, 6).addBox(0.6F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F),
                PartPose.offset(0.5F, 16.0F, -4.0F));
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create(),
                PartPose.offset(-1.0F, 12.0F, 8.0F));
        tail.addOrReplaceChild("real_tail", CubeListBuilder.create().texOffs(9, 18)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F)
                .texOffs(52, 24).addBox(-0.5F, 4.0F, 0.3F, 1.0F, 5.0F, 1.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }
}
