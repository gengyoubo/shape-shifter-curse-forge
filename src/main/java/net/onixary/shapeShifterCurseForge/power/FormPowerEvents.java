package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;
import net.onixary.shapeShifterCurseForge.advancement.SscAdvancementTriggers;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.power.LivingEntityJumpState;
import net.onixary.shapeShifterCurseForge.effect.TransformativeStatusEffect;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Server event bridge for the high-frequency Apoli power families used by the forms. */
@SuppressWarnings("deprecation")
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class FormPowerEvents {
    private FormPowerEvents() {
    }

    private static final Map<UUID, Map<ResourceLocation, Float>> FOOD_HEAL_REMAINDERS = new HashMap<>();
    /** Modifiers currently owned by the Forge-native Apoli compatibility layer, per player. */
    private static final Map<UUID, Map<UUID, AttributeInstance>> OWNED_ATTRIBUTE_MODIFIERS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_ATTRIBUTE_REFRESH_TICK = new HashMap<>();
    /** Last Axolotl III movement snapshot written to the server log, per player. */
    private static final Map<UUID, String> LAST_AXOLOTL_MOVE_DEBUG = new HashMap<>();
    /** Per-power transition state matching SSC Fabric's DelayAttributePower. */
    private static final Map<UUID, Map<UUID, DelayAttributeState>> DELAY_ATTRIBUTE_STATES = new HashMap<>();
    /** Cached condition results for apoli:conditioned_attribute tick_rate semantics. */
    private static final Map<UUID, Map<UUID, ConditionedAttributeState>> CONDITIONED_ATTRIBUTE_STATES = new HashMap<>();
    /** Whether an owned modifier must preserve the player's health percentage on changes. */
    private static final Map<UUID, Map<UUID, Boolean>> UPDATE_HEALTH_MODIFIERS = new HashMap<>();
    private static final Map<UUID, Map<UUID, Integer>> DAMAGE_OVER_TIME_STARTED_TICKS = new HashMap<>();
    private static final ThreadLocal<Boolean> SWEEP_DAMAGE = ThreadLocal.withInitial(() -> false);
    private static final ResourceLocation LEGACY_WATER_SPEED = ResourceLocation.fromNamespaceAndPath(
            "additionalentityattributes", "generic.water_speed");

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        // Fabric's UpdateAir mixin runs on both sides.  Mirror its air value on the
        // client as well, otherwise the vanilla client prediction refills the HUD
        // until the next server entity-data sync.
        if (player.level().isClientSide) {
            // Fabric's AttributePower and ConditionedAttributePower also update on
            // the logical client.  The server's attribute value was already right,
            // but omitting this left the owning client predicting vanilla movement
            // until a correction arrived.
            refreshAttributes(player);
            logAxolotlMovementState(player);
            enforceSprinting(player);
            applyClimbing(player);
            tickCustomWaterBreathing(player);
            return;
        }

        refreshAttributes(player);
        logAxolotlMovementState(player);
        FormActivePowerService.tick(player);
        InstinctService.tick((net.minecraft.server.level.ServerPlayer) player);
        BatAttachService.tick(player);
        PowerAnimationService.tick((net.minecraft.server.level.ServerPlayer) player);
        MovementPowerService.tick(player);
        enforceSprinting(player);
        adjustFoodHealTimer(player);
        SscJavaRegistries.tickActivePowers(player);
        FormPowerRegistry.visitActive(player, (id, power) -> tickPower(player, id, power));
        applyClimbing(player);
        maintainBreathingAndImmunity(player);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        FormActivePowerService.postTravelTick(player);
        final float[] multiplier = {1.0F};
        final boolean[] modified = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:modify_footstep_sound_speed".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            boolean sprintOverride = power.has("adjust_run_individually")
                    && power.get("adjust_run_individually").getAsBoolean() && player.isSprinting();
            float value = FormPowerRuntime.floatValue(power,
                    sprintOverride ? "run_speed_multiplier" : "speed_multiplier", 1.0F);
            if (value > 0.0F) {
                multiplier[0] = value;
                modified[0] = true;
            }
        });
        if (modified[0]) player.nextStep = player.moveDist + 1.0F / multiplier[0];
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity().level().isClientSide
                || !(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer receiver)
                || !(event.getTarget() instanceof net.minecraft.server.level.ServerPlayer target)) {
            return;
        }
        PowerAnimationService.synchronizeTo(target, receiver);
    }

    @SubscribeEvent
    public static void potionEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("apoli:effect_immunity".equals(FormPowerRegistry.typeOf(power))
                    && effectIsListed(event.getEffectInstance().getEffect(), power)) {
                event.setCanceled(true);
                return;
            }
            if (!(event.getEffectSource() instanceof net.minecraft.world.entity.projectile.ThrownPotion
                    || event.getEffectSource() instanceof net.minecraft.world.entity.AreaEffectCloud)) return;
            if ("shape-shifter-curse:action_on_splash_potion_take_effect".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("entity_condition"))) {
                FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
            }
        });
        // Effect immunity must win.  Only a potion that actually remains on the
        // player may create a pending transformative effect.
        if (!event.isCanceled()
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && event.getEffectInstance().getEffect() instanceof TransformativeStatusEffect transformative) {
            transformative.queue(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void waterPotionImpact(ProjectileImpactEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.projectile.ThrownPotion potion)
                || potion.level().isClientSide
                || net.minecraft.world.item.alchemy.PotionUtils.getPotion(potion.getItem())
                != net.minecraft.world.item.alchemy.Potions.WATER) return;
        for (Player player : potion.level().getEntitiesOfClass(Player.class,
                potion.getBoundingBox().inflate(4.0D), LivingEntity::isAlive)) {
            FormPowerRegistry.visitActive(player, (id, power) -> {
                if ("shape-shifter-curse:action_on_splash_potion_take_effect".equals(FormPowerRegistry.typeOf(power))
                        && power.has("trigger_on_no_effect") && power.get("trigger_on_no_effect").getAsBoolean()
                        && FormPowerRuntime.test(player, player, power.getAsJsonObject("entity_condition"))) {
                    FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
                }
            });
        }
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
        Entity directEntity = event.getSource().getDirectEntity();
        if (event.getEntity() instanceof Player defender) {
            FormPowerRegistry.visitActive(defender, (id, power) -> {
                String type = FormPowerRegistry.typeOf(power);
                if (("apoli:fire_immunity".equals(type) && event.getSource().is(DamageTypeTags.IS_FIRE))
                        || ("apoli:invulnerability".equals(type)
                        && FormPowerRuntime.matchesDamageSource(event.getSource(), power.getAsJsonObject("damage_condition")))) {
                    event.setCanceled(true);
                    return;
                }
                if ("shape-shifter-curse:virtual_shield".equals(type)
                        && blocksWithVirtualShield(defender, event, power)) {
                    event.setCanceled(true);
                }
                if ("apoli:modify_damage_taken".equals(type)
                        && FormPowerRuntime.test(defender, attacker, power.getAsJsonObject("condition"))) {
                    event.setAmount((float) FormPowerRuntime.applyModifier(event.getAmount(), power.getAsJsonObject("modifier")));
                }
                if ("apoli:self_action_when_hit".equals(type)
                        && FormPowerRuntime.testDamageCondition(defender, attacker, event.getSource(), event.getAmount(),
                        power.getAsJsonObject("damage_condition"))) {
                    FormPowerRuntime.execute(defender, defender, power.getAsJsonObject("entity_action"));
                }
                if ("apoli:action_when_hit".equals(type)
                        && attacker != null && FormPowerRuntime.testDamageCondition(defender, attacker, event.getSource(),
                        event.getAmount(), power.getAsJsonObject("damage_condition"))) {
                    FormPowerRuntime.execute(defender, attacker, power.getAsJsonObject("entity_action"));
                }
                if ("shape-shifter-curse:burn_damage_modifier".equals(type)
                        && event.getSource().is(DamageTypeTags.IS_FIRE)
                        && defender.isOnFire()
                        && !defender.hasEffect(MobEffects.FIRE_RESISTANCE)
                        && FormPowerRuntime.test(defender, attacker, power.getAsJsonObject("condition"))) {
                    event.setAmount(event.getAmount() + FormPowerRuntime.floatValue(power, "modifier", 0.0F));
                    FormPowerRuntime.execute(defender, defender, power.getAsJsonObject("action"));
                }
                if ("shape-shifter-curse:modify_instant_damage_scale".equals(type)
                        && isInstantMagic(event.getSource())) {
                    event.setAmount(event.getAmount() * FormPowerRuntime.floatValue(power, "scale", 1.0F));
                }
            });
        }

        if (attacker instanceof Player player) {
            FormPowerRegistry.visitActive(player, (id, power) -> {
                String type = FormPowerRegistry.typeOf(power);
                if ("apoli:modify_damage_dealt".equals(type)
                        && FormPowerRuntime.test(player, event.getEntity(), power.getAsJsonObject("condition"))) {
                    event.setAmount((float) FormPowerRuntime.applyModifier(event.getAmount(), power.getAsJsonObject("modifier")));
                }
                if ("apoli:self_action_on_hit".equals(type)
                        && FormPowerRuntime.testDamageCondition(player, event.getEntity(), event.getSource(), event.getAmount(),
                        power.getAsJsonObject("damage_condition"))) {
                    FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
                }
                if ("apoli:action_on_hit".equals(type)
                        && FormPowerRuntime.testDamageCondition(player, event.getEntity(), event.getSource(), event.getAmount(),
                        power.getAsJsonObject("damage_condition"))) {
                    FormPowerRuntime.execute(player, event.getEntity(), power.getAsJsonObject("entity_action"));
                }
                if ("shape-shifter-curse:enhanced_falling_attack".equals(type) && player.fallDistance > 0.0F) {
                    FormPowerRuntime.execute(player, event.getEntity(), power.getAsJsonObject("target_action_on_critical_hit"));
                    FormPowerRuntime.execute(player, player, power.getAsJsonObject("self_action_on_critical_hit"));
                }
            });
        }

        if (directEntity instanceof Projectile projectile && projectile.getOwner() instanceof Player player) {
            FormPowerRegistry.visitActive(player, (id, power) -> {
                if (!"apoli:modify_projectile_damage".equals(FormPowerRegistry.typeOf(power))
                        || !FormPowerRuntime.test(player, event.getEntity(), power.getAsJsonObject("condition"))
                        || !matchesProjectile(projectile, power.getAsJsonObject("damage_condition"))) return;
                event.setAmount((float) FormPowerRuntime.applyModifier(event.getAmount(), power.getAsJsonObject("modifier")));
                FormPowerRuntime.execute(player, event.getEntity(), power.getAsJsonObject("target_action"));
                FormPowerRuntime.execute(player, player, power.getAsJsonObject("self_action"));
            });
        }
    }

    @SubscribeEvent
    public static void trackMobAttacks(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide
                || !(event.getSource().getEntity() instanceof Player player)) return;
        FormPowerRuntime.recordPlayerAttack(player, event.getEntity());
    }

    @SubscribeEvent
    public static void attack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || !"sweetBerryBush".equals(event.getSource().getMsgId())) return;
        final boolean[] immune = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:prevent_berry_effect".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                immune[0] = true;
            }
        });
        if (immune[0]) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void heal(LivingHealEvent event) {
        // TODO[PARITY] Fabric scales instant health at the potion mixin; this LivingHealEvent
        //   approximation also catches any non-potion heal above 1.0 that reaches this path.
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || event.getAmount() <= 1.0F) return;
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:modify_instant_health_scale".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            event.setAmount(event.getAmount() * FormPowerRuntime.floatValue(power, "scale", 1.0F));
        });
    }

    @SubscribeEvent
    public static void criticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !event.isVanillaCritical()) return;
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:critical_damage_modifier".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, event.getTarget(), power.getAsJsonObject("condition"))) return;
            event.setDamageModifier(event.getDamageModifier()
                    * FormPowerRuntime.floatValue(power, "multiplier", 1.0F));
            FormPowerRuntime.execute(player, player, power.getAsJsonObject("action"));
        });
    }

    @SubscribeEvent
    public static void sweepingHit(LivingHurtEvent event) {
        if (Boolean.TRUE.equals(SWEEP_DAMAGE.get())
                || !(event.getSource().getEntity() instanceof Player player)
                || event.getEntity() == player || !"player".equals(event.getSource().getMsgId())
                || player.level().isClientSide) return;
        final boolean[] enabled = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:always_sweeping".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, event.getEntity(), power.getAsJsonObject("condition"))) {
                enabled[0] = true;
            }
        });
        if (!enabled[0]) return;

        SWEEP_DAMAGE.set(true);
        try {
            float sweepDamage = Math.max(1.0F, event.getAmount() * 0.2F);
            for (LivingEntity nearby : player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(1.0D), candidate -> candidate != player
                            && candidate != event.getEntity() && candidate.isAlive()
                            && !player.isAlliedTo(candidate))) {
                nearby.hurt(player.damageSources().playerAttack(player), sweepDamage);
            }
        } finally {
            SWEEP_DAMAGE.set(false);
        }
    }

    @SubscribeEvent
    public static void jump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!player.level().isClientSide) {
            if (BatAttachService.detachForJump(player)) {
                return;
            }
            FormActivePowerService.registerGroundJump(player);
            FormActivePowerService.triggerVanillaKey(player, "key.jump");
        }

        // Jump height itself is handled by LivingEntityMixin#getJumpPower,
        // which applies apoli:modify_jump inside vanilla's calculation.
        // This handler only runs the jump-triggered actions (particles, sounds,
        // forward dash, entity_action of modify_jump powers).
        List<JsonObject> pendingActions = new ArrayList<>();

        FormPowerRegistry.visitActive(player, (id, power) -> {
            String type = FormPowerRegistry.typeOf(power);
            JsonObject condition = power.has("entity_condition")
                    ? power.getAsJsonObject("entity_condition") : power.getAsJsonObject("condition");
            if ("shape-shifter-curse:action_on_jump".equals(type)
                    && testJumpCondition(player, condition)) {
                pendingActions.add(power.getAsJsonObject("entity_action"));
            }
            if ("apoli:modify_jump".equals(type)
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))
                    && power.has("entity_action")) {
                pendingActions.add(power.getAsJsonObject("entity_action"));
            }
        });

        Vec3 velocityBeforeActions = player.getDeltaMovement();
        for (JsonObject action : pendingActions) {
            FormPowerRuntime.execute(player, player, action);
        }
        // Fabric executes ActionOnJumpPower on both logical sides, so the local
        // player immediately sees its forward boost. Forge's Entity#push only
        // marks hasImpulse, whose tracker broadcast excludes the owning player;
        // hurtMarked uses the matching broadcast-and-send path for that owner.
        if (!player.level().isClientSide
                && !player.getDeltaMovement().equals(velocityBeforeActions)) {
            player.hurtMarked = true;
        }
        if (player instanceof LivingEntityJumpState jumpState) {
            jumpState.ssc$clearJumpStartedOnBlock();
        }
    }

    private static boolean testJumpCondition(Player player, JsonObject condition) {
        if (condition == null) return true;
        String type = FormPowerRegistry.typeOf(condition);
        boolean result;
        if ("apoli:and".equals(type)) {
            result = true;
            for (var child : condition.getAsJsonArray("conditions")) {
                if (child.isJsonObject() && !testJumpCondition(player, child.getAsJsonObject())) {
                    result = false;
                    break;
                }
            }
        } else if ("apoli:or".equals(type)) {
            result = false;
            for (var child : condition.getAsJsonArray("conditions")) {
                if (child.isJsonObject() && testJumpCondition(player, child.getAsJsonObject())) {
                    result = true;
                    break;
                }
            }
        } else if ("apoli:on_block".equals(type) && !condition.has("block_condition")
                && player instanceof LivingEntityJumpState jumpState) {
            result = jumpState.ssc$wasJumpStartedOnBlock();
        } else {
            result = FormPowerRuntime.test(player, player, condition);
        }
        return (condition.has("inverted") && condition.get("inverted").getAsBoolean()) != result;
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        final float[] distance = {event.getDistance()};
        final float[] multiplier = {event.getDamageMultiplier()};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            String type = FormPowerRegistry.typeOf(power);
            if ("shape-shifter-curse:bypass_landing_effect".equals(type)
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                distance[0] = 0.0F;
                multiplier[0] = 0.0F;
            }
            if ("shape-shifter-curse:falling_protection".equals(type)
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                distance[0] = Math.max(0.0F, distance[0] - FormPowerRuntime.floatValue(power, "fall_distance", 0.0F));
            }
            if ("apoli:modify_falling".equals(type)
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))
                    && power.has("take_fall_damage") && !power.get("take_fall_damage").getAsBoolean()) {
                multiplier[0] = 0.0F;
            }
            if (!"shape-shifter-curse:modfiy_fall_damage".equals(type)) return;
            distance[0] = (float) applyFallModifiers(distance[0], power, "modifier_fall_distance", "modifiers_fall_distance");
            multiplier[0] = (float) applyFallModifiers(multiplier[0], power, "modifier_damage_multiplier", "modifiers_damage_multiplier");
        });
        event.setDistance(Math.max(0.0F, distance[0]));
        event.setDamageMultiplier(Math.max(0.0F, multiplier[0]));
    }

    @SubscribeEvent
    public static void useItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getEntity().level().isClientSide) {
            if (handleCustomEdible(event.getEntity())) {
                event.setCanceled(true);
                return;
            }
            if (preventsItemUse(event.getEntity())) {
                event.setCanceled(true);
                return;
            }
            runItemUseInteraction(event.getEntity(), event.getItemStack(), false);
        }
    }

    @SubscribeEvent
    public static void finishUsingItem(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        ItemStack used = event.getItem();
        // Apoli action_on_item_use with trigger=finish fires after the use duration completes.
        runItemUseInteraction(player, used, true);
        if (used.is(Items.GOLDEN_APPLE) || used.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                FormDefinition current = FormManager.current(player);
                if (!current.hasFlag("no_instinct") && !current.hasFlag("lock_instinct")) {
                    SscAdvancementTriggers.ON_USE_GOLDEN_APPLE.trigger(serverPlayer);
                }
                TransformativeEffectService.clear(serverPlayer);
            }
        }
        FoodProperties food = used.getFoodProperties(player);
        if (food == null) return;
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:modify_food".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.matchesItem(used, power.getAsJsonObject("item_condition"))) return;
            // Vanilla applies the food's status effects inside the Eat event, so "prevent_effects"
            // removes them again here.
            if (FormPowerRuntime.booleanValue(power, "prevent_effects", false)) {
                for (var effectPair : food.getEffects()) {
                    player.removeEffect(effectPair.getFirst().getEffect());
                }
            }
            int before = food.getNutrition();
            float nutrition = applyFoodModifiers(before, player, power, "food_modifier", "food_modifiers");
            int after = Math.round(nutrition);
            float saturationBefore = food.getSaturationModifier();
            float saturationAfter = applyFoodModifiers(saturationBefore, player,
                    power, "saturation_modifier", "saturation_modifiers");
            player.getFoodData().setFoodLevel(Math.max(0, Math.min(20,
                    player.getFoodData().getFoodLevel() + after - before)));
            player.getFoodData().setSaturation(Math.max(0.0F, Math.min(player.getFoodData().getFoodLevel(),
                    player.getFoodData().getSaturationLevel() + (after * saturationAfter) - (before * saturationBefore))));
        });
    }

    /**
     * Apoli's modify_food accepts either a single modifier ("food_modifier") or an ordered
     * list ("food_modifiers"); both are applied through the Apoli modifier pipeline.
     */
    private static float applyFoodModifiers(float value, Player player, JsonObject power, String singleKey, String pluralKey) {
        java.util.List<JsonObject> modifiers = new java.util.ArrayList<>();
        if (power.has(singleKey) && power.get(singleKey).isJsonObject()) {
            modifiers.add(power.getAsJsonObject(singleKey));
        }
        if (power.has(pluralKey) && power.get(pluralKey).isJsonArray()) {
            for (var modifier : power.getAsJsonArray(pluralKey)) {
                if (modifier.isJsonObject()) modifiers.add(modifier.getAsJsonObject());
            }
        }
        return (float) FormPowerRuntime.applyModifierList(player, value, modifiers);
    }

    @SubscribeEvent
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().level().isClientSide) {
            if (BatAttachService.toggleOrAttach((net.minecraft.server.level.ServerPlayer) event.getEntity(),
                    event.getPos(), event.getFace())) {
                event.setCanceled(true);
                return;
            }
            runInteraction(event.getEntity(), null, "apoli:action_on_block_use");
        }
    }

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) {
            // A right-clicked attachment always detaches safely when its supporting block changes.
            BatAttachService.tick(player);
        }
    }

    @SubscribeEvent
    public static void useEntity(PlayerInteractEvent.EntityInteract event) {
        if (!event.getEntity().level().isClientSide && event.getTarget() instanceof LivingEntity target) {
            if (eatEntity(event.getEntity(), target)) {
                event.setCanceled(true);
                return;
            }
            runInteraction(event.getEntity(), target, "apoli:action_on_entity_use");
        }
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        final float[] speed = {event.getOriginalSpeed()};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("apoli:modify_break_speed".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))
                    && (!power.has("block_condition") || FormPowerRuntime.matchesBlockState(player.level(),
                    event.getPosition().orElse(player.blockPosition()), power.getAsJsonObject("block_condition")))) {
                speed[0] = (float) FormPowerRuntime.applyModifier(speed[0], power.getAsJsonObject("modifier"));
            }
        });
        event.setNewSpeed(speed[0]);
    }

    private static void tickPower(Player player, ResourceLocation powerId, JsonObject power) {
        if ("apoli:exhaust".equals(FormPowerRegistry.typeOf(power))) {
            // Apoli's exhaust power drains hunger every "interval" ticks while its condition holds.
            int interval = Math.max(1, FormPowerRuntime.intValue(power, "interval", 20));
            if (player.tickCount % interval == 0
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                float exhaustion = FormPowerRuntime.floatValue(power, "exhaustion", 0.0F);
                if (exhaustion != 0.0F) {
                    player.causeFoodExhaustion(exhaustion);
                }
            }
        }
        if ("apoli:action_over_time".equals(FormPowerRegistry.typeOf(power))) {
            int interval = Math.max(1, FormPowerRuntime.intValue(power, "interval", 20));
            if (player.tickCount % interval == 0
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
            }
        }
        if ("shape-shifter-curse:add_sustained_instinct".equals(FormPowerRegistry.typeOf(power))
                && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
            InstinctService.add(player, FormPowerRuntime.stringValue(power, "instinct_effect_id", "shape-shifter-curse:unknown"),
                    FormPowerRuntime.floatValue(power, "value", 0.0F), FormPowerRuntime.intValue(power, "duration", 1), false);
        }
        if ("shape-shifter-curse:action_on_entity_in_range".equals(FormPowerRegistry.typeOf(power))) {
            int interval = Math.max(1, FormPowerRuntime.intValue(power, "detection_interval", 20));
            if (player.tickCount % interval == 0) {
                double radius = FormPowerRuntime.doubleValue(power, "action_radius", 4.0D);
                for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                        target -> target != player && FormPowerRuntime.test(player, target, power.getAsJsonObject("entity_condition")))) {
                    FormPowerRuntime.execute(player, target, power.getAsJsonObject("entity_action"));
                }
                // Fabric also runs the optional self_action on the owner once per detection interval.
                if (power.has("self_action")) {
                    FormPowerRuntime.execute(player, player, power.getAsJsonObject("self_action"));
                }
            }
        }
        if ("apoli:damage_over_time".equals(FormPowerRegistry.typeOf(power))) {
            UUID stateId = UUID.nameUUIDFromBytes((powerId + "|" + power).getBytes(StandardCharsets.UTF_8));
            Map<UUID, Integer> starts = DAMAGE_OVER_TIME_STARTED_TICKS.computeIfAbsent(
                    player.getUUID(), ignored -> new HashMap<>());
            if (!FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                starts.remove(stateId);
                return;
            }
            int start = starts.computeIfAbsent(stateId, ignored -> player.tickCount);
            int onset = Math.max(0, FormPowerRuntime.intValue(power, "onset_delay", 0));
            int interval = Math.max(1, FormPowerRuntime.intValue(power, "interval", 20));
            int elapsed = player.tickCount - start;
            if (elapsed >= onset && (elapsed - onset) % interval == 0) {
                String damageType = FormPowerRuntime.stringValue(power, "damage_type", "minecraft:generic");
                var source = "minecraft:on_fire".equals(damageType) ? player.damageSources().onFire()
                        : player.damageSources().generic();
                // Apoli uses damage_easy on EASY and deals nothing on PEACEFUL.
                var difficulty = player.level().getDifficulty();
                if (difficulty != net.minecraft.world.Difficulty.PEACEFUL) {
                    float amount = difficulty == net.minecraft.world.Difficulty.EASY
                            ? FormPowerRuntime.floatValue(power, "damage_easy",
                                    FormPowerRuntime.floatValue(power, "damage", 0.0F))
                            : FormPowerRuntime.floatValue(power, "damage", 0.0F);
                    player.hurt(source, amount);
                }
            }
        }
    }

    private static void runInteraction(Player player, LivingEntity target, String expectedType) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            JsonObject condition = power.has("bientity_condition") ? power.getAsJsonObject("bientity_condition")
                    : power.getAsJsonObject("condition");
            if (expectedType.equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, target, condition)
                    && (!power.has("item_condition")
                    || FormPowerRuntime.matchesHeldItem(player, power.getAsJsonObject("item_condition")))) {
                FormPowerRuntime.execute(player, target, power.getAsJsonObject("entity_action"));
                FormPowerRuntime.executeHeldItemAction(player, power.getAsJsonObject("item_action"));
            }
        });
    }

    /**
     * Apoli's action_on_item_use fires on a per-power {@code trigger}: "instant" on the
     * initial right-click, "finish" after the use duration completes (or a custom edible
     * is consumed). The stack to match is passed explicitly so finished stacks still match.
     */
    private static void runItemUseInteraction(Player player, ItemStack stack, boolean finish) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:action_on_item_use".equals(FormPowerRegistry.typeOf(power))) return;
            boolean powerFinish = "finish".equals(FormPowerRuntime.stringValue(power, "trigger", "instant"));
            if (powerFinish != finish) return;
            if (power.has("item_condition")
                    && !FormPowerRuntime.matchesItem(stack, power.getAsJsonObject("item_condition"))) return;
            JsonObject condition = power.has("bientity_condition") ? power.getAsJsonObject("bientity_condition")
                    : power.getAsJsonObject("condition");
            if (!FormPowerRuntime.test(player, player, condition)) return;
            FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
            FormPowerRuntime.executeHeldItemAction(player, power.getAsJsonObject("item_action"));
        });
    }

    private static boolean preventsItemUse(Player player) {
        final boolean[] prevents = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("apoli:prevent_item_use".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.matchesHeldItem(player, power.getAsJsonObject("item_condition"))) {
                prevents[0] = true;
            }
        });
        return prevents[0];
    }

    private static boolean handleCustomEdible(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return false;
        ItemStack eaten = stack.copy();
        final boolean[] consumed = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:custom_edible".equals(FormPowerRegistry.typeOf(power))
                    || !matchesCustomFood(stack, power) || (!power.has("always_edible") || !power.get("always_edible").getAsBoolean())
                    && player.getFoodData().getFoodLevel() >= 20) return;
            player.getFoodData().eat(FormPowerRuntime.intValue(power, "hunger", 0),
                    FormPowerRuntime.floatValue(power, "saturation_modifier", 0.0F));
            FormPowerRuntime.consumeHeldItem(player, 1);
            consumed[0] = true;
        });
        if (consumed[0]) {
            // Custom edibles consume without a vanilla use duration; fire finish-triggered item-use powers here.
            runItemUseInteraction(player, eaten, true);
        }
        return consumed[0];
    }

    private static boolean matchesCustomFood(ItemStack stack, JsonObject power) {
        if (!power.has("item_id_list") || !power.get("item_id_list").isJsonArray()) return false;
        ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (var entry : power.getAsJsonArray("item_id_list")) {
            if (stackId.toString().equals(entry.getAsString())) return true;
        }
        return false;
    }

    private static void refreshAttributes(Player player) {
        UUID stateKey = attributeStateKey(player);
        Map<UUID, AttributeInstance> owned = OWNED_ATTRIBUTE_MODIFIERS.computeIfAbsent(
                stateKey, ignored -> new HashMap<>());
        Set<UUID> wanted = new HashSet<>();
        Set<UUID> seen = new HashSet<>();
        FormPowerRegistry.visitActive(player, (id, power) -> refreshAttribute(player, id, power, wanted, seen, owned));
        owned.entrySet().removeIf(entry -> {
            if (wanted.contains(entry.getKey())) {
                return false;
            }
            Map<UUID, Boolean> healthStates = UPDATE_HEALTH_MODIFIERS.get(stateKey);
            boolean updateHealth = healthStates != null && healthStates.getOrDefault(entry.getKey(), false);
            float oldMaxHealth = player.getMaxHealth();
            float healthRatio = oldMaxHealth <= 0.0F ? 1.0F : player.getHealth() / oldMaxHealth;
            entry.getValue().removeModifier(entry.getKey());
            if (updateHealth && oldMaxHealth != player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth() * healthRatio);
            }
            if (healthStates != null) {
                healthStates.remove(entry.getKey());
            }
            return true;
        });
        Map<UUID, DelayAttributeState> delayStates = DELAY_ATTRIBUTE_STATES.get(stateKey);
        if (delayStates != null) {
            delayStates.keySet().retainAll(seen);
        }
        Map<UUID, ConditionedAttributeState> conditionedStates = CONDITIONED_ATTRIBUTE_STATES.get(stateKey);
        if (conditionedStates != null) {
            conditionedStates.keySet().retainAll(seen);
        }
        Map<UUID, Boolean> updateHealthStates = UPDATE_HEALTH_MODIFIERS.get(stateKey);
        if (updateHealthStates != null) {
            updateHealthStates.keySet().retainAll(seen);
        }
        LAST_ATTRIBUTE_REFRESH_TICK.put(stateKey, player.tickCount);
    }

    /**
     * Integrated servers host the logical client and server in one JVM.  Their
     * Player instances have the same UUID but must never share transient-modifier
     * bookkeeping, or one side can believe the other side's modifier is local.
     */
    private static UUID attributeStateKey(Player player) {
        if (!player.level().isClientSide) {
            return player.getUUID();
        }
        return UUID.nameUUIDFromBytes(("client-attributes:" + player.getUUID())
                .getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The in-game power-status command writes to chat, not latest.log.  Keep a
     * compact server-log probe for the three Axolotl III land-speed powers so a
     * dedicated-server report contains the actual condition and installed state.
     * It is edge-triggered, so ordinary movement does not flood the log.
     */
    private static void logAxolotlMovementState(Player player) {
        if (!"shape-shifter-curse:axolotl_3".equals(FormManager.current(player).id().toString())) {
            LAST_AXOLOTL_MOVE_DEBUG.remove(attributeStateKey(player));
            return;
        }
        MoveSpeedDebug debug = moveSpeedDebug(player);
        String powers = debug.powers().stream()
                .filter(power -> power.powerId().getPath().startsWith("form_axolotl_3_"))
                .map(power -> power.powerId().getPath() + "=" + power.conditionMet()
                        + "/" + power.installed())
                .reduce((left, right) -> left + "," + right).orElse("<none>");
        String snapshot = "sprint=" + debug.sprinting() + ", shift=" + player.isShiftKeyDown()
                + ", air=" + player.getAirSupply() + ", water=" + player.getFluidHeight(FluidTags.WATER)
                + ", ground=" + player.onGround() + ", speed=" + debug.effectiveValue()
                + ", powers=" + powers;
        UUID stateKey = attributeStateKey(player);
        if (!snapshot.equals(LAST_AXOLOTL_MOVE_DEBUG.put(stateKey, snapshot))) {
            ShapeShifterCurseForge.LOGGER.info("[SSC-MOVE-DEBUG] side={} player={} {}",
                    player.level().isClientSide ? "client" : "server", player.getGameProfile().getName(), snapshot);
        }
    }

    private static void adjustFoodHealTimer(Player player) {
        if (player.getFoodData().getFoodLevel() < 18 || player.getHealth() >= player.getMaxHealth()) return;
        Map<ResourceLocation, Float> remainders = FOOD_HEAL_REMAINDERS.computeIfAbsent(
                player.getUUID(), ignored -> new HashMap<>());
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:modify_food_heal".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            int rate = Math.max(1, FormPowerRuntime.intValue(power, "modify_food_timer_tick_rate", 20));
            if (player.tickCount % rate != 0) return;
            float pending = remainders.getOrDefault(id, 0.0F)
                    + FormPowerRuntime.floatValue(power, "food_timer_add_amount", 1.0F);
            int adjustment = pending > 0.0F ? (int) Math.floor(pending) : (int) Math.ceil(pending);
            remainders.put(id, pending - adjustment);
            FoodData food = player.getFoodData();
            food.tickTimer = Math.max(0, food.tickTimer + adjustment);
        });
    }

    private static boolean effectIsListed(net.minecraft.world.effect.MobEffect effect, JsonObject power) {
        ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        boolean listed = false;
        // Apoli (and SSC's optional variant) accept a single "effect" and/or an "effects" list.
        if (power.has("effect") && power.get("effect").isJsonPrimitive()) {
            listed = Objects.requireNonNull(effectId).toString().equals(power.get("effect").getAsString());
        }
        if (!listed && power.has("effects") && power.get("effects").isJsonArray()) {
            for (var entry : power.getAsJsonArray("effects")) {
                if (Objects.requireNonNull(effectId).toString().equals(entry.getAsString())) {
                    listed = true;
                    break;
                }
            }
        }
        // "inverted" means immune to everything EXCEPT the listed effects.
        return (power.has("inverted") && power.get("inverted").getAsBoolean()) != listed;
    }

    /** Collects the "effect" and "effects" ids declared by an effect filter. */
    private static java.util.List<ResourceLocation> collectedEffectIds(JsonObject power) {
        java.util.List<ResourceLocation> ids = new java.util.ArrayList<>();
        if (power.has("effect") && power.get("effect").isJsonPrimitive()) {
            ResourceLocation id = ResourceLocation.tryParse(power.get("effect").getAsString());
            if (id != null) ids.add(id);
        }
        if (power.has("effects") && power.get("effects").isJsonArray()) {
            for (var entry : power.getAsJsonArray("effects")) {
                ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
                if (id != null) ids.add(id);
            }
        }
        return ids;
    }

    private static void enforceSprinting(Player player) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("apoli:prevent_sprinting".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                player.setSprinting(false);
            }
        });
    }

    private static boolean matchesProjectile(Projectile projectile, JsonObject condition) {
        if (condition == null) return true;
        if (!"apoli:projectile".equals(FormPowerRegistry.typeOf(condition))) return true;
        ResourceLocation id = ResourceLocation.tryParse(FormPowerRuntime.stringValue(condition, "projectile", ""));
        return id != null && id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType()));
    }

    private static boolean isInstantMagic(net.minecraft.world.damagesource.DamageSource source) {
        // TODO[PARITY] Fabric scales instant damage at the potion mixin; this magic/indirectMagic
        //   source check is an approximation that may also match other magic damage.
        String id = source.getMsgId();
        return "magic".equals(id) || "indirectMagic".equals(id);
    }

    private static boolean blocksWithVirtualShield(Player defender, LivingHurtEvent event, JsonObject power) {
        if (!FormPowerRuntime.test(defender, event.getSource().getEntity(),
                power.getAsJsonObject("active_shield_condition"))) return false;
        var source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_SHIELD)) return false;
        if (source.getDirectEntity() instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) return false;
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition == null) return false;
        Vec3 incoming = sourcePosition.vectorTo(defender.position()).normalize();
        Vec3 facing = defender.getLookAngle();
        if (new Vec3(incoming.x, 0.0D, incoming.z).dot(new Vec3(facing.x, 0.0D, facing.z)) >= 0.0D) {
            return false;
        }
        FormPowerRuntime.execute(defender, defender, power.getAsJsonObject("taken_damage_action"));
        if (source.getEntity() instanceof LivingEntity living && living.canDisableShield()) {
            FormPowerRuntime.execute(defender, defender, power.getAsJsonObject("shield_break_action"));
        } else {
            FormPowerRuntime.execute(defender, defender, power.getAsJsonObject("normal_damage_action"));
        }
        return true;
    }

    private static double applyFallModifiers(double value, JsonObject power, String single, String plural) {
        if (power.has(single) && power.get(single).isJsonObject()) {
            value = FormPowerRuntime.applyModifier(value, power.getAsJsonObject(single));
        }
        if (power.has(plural) && power.get(plural).isJsonArray()) {
            for (var modifier : power.getAsJsonArray(plural)) {
                if (modifier.isJsonObject()) value = FormPowerRuntime.applyModifier(value, modifier.getAsJsonObject());
            }
        }
        return value;
    }

    private static void applyClimbing(Player player) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:climbing_ex".equals(FormPowerRegistry.typeOf(power))) return;
            JsonObject start = power.getAsJsonObject("start_climb_condition");
            JsonObject keep = power.getAsJsonObject("continue_climb_condition");
            if (FormPowerRuntime.test(player, player, start) || FormPowerRuntime.test(player, player, keep)) {
                Vec3 motion = player.getDeltaMovement();
                double y = Math.max(motion.y, -0.15D);
                if (Double.compare(motion.y, y) != 0) {
                    player.setDeltaMovement(motion.x, y, motion.z);
                    // The server tracker otherwise broadcasts hasImpulse only to
                    // watchers, not to the player whose climb velocity changed.
                    if (!player.level().isClientSide) player.hurtMarked = true;
                }
                player.resetFallDistance();
            }
        });
    }

    private static boolean eatEntity(Player player, LivingEntity target) {
        if (!player.getMainHandItem().isEmpty()) return false;
        final boolean[] ate = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (ate[0] || !"shape-shifter-curse:eat_entity".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, target, power.getAsJsonObject("condition"))) return;
            if (power.has("must_empty_hand") && power.get("must_empty_hand").getAsBoolean() && !player.getMainHandItem().isEmpty()) return;
            if (!power.has("food_map") || !power.get("food_map").isJsonArray()) return;
            ResourceLocation targetId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
            for (var mapping : power.getAsJsonArray("food_map")) {
                if (!mapping.isJsonObject() || !targetId.toString().equals(FormPowerRuntime.stringValue(mapping.getAsJsonObject(), "entity", ""))) continue;
                JsonObject food = mapping.getAsJsonObject().getAsJsonObject("food");
                if (food == null) continue;
                player.getFoodData().eat(FormPowerRuntime.intValue(food, "hunger", 0),
                        FormPowerRuntime.floatValue(food, "saturation", 0.0F));
                if (food.has("effects") && food.get("effects").isJsonArray()) {
                    for (var entry : food.getAsJsonArray("effects")) {
                        if (entry.isJsonObject() && entry.getAsJsonObject().has("effect")) {
                            JsonObject apply = new JsonObject();
                            apply.addProperty("type", "apoli:apply_effect");
                            apply.add("effect", entry.getAsJsonObject().getAsJsonObject("effect"));
                            FormPowerRuntime.execute(player, player, apply);
                        }
                    }
                }
                target.hurt(player.damageSources().playerAttack(player), Float.MAX_VALUE);
                ate[0] = true;
                break;
            }
        });
        return ate[0];
    }

    private static void maintainBreathingAndImmunity(Player player) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            String type = FormPowerRegistry.typeOf(power);
            if ("shape-shifter-curse:hold_breath".equals(type) && player.isInWater()
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                player.setAirSupply(player.getMaxAirSupply());
            }
            if ("shape-shifter-curse:optional_effect_immunity".equals(type)) {
                if (power.has("inverted") && power.get("inverted").getAsBoolean()) {
                    // Immune to everything except the listed effects.
                    for (var instance : new java.util.ArrayList<>(player.getActiveEffects())) {
                        if (!effectIsListed(instance.getEffect(), power)) {
                            player.removeEffect(instance.getEffect());
                        }
                    }
                } else {
                    for (ResourceLocation effectId : collectedEffectIds(power)) {
                        var effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
                        if (effect != null) player.removeEffect(effect);
                    }
                }
            }
        });
        tickCustomWaterBreathing(player);
    }

    /**
     * Forge port of Fabric's CustomWaterBreathingMixin UpdateAir tick (moisture/oxygen).
     * Vanilla's own air change is neutralized in {@link #onLivingBreathe} so the values
     * below are net changes, matching Fabric where the mixin owns the whole section:
     * <ul>
     *   <li>creative/spectator refills to maxAir (Fabric first branch); without this,
     *       creative air sticks at &le;0 and every air&gt;0-gated power (e.g. axolotl
     *       sprinting_speed) silently stops working;</li>
     *   <li>land drain subtracts {@code increaseAirSupply(0)} (normally 4) after
     *       the respiration-style roll. Thus it is -4 when the roll skips and -5
     *       when it does not; {@code level &gt;= 1000} still drains by -4;</li>
     *   <li>rain and eye-in-water recover via vanilla {@code increaseAirSupply} (+4);</li>
     *   <li>depleted state: without damage configured negatives clamp to -1
     *       ("oxygen (moisture)"); with damage configured the -20 loop deals the
     *       custom gills damage instead of vanilla drown (see {@link #onLivingDrown}).</li>
     * </ul>
     */
    private static void tickCustomWaterBreathing(Player player) {
        final int[] totalLevel = {0};
        final boolean[] damageWhenNoAir = {false};
        final boolean[] any = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:custom_water_breathing".equals(FormPowerRegistry.typeOf(power))) return;
            if (!FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            any[0] = true;
            totalLevel[0] += Math.max(0, FormPowerRuntime.intValue(power, "land_water_breathing_level", 24));
            if (FormPowerRuntime.booleanValue(power, "damage_when_no_air", false)) damageWhenNoAir[0] = true;
        });
        if (!any[0]) return;

        if (player.isCreative() || player.isSpectator()) {
            if (player.getAirSupply() < player.getMaxAirSupply()) {
                player.setAirSupply(player.getMaxAirSupply());
            }
            return;
        }
        if (player.hasEffect(MobEffects.WATER_BREATHING) || player.hasEffect(MobEffects.CONDUIT_POWER)) {
            if (player.getAirSupply() < player.getMaxAirSupply()) {
                player.setAirSupply(Math.min(player.getAirSupply() + 4, player.getMaxAirSupply()));
            }
        } else if (player.level().isRainingAt(player.blockPosition())) {
            if (player.getAirSupply() < player.getMaxAirSupply()) {
                player.setAirSupply(Math.min(player.getAirSupply() + 4, player.getMaxAirSupply()));
            }
        } else if (player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) {
            if (player.getAirSupply() < player.getMaxAirSupply()) {
                player.setAirSupply(Math.min(player.getAirSupply() + 4, player.getMaxAirSupply()));
            }
        } else {
            boolean skip = totalLevel[0] >= 1000
                    || (totalLevel[0] > 0 && player.getRandom().nextInt(totalLevel[0] + 1) > 0);
            // Exact Fabric expression:
            // setAir(getNextAirUnderwaterSlow(getAir(), level) - increaseAirSupply(0)).
            int airAfterRespirationRoll = player.getAirSupply() - (skip ? 0 : 1);
            player.setAirSupply(airAfterRespirationRoll - Math.min(4, player.getMaxAirSupply()));
        }
        if (damageWhenNoAir[0]) {
            if (player.getAirSupply() == -20) {
                player.setAirSupply(0);
                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE,
                            player.getX(), player.getY() + 0.5D, player.getZ(),
                            8, 0.5D, 0.1D, 0.5D, 0.0D);
                }
                player.hurt(gillsDamageSource(player), 2.0F);
            }
        } else {
            if (player.getAirSupply() < 0) {
                player.setAirSupply(-1);
            }
        }
    }

    private static net.minecraft.world.damagesource.DamageSource gillsDamageSource(Player player) {
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, "no_water_for_gills"));
        var holder = player.level().registryAccess().registryOrThrow(
                net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key);
        return new net.minecraft.world.damagesource.DamageSource(holder);
    }

    /**
     * Neutralizes vanilla's own air delta for power holders so {@link #tickCustomWaterBreathing}
     * is the single owner of moisture values (Fabric's mixin replaces the whole section).
     * Without this, vanilla's underwater -1 would stack on top of the custom logic.
     */
    @SubscribeEvent
    public static void onLivingBreathe(net.minecraftforge.event.entity.living.LivingBreatheEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        final boolean[] active = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!active[0] && "shape-shifter-curse:custom_water_breathing".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                active[0] = true;
            }
        });
        if (!active[0]) return;
        event.setConsumeAirAmount(0);
        event.setCanRefillAir(false);
    }

    /**
     * Takes over the -20 drowning loop for power holders: vanilla drown is cancelled,
     * {@link #tickCustomWaterBreathing} applies the custom gills damage (or the -1 clamp)
     * on the same tick instead.
     */
    @SubscribeEvent
    public static void onLivingDrown(net.minecraftforge.event.entity.living.LivingDrownEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        final boolean[] active = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!active[0] && "shape-shifter-curse:custom_water_breathing".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                active[0] = true;
            }
        });
        if (active[0]) {
            event.setCanceled(true);
        }
    }

    /**
     * Applies only the modifiers selected by the current form.  Keeping an already installed
     * modifier avoids a remove/add attribute packet every server tick, while the owned set
     * removes modifiers from a previous form (including children of apoli:multiple).
     */
    private static void refreshAttribute(Player player, ResourceLocation powerId, JsonObject power,
                                         Set<UUID> wanted, Set<UUID> seen, Map<UUID, AttributeInstance> owned) {
        String type = FormPowerRegistry.typeOf(power);
        boolean waterSpeedModifier = "shape-shifter-curse:in_water_speed_modifier".equals(type);
        if (!"apoli:attribute".equals(type) && !"apoli:conditioned_attribute".equals(type)
                && !"shape-shifter-curse:delay_attribute".equals(type) && !waterSpeedModifier) {
            return;
        }

        JsonObject modifier = waterSpeedModifier ? null : power.getAsJsonObject("modifier");
        ResourceLocation attributeId = waterSpeedModifier ? LEGACY_WATER_SPEED
                : ResourceLocation.tryParse(FormPowerRuntime.stringValue(modifier, "attribute", ""));
        // Additional Entity Attributes supplied the Fabric water-speed attribute. Forge 1.20.1
        // has the same movement hook built in; mapping it preserves the original JSON values
        // without retaining that dependency.
        Attribute attribute = LEGACY_WATER_SPEED.equals(attributeId)
                ? ForgeMod.SWIM_SPEED.get()
                : attributeId == null ? null : BuiltInRegistries.ATTRIBUTE.get(attributeId);
        AttributeInstance instance = attribute == null ? null : player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        UUID uuid = attributeModifierId(powerId, attributeId, power);
        seen.add(uuid);
        boolean updateHealth = power.has("updateHealth") && power.get("updateHealth").getAsBoolean();
        UPDATE_HEALTH_MODIFIERS.computeIfAbsent(attributeStateKey(player), ignored -> new HashMap<>()).put(uuid, updateHealth);
        if (!attributeConditionMet(player, power, type, uuid)) {
            return;
        }

        wanted.add(uuid);
        owned.put(uuid, instance);
        if (instance.getModifier(uuid) != null) {
            return;
        }

        boolean isLegacy = LEGACY_WATER_SPEED.equals(attributeId);
        AttributeModifier.Operation operation;
        double amount;
        if (isLegacy) {
            // Fabric water_speed 1.2 means 20% boost, not 120%; convert to 0.2 for Forge SWIM_SPEED
            operation = AttributeModifier.Operation.MULTIPLY_TOTAL;
            amount = FormPowerRuntime.doubleValue(modifier, "value", 1.0D) - 1.0D;
        } else {
            // Apoli's extended attribute operations are collapsed to the three vanilla
            // AttributeModifier operations. SSC's data only uses addition/multiply_base/multiply_total,
            // so this is currently equivalent, but it is not a general Apoli attribute pipeline.
            operation = switch (FormPowerRuntime.stringValue(modifier, "operation", "addition")) {
                case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
                case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
                default -> AttributeModifier.Operation.ADDITION;
            };
            amount = FormPowerRuntime.doubleValue(modifier, "value", 0.0D);
        }
        float oldMaxHealth = player.getMaxHealth();
        float healthRatio = oldMaxHealth <= 0.0F ? 1.0F : player.getHealth() / oldMaxHealth;
        instance.addTransientModifier(new AttributeModifier(uuid,
                waterSpeedModifier ? powerId.toString() : FormPowerRuntime.stringValue(modifier, "name", powerId.toString()),
                amount, operation));
        if (updateHealth && oldMaxHealth != player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth() * healthRatio);
        }
    }

    private static boolean attributeConditionMet(Player player, JsonObject power, String type, UUID modifierId) {
        if (isLegacyWaterSpeed(power, type) && !player.isEyeInFluid(FluidTags.WATER)) {
            return false;
        }
        if ("apoli:attribute".equals(type)) {
            return true;
        }
        if ("shape-shifter-curse:in_water_speed_modifier".equals(type)) {
            return player.isInWater() && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"));
        }
        if ("apoli:conditioned_attribute".equals(type)) {
            ConditionedAttributeState state = CONDITIONED_ATTRIBUTE_STATES
                    .computeIfAbsent(attributeStateKey(player), ignored -> new HashMap<>())
                    .computeIfAbsent(modifierId, ignored -> new ConditionedAttributeState());
            int tickRate = Math.max(1, FormPowerRuntime.intValue(power, "tick_rate", 20));
            if (!state.initialized || player.tickCount % tickRate == 0) {
                state.applied = FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"));
                state.initialized = true;
            }
            return state.applied;
        }
        boolean conditionMet = FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"));
        if (!"shape-shifter-curse:delay_attribute".equals(type)) {
            return conditionMet;
        }
        DelayAttributeState state = DELAY_ATTRIBUTE_STATES.computeIfAbsent(attributeStateKey(player), ignored -> new HashMap<>())
                .computeIfAbsent(modifierId, ignored -> new DelayAttributeState(
                        Math.max(0, FormPowerRuntime.intValue(power, "delay", 0))));
        int tickRate = Math.max(1, FormPowerRuntime.intValue(power, "tick_rate", 1));
        if (player.tickCount % tickRate != 0) {
            return state.applied;
        }
        if (conditionMet == state.applied) {
            state.transitionTicks = 0;
            return state.applied;
        }
        if (state.transitionTicks >= state.delay) {
            state.applied = conditionMet;
            state.transitionTicks = 0;
        } else {
            state.transitionTicks++;
        }
        return state.applied;
    }

    private static boolean isLegacyWaterSpeed(JsonObject power, String type) {
        if (!"apoli:attribute".equals(type) && !"apoli:conditioned_attribute".equals(type)
                && !"shape-shifter-curse:delay_attribute".equals(type)) {
            return false;
        }
        JsonObject modifier = power.getAsJsonObject("modifier");
        ResourceLocation attributeId = ResourceLocation.tryParse(
                FormPowerRuntime.stringValue(modifier, "attribute", ""));
        return LEGACY_WATER_SPEED.equals(attributeId);
    }

    /** Server-side probe used by /ssc power status to verify the complete swim-speed chain. */
    public static SwimSpeedDebug swimSpeedDebug(Player player) {
        AttributeInstance instance = player.getAttribute(ForgeMod.SWIM_SPEED.get());
        List<SwimModifierDebug> powers = new ArrayList<>();
        FormPowerRegistry.visitActive(player, (powerId, power) -> {
            String type = FormPowerRegistry.typeOf(power);
            boolean waterSpeedModifier = "shape-shifter-curse:in_water_speed_modifier".equals(type);
            if (!"apoli:attribute".equals(type) && !"apoli:conditioned_attribute".equals(type)
                    && !"shape-shifter-curse:delay_attribute".equals(type) && !waterSpeedModifier) {
                return;
            }
            JsonObject modifier = waterSpeedModifier ? null : power.getAsJsonObject("modifier");
            ResourceLocation attributeId = waterSpeedModifier ? LEGACY_WATER_SPEED
                    : ResourceLocation.tryParse(FormPowerRuntime.stringValue(modifier, "attribute", ""));
            if (!LEGACY_WATER_SPEED.equals(attributeId)) {
                return;
            }
            UUID modifierId = attributeModifierId(powerId, attributeId, power);
            boolean conditionMet = attributeConditionMet(player, power, type, modifierId);
            boolean installed = instance != null && instance.getModifier(modifierId) != null;
            powers.add(new SwimModifierDebug(powerId, conditionMet, installed,
                    waterSpeedModifier ? FormPowerRuntime.doubleValue(power, "modifier", 1.0D) - 1.0D
                            : FormPowerRuntime.doubleValue(modifier, "value", 0.0D),
                    waterSpeedModifier ? "multiply_total" : FormPowerRuntime.stringValue(modifier, "operation", "addition")));
        });
        return new SwimSpeedDebug(instance != null, instance == null ? 0.0D : instance.getBaseValue(),
                instance == null ? 0.0D : instance.getValue(),
                LAST_ATTRIBUTE_REFRESH_TICK.getOrDefault(attributeStateKey(player), -1), List.copyOf(powers));
    }

    private static UUID attributeModifierId(ResourceLocation powerId, ResourceLocation attributeId, JsonObject power) {
        return UUID.nameUUIDFromBytes((powerId + "|" + attributeId + "|" + power)
                .getBytes(StandardCharsets.UTF_8));
    }

    public record SwimSpeedDebug(boolean attributePresent, double baseValue, double effectiveValue,
                                 int lastRefreshTick, List<SwimModifierDebug> powers) {
    }

    public record SwimModifierDebug(ResourceLocation powerId, boolean conditionMet, boolean installed,
                                    double amount, String operation) {
    }

    /** Movement-speed probe for the walk-vs-sprint report: lists every movement_speed
     * modifier (installed or not), the vanilla sprint boost, and the live sprint flag. */
    public static MoveSpeedDebug moveSpeedDebug(Player player) {
        var instance = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        List<SwimModifierDebug> powers = new ArrayList<>();
        FormPowerRegistry.visitActive(player, (powerId, power) -> {
            String type = FormPowerRegistry.typeOf(power);
            if (!"apoli:attribute".equals(type) && !"apoli:conditioned_attribute".equals(type)
                    && !"shape-shifter-curse:delay_attribute".equals(type)) {
                return;
            }
            com.google.gson.JsonElement modEl = power.get("modifier");
            if (modEl == null || !modEl.isJsonObject()) return;
            JsonObject modifier = modEl.getAsJsonObject();
            ResourceLocation attributeId = ResourceLocation.tryParse(
                    FormPowerRuntime.stringValue(modifier, "attribute", ""));
            if (attributeId == null || !attributeId.toString().equals("minecraft:generic.movement_speed")) {
                return;
            }
            UUID modifierId = attributeModifierId(powerId, attributeId, power);
            boolean conditionMet = attributeConditionMet(player, power, type, modifierId);
            boolean installed = instance != null && instance.getModifier(modifierId) != null;
            powers.add(new SwimModifierDebug(powerId, conditionMet, installed,
                    FormPowerRuntime.doubleValue(modifier, "value", 0.0D),
                    FormPowerRuntime.stringValue(modifier, "operation", "addition")));
        });
        boolean vanillaBoost = false;
        try {
            vanillaBoost = instance != null && instance.getModifier(
                    UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D")) != null;
        } catch (IllegalArgumentException ignored) { }
        return new MoveSpeedDebug(player.isSprinting(), instance != null,
                instance == null ? 0.0D : instance.getBaseValue(),
                instance == null ? 0.0D : instance.getValue(), vanillaBoost,
                LAST_ATTRIBUTE_REFRESH_TICK.getOrDefault(attributeStateKey(player), -1), List.copyOf(powers));
    }

    public record MoveSpeedDebug(boolean sprinting, boolean attributePresent, double baseValue,
                                 double effectiveValue, boolean vanillaSprintBoost, int lastRefreshTick,
                                 List<SwimModifierDebug> powers) {
    }

    private static final class DelayAttributeState {
        private final int delay;
        private int transitionTicks;
        private boolean applied;

        private DelayAttributeState(int delay) {
            this.delay = delay;
            this.transitionTicks = 0;
        }
    }

    private static final class ConditionedAttributeState {
        private boolean initialized;
        private boolean applied;
    }
}
