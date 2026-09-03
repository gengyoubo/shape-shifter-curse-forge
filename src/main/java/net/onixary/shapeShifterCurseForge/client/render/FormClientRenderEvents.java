package net.onixary.shapeShifterCurseForge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FormClientRenderEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, FormGeoRenderer> RENDERERS = new HashMap<>();
    private static final Set<UUID> REPORTED_RENDER_FAILURES = new HashSet<>();
    private static final float PLAYER_SCALE = 0.9375F;
    private static final float EYE_BED_OFFSET = 0.1F;

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
        renderer.setVanillaPlayerModel(event.getRenderer().getModel());
        // Both survival and creative inventories render their player preview full-bright.  This
        // is stable across the two screen classes, unlike instanceof InventoryScreen.
        boolean inventoryPreview = minecraft.screen != null && player == minecraft.player
                && event.getPackedLight() == LightTexture.FULL_BRIGHT;
        renderer.setInventoryPreview(inventoryPreview);
        renderer.prepareVanillaPlayerPose(event.getPartialTick());
        // The original player renderer is cancelled below. If data-driven animation
        // state is ever invalid, leave the event alone so the player remains visible.
        if (!renderer.getAnimatable().hasSafeRenderState()) {
            reportRenderFailure(player, form, null);
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        boolean rendered = false;
        poseStack.pushPose();
        try {
            // Forge posts RenderPlayerEvent.Pre before LivingEntityRenderer performs its entity
            // transforms.  Fabric's FormRenderFeature runs after them, so recreate the full
            // PlayerRenderer path before applying its own Geo coordinate conversion.
            applyVanillaPlayerTransforms(player, poseStack, event.getPartialTick());
            applyPlayerAnimationBodyTransform(renderer.getAnimatable().getBodyTransform(), poseStack);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.translate(0.0D, -1.51D, 0.0D);
            poseStack.translate(-0.5D, -0.5D, -0.5D);

            RenderType renderType = RenderType.entityTranslucent(renderer.getTextureLocation(renderer.getAnimatable()));
            renderer.render(poseStack, renderer.getAnimatable(), event.getMultiBufferSource(), renderType,
                    event.getMultiBufferSource().getBuffer(renderType), event.getPackedLight());
            rendered = true;
        } catch (RuntimeException exception) {
            // Do not strand the player invisible if a Gecko model or animation fails.
            // Returning without cancellation lets vanilla finish this render pass.
            reportRenderFailure(player, form, exception);
        } finally {
            poseStack.popPose();
        }
        if (rendered) {
            REPORTED_RENDER_FAILURES.remove(player.getUUID());
            event.setCanceled(true);
        }
    }

    private static void reportRenderFailure(Player player, FormDefinition form, RuntimeException exception) {
        if (!REPORTED_RENDER_FAILURES.add(player.getUUID())) {
            return;
        }
        if (exception == null) {
            LOGGER.warn("Skipping invalid form render state for {}; using the vanilla player model",
                    player.getGameProfile().getName());
        } else {
            LOGGER.error("Unable to render form {} for {}; using the vanilla player model until it recovers",
                    form.id(), player.getGameProfile().getName(), exception);
        }
    }

    private static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, path);
    }

    /** Mirrors Player Animation Lib's optional global {@code body} transform. */
    private static void applyPlayerAnimationBodyTransform(BedrockAnimationPlayer.BodyTransform transform,
                                                           PoseStack poseStack) {
        if (transform == null || transform.isIdentity()) {
            return;
        }
        poseStack.translate(transform.x(), transform.y() + 0.7F, transform.z());
        poseStack.mulPose(Axis.ZP.rotation(transform.roll()));
        poseStack.mulPose(Axis.YP.rotation(transform.yaw()));
        poseStack.mulPose(Axis.XP.rotation(transform.pitch()));
        poseStack.translate(0.0D, -0.7D, 0.0D);
    }

    /** Mirrors LivingEntityRenderer#render and PlayerRenderer#setupRotations for 1.20.1. */
    private static void applyVanillaPlayerTransforms(Player player, PoseStack poseStack, float partialTick) {
        boolean shouldSit = player.isPassenger() && player.getVehicle() != null && player.getVehicle().shouldRiderSit();
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        if (shouldSit && player.getVehicle() instanceof net.minecraft.world.entity.LivingEntity vehicle) {
            bodyYaw = Mth.rotLerp(partialTick, vehicle.yBodyRotO, vehicle.yBodyRot);
            float relativeHeadYaw = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -85.0F, 85.0F);
            bodyYaw = headYaw - relativeHeadYaw;
            if (relativeHeadYaw * relativeHeadYaw > 2500.0F) {
                bodyYaw += relativeHeadYaw * 0.2F;
            }
        }

        if (player.hasPose(Pose.SLEEPING)) {
            Direction direction = player.getBedOrientation();
            if (direction != null) {
                float bedOffset = player.getEyeHeight(Pose.STANDING) - EYE_BED_OFFSET;
                poseStack.translate(-direction.getStepX() * bedOffset, 0.0F, -direction.getStepZ() * bedOffset);
            }
        }

        applyPlayerRotations(player, poseStack, player.tickCount + partialTick, bodyYaw, partialTick);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
        poseStack.translate(0.0F, -1.501F, 0.0F);
    }

    /** Exact PlayerRenderer swimming/fall-flying branch around the base LivingEntity rotations. */
    private static void applyPlayerRotations(Player player, PoseStack poseStack,
                                             float animationProgress, float bodyYaw, float partialTick) {
        float swimAmount = player.getSwimAmount(partialTick);
        applyLivingRotations(player, poseStack, animationProgress, bodyYaw, partialTick);

        if (player.isFallFlying()) {
            float flightTicks = player.getFallFlyingTicks() + partialTick;
            float flightProgress = Mth.clamp(flightTicks * flightTicks / 100.0F, 0.0F, 1.0F);
            if (!player.isAutoSpinAttack()) {
                poseStack.mulPose(Axis.XP.rotationDegrees(flightProgress * (-90.0F - player.getXRot())));
            }

            Vec3 view = player.getViewVector(partialTick);
            // RenderPlayerEvent is typed as Player, although the client renderer always
            // supplies AbstractClientPlayer. Keep a harmless fallback for foreign callers.
            Vec3 velocity = player instanceof AbstractClientPlayer clientPlayer
                    ? clientPlayer.getDeltaMovementLerped(partialTick)
                    : player.getDeltaMovement();
            double velocityHorizontal = velocity.horizontalDistanceSqr();
            double viewHorizontal = view.horizontalDistanceSqr();
            if (velocityHorizontal > 0.0D && viewHorizontal > 0.0D) {
                double alignment = (velocity.x * view.x + velocity.z * view.z)
                        / Math.sqrt(velocityHorizontal * viewHorizontal);
                double cross = velocity.x * view.z - velocity.z * view.x;
                poseStack.mulPose(Axis.YP.rotation((float) (Math.signum(cross) * Math.acos(alignment))));
            }
        } else if (swimAmount > 0.0F) {
            boolean inSwimmableFluid = player.isInWater()
                    || player.isInFluidType((fluidType, height) -> player.canSwimInFluidType(fluidType));
            float targetPitch = inSwimmableFluid ? -90.0F - player.getXRot() : -90.0F;
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(swimAmount, 0.0F, targetPitch)));
            if (player.isVisuallySwimming()) {
                poseStack.translate(0.0F, -1.0F, 0.3F);
            }
        }
    }

    /** Mirrors LivingEntityRenderer#setupRotations, which PlayerRenderer invokes first. */
    private static void applyLivingRotations(Player player, PoseStack poseStack,
                                             float animationProgress, float bodyYaw, float partialTick) {
        if (player.isFullyFrozen()) {
            bodyYaw += (float) (Math.cos(player.tickCount * 3.25D) * Math.PI * 0.4F);
        }
        if (!player.hasPose(Pose.SLEEPING)) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        }

        if (player.deathTime > 0) {
            float deathProgress = ((player.deathTime + partialTick - 1.0F) / 20.0F) * 1.6F;
            deathProgress = Math.min(Mth.sqrt(deathProgress), 1.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(deathProgress * 90.0F));
        } else if (player.isAutoSpinAttack()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F - player.getXRot()));
            poseStack.mulPose(Axis.YP.rotationDegrees((player.tickCount + partialTick) * -75.0F));
        } else if (player.hasPose(Pose.SLEEPING)) {
            Direction direction = player.getBedOrientation();
            float sleepYaw = direction == null ? bodyYaw : sleepDirectionToRotation(direction);
            poseStack.mulPose(Axis.YP.rotationDegrees(sleepYaw));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
        } else if (LivingEntityRenderer.isEntityUpsideDown(player)) {
            poseStack.translate(0.0F, player.getBbHeight() + 0.1F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
    }

    private static float sleepDirectionToRotation(Direction direction) {
        return switch (direction) {
            case SOUTH -> 90.0F;
            case WEST -> 0.0F;
            case NORTH -> 270.0F;
            case EAST -> 180.0F;
            default -> 0.0F;
        };
    }
}
