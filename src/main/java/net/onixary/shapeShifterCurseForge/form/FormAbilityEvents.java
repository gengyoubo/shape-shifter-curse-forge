package net.onixary.shapeShifterCurseForge.form;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FormAbilityEvents {
    private static final int EFFECT_DURATION = 80;

    private FormAbilityEvents() {
    }

    @SubscribeEvent
    public static void resize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        FormDefinition form = FormManager.current(player);
        EntityDimensions original = event.getOriginalSize();
        event.setNewSize(EntityDimensions.scalable(
                original.width * form.widthScale(),
                original.height * form.heightScale()
        ));
        event.setNewEyeHeight(event.getNewEyeHeight() * form.eyeScale());
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        FormDefinition form = FormManager.current(player);
        if (!player.level().isClientSide && player.tickCount % 40 == 0) {
            applyEffect(player, form, "night_vision", MobEffects.NIGHT_VISION);
            applyEffect(player, form, "water_breathing", MobEffects.WATER_BREATHING);
        }

        Vec3 movement = player.getDeltaMovement();
        if (form.hasFlag("slow_fall") && !player.onGround() && movement.y < -0.08D) {
            player.setDeltaMovement(movement.x, -0.08D, movement.z);
            player.resetFallDistance();
        }
        if (form.hasFlag("climb") && player.horizontalCollision && !player.onGround()) {
            player.setDeltaMovement(movement.x, Math.max(movement.y, -0.15D), movement.z);
            player.resetFallDistance();
        }

        updateStepHeight(player, form);
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            FormDefinition form = FormManager.current(player);
            if (form.hasFlag("slow_fall")) {
                event.setDistance(0.0F);
                event.setDamageMultiplier(0.0F);
            } else if (form.fallProtectionDistance() > 0.0F) {
                event.setDistance(Math.max(0.0F, event.getDistance() - form.fallProtectionDistance()));
            }
        }
    }

    private static void applyEffect(Player player, FormDefinition form, String flag, MobEffect effect) {
        if (form.hasFlag(flag)) {
            player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, 0, true, false, false));
        }
    }

    @SubscribeEvent
    public static void jump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        float addition = FormManager.current(player).jumpVelocityAddition();
        if (addition > 0.0F) {
            Vec3 movement = player.getDeltaMovement();
            player.setDeltaMovement(movement.x, movement.y + addition, movement.z);
        }
    }

    @SubscribeEvent
    public static void effectMaintenance(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }

        if (FormManager.current(player).hasFlag("poison_immune")) {
            player.removeEffect(MobEffects.POISON);
        }
    }

    private static void updateStepHeight(Player player, FormDefinition form) {
        float multiplier = 1.0F;
        String path = form.id().getPath();
        if (path.equals("spider_3")) {
            multiplier = 2.0F;
        } else if (path.equals("ocelot_3") && player.isCrouching()) {
            multiplier = 2.0F;
        } else if (path.equals("axolotl_3") && player.isSprinting()) {
            multiplier = 2.0F;
        }

        float target = 0.6F * multiplier;
        if (Math.abs(player.maxUpStep() - target) > 0.001F) {
            player.setMaxUpStep(target);
        }
    }
}
