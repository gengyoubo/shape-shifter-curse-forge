package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

/** Server event bridge for the high-frequency Apoli power families used by the forms. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class FormPowerEvents {
    private FormPowerEvents() {
    }

    private static final Map<UUID, Map<ResourceLocation, Float>> FOOD_HEAL_REMAINDERS = new HashMap<>();
    private static final ThreadLocal<Boolean> SWEEP_DAMAGE = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }

        refreshAttributes(player);
        FormActivePowerService.tick(player);
        InstinctService.tick((net.minecraft.server.level.ServerPlayer) player);
        BatAttachService.tick(player);
        MovementPowerService.tick(player);
        adjustFoodHealTimer(player);
        FormPowerRegistry.visitActive(player, (id, power) -> tickPower(player, power));
        applyClimbing(player);
        maintainBreathingAndImmunity(player);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
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
    public static void potionEffectAdded(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || !(event.getEffectSource() instanceof net.minecraft.world.entity.projectile.ThrownPotion
                || event.getEffectSource() instanceof net.minecraft.world.entity.AreaEffectCloud)) return;
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:action_on_splash_potion_take_effect".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("entity_condition"))) {
                FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
            }
        });
    }

    @SubscribeEvent
    public static void waterPotionImpact(ProjectileImpactEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.projectile.ThrownPotion potion)
                || potion.level().isClientSide
                || net.minecraft.world.item.alchemy.PotionUtils.getPotion(potion.getItem())
                != net.minecraft.world.item.alchemy.Potions.WATER) return;
        for (Player player : potion.level().getEntitiesOfClass(Player.class,
                potion.getBoundingBox().inflate(4.0D), candidate -> candidate.isAlive())) {
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
                if ("shape-shifter-curse:virtual_shield".equals(type)
                        && blocksWithVirtualShield(defender, event, power)) {
                    event.setCanceled(true);
                }
                if ("apoli:modify_damage_taken".equals(type)
                        && FormPowerRuntime.test(defender, attacker, power.getAsJsonObject("condition"))) {
                    event.setAmount((float) FormPowerRuntime.applyModifier(event.getAmount(), power.getAsJsonObject("modifier")));
                }
                if ("apoli:self_action_when_hit".equals(type)
                        && FormPowerRuntime.test(defender, attacker, power.getAsJsonObject("damage_condition"))) {
                    FormPowerRuntime.execute(defender, defender, power.getAsJsonObject("entity_action"));
                }
                if ("apoli:action_when_hit".equals(type)
                        && attacker != null && FormPowerRuntime.test(defender, attacker, power.getAsJsonObject("damage_condition"))) {
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
                        && FormPowerRuntime.test(player, event.getEntity(), power.getAsJsonObject("damage_condition"))) {
                    FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
                }
                if ("apoli:action_on_hit".equals(type)
                        && FormPowerRuntime.test(player, event.getEntity(), power.getAsJsonObject("damage_condition"))) {
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
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        if (BatAttachService.detachForJump(player)) {
            return;
        }
        FormActivePowerService.registerGroundJump(player);
        FormActivePowerService.triggerVanillaKey(player, "key.jump");
        FormPowerRegistry.visitActive(player, (id, power) -> {
            String type = FormPowerRegistry.typeOf(power);
            if ("shape-shifter-curse:action_on_jump".equals(type)
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
            }
            if ("apoli:modify_jump".equals(type)
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                double modifier = FormPowerRuntime.applyModifier(0.42D, power.getAsJsonObject("modifier"));
                player.setDeltaMovement(player.getDeltaMovement().x, modifier, player.getDeltaMovement().z);
            }
        });
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        final float[] distance = {event.getDistance()};
        final float[] multiplier = {event.getDamageMultiplier()};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            String type = FormPowerRegistry.typeOf(power);
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
            runInteraction(event.getEntity(), null, "apoli:action_on_item_use");
        }
    }

    @SubscribeEvent
    public static void finishUsingItem(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        ItemStack used = event.getItem();
        FoodProperties food = used.getFoodProperties(player);
        if (food == null) return;
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:modify_food".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.matchesItem(used, power.getAsJsonObject("item_condition"))) return;
            int before = food.getNutrition();
            int after = (int) Math.round(FormPowerRuntime.applyModifier(before, power.getAsJsonObject("food_modifier")));
            float saturationBefore = food.getSaturationModifier();
            float saturationAfter = (float) FormPowerRuntime.applyModifier(saturationBefore,
                    power.getAsJsonObject("saturation_modifier"));
            player.getFoodData().setFoodLevel(Math.max(0, Math.min(20,
                    player.getFoodData().getFoodLevel() + after - before)));
            player.getFoodData().setSaturation(Math.max(0.0F, Math.min(player.getFoodData().getFoodLevel(),
                    player.getFoodData().getSaturationLevel() + (after * saturationAfter) - (before * saturationBefore))));
        });
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
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                speed[0] = (float) FormPowerRuntime.applyModifier(speed[0], power.getAsJsonObject("modifier"));
            }
        });
        event.setNewSpeed(speed[0]);
    }

    private static void tickPower(Player player, JsonObject power) {
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
        FormPowerRegistry.all().forEach((id, definition) -> refreshAttribute(player, id, definition.data(), false));
        FormPowerRegistry.visitActive(player, (id, power) -> refreshAttribute(player, id, power, true));
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

    private static boolean matchesProjectile(Projectile projectile, JsonObject condition) {
        if (condition == null) return true;
        if (!"apoli:projectile".equals(FormPowerRegistry.typeOf(condition))) return true;
        ResourceLocation id = ResourceLocation.tryParse(FormPowerRuntime.stringValue(condition, "projectile", ""));
        return id != null && id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType()));
    }

    private static boolean isInstantMagic(net.minecraft.world.damagesource.DamageSource source) {
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
                player.setDeltaMovement(player.getDeltaMovement().x, Math.max(player.getDeltaMovement().y, -0.15D),
                        player.getDeltaMovement().z);
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
            if ("shape-shifter-curse:breathing_under_water".equals(type)
                    || ("shape-shifter-curse:hold_breath".equals(type) && player.isInWater())) {
                player.setAirSupply(player.getMaxAirSupply());
            }
            if ("shape-shifter-curse:custom_water_breathing".equals(type) && !player.isInWater()) {
                int level = Math.max(1, FormPowerRuntime.intValue(power, "land_water_breathing_level", 24));
                if (player.getRandom().nextInt(level) == 0) {
                    player.setAirSupply(player.getAirSupply() - 1);
                    if (player.getAirSupply() <= -20 && power.has("damage_when_no_air")
                            && power.get("damage_when_no_air").getAsBoolean()) {
                        player.hurt(player.damageSources().drown(), 2.0F);
                    }
                }
            }
            if ("shape-shifter-curse:optional_effect_immunity".equals(type)
                    && power.has("effects") && power.get("effects").isJsonArray()) {
                for (var effectId : power.getAsJsonArray("effects")) {
                    ResourceLocation idToRemove = ResourceLocation.tryParse(effectId.getAsString());
                    if (idToRemove == null) continue;
                    var effect = BuiltInRegistries.MOB_EFFECT.get(idToRemove);
                    if (effect != null) player.removeEffect(effect);
                }
            }
        });
    }

    private static void refreshAttribute(Player player, ResourceLocation powerId, JsonObject power, boolean active) {
        String type = FormPowerRegistry.typeOf(power);
        if (!"apoli:attribute".equals(type) && !"apoli:conditioned_attribute".equals(type)) {
            return;
        }

        JsonObject modifier = power.getAsJsonObject("modifier");
        ResourceLocation attributeId = ResourceLocation.tryParse(FormPowerRuntime.stringValue(modifier, "attribute", ""));
        Attribute attribute = attributeId == null ? null : BuiltInRegistries.ATTRIBUTE.get(attributeId);
        AttributeInstance instance = attribute == null ? null : player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        UUID uuid = UUID.nameUUIDFromBytes((powerId + "|" + attributeId + "|" + power.toString())
                .getBytes(StandardCharsets.UTF_8));
        instance.removeModifier(uuid);
        if (!active || ("apoli:conditioned_attribute".equals(type)
                && !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition")))) {
            return;
        }

        AttributeModifier.Operation operation = switch (FormPowerRuntime.stringValue(modifier, "operation", "addition")) {
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> AttributeModifier.Operation.ADDITION;
        };
        instance.addTransientModifier(new AttributeModifier(uuid,
                FormPowerRuntime.stringValue(modifier, "name", powerId.toString()),
                FormPowerRuntime.doubleValue(modifier, "value", 0.0D), operation));
    }
}
