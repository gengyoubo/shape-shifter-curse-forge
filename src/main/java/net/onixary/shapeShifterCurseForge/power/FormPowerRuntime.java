package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.util.Accessory.AccessoryUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Shared condition and action interpreter for the common Origins JSON building blocks. */
public final class FormPowerRuntime {
    private FormPowerRuntime() {
    }

    public static boolean test(Player actor, Entity target, JsonObject condition) {
        if (condition == null) {
            return true;
        }

        String type = FormPowerRegistry.typeOf(condition);
        boolean result = switch (type) {
            case "apoli:and" -> testAll(actor, target, condition.getAsJsonArray("conditions"));
            case "apoli:or" -> testAny(actor, target, condition.getAsJsonArray("conditions"));
            case "apoli:sneaking" -> actor.isCrouching();
            case "apoli:sprinting" -> actor.isSprinting();
            case "apoli:on_ground" -> actor.onGround();
            case "apoli:moving" -> actor.getDeltaMovement().horizontalDistanceSqr() > 0.0004D;
            case "apoli:food_level" -> compare(actor.getFoodData().getFoodLevel(), condition);
            case "apoli:fluid_height" -> compare(fluidHeight(actor, condition), condition);
            case "apoli:submerged_in" -> submergedIn(actor, condition);
            case "apoli:exposed_to_sun" -> actor.level().canSeeSky(actor.blockPosition())
                    && actor.level().isDay() && actor.level().getMaxLocalRawBrightness(actor.blockPosition()) >= 12;
            case "apoli:status_effect" -> hasEffect(actor, condition);
            case "apoli:biome" -> matchesBiome(actor, condition);
            case "apoli:temperature" -> compare(actor.level().getBiome(actor.blockPosition()).value().getBaseTemperature(), condition);
            case "apoli:time_of_day" -> compare(actor.level().getDayTime() % 24000L, condition);
            case "apoli:brightness" -> compare(actor.level().getMaxLocalRawBrightness(actor.blockPosition()) / 15.0D, condition);
            case "apoli:inventory" -> matchesInventory(actor, condition);
            case "apoli:on_block" -> matchesBlock(actor, condition);
            case "apoli:in_block_anywhere" -> matchesBlockAnywhere(actor, condition.getAsJsonObject("block_condition"));
            case "apoli:block_in_radius" -> compare(blockCountInRadius(actor, condition), condition);
            case "apoli:power_active" -> hasPower(actor, condition);
            case "apoli:resource" -> matchesResource(actor, condition);
            case "apoli:air" -> compare(actor.getAirSupply(), condition);
            case "apoli:in_tag" -> target != null && matchesEntityTag(target, condition);
            // `empty` is an item condition; when it reaches the generic interpreter,
            // evaluate the player's main hand instead of silently succeeding.
            case "apoli:empty" -> actor.getMainHandItem().isEmpty();
            case "apoli:fall_distance" -> compare(actor.fallDistance, condition);
            case "apoli:fall_flying" -> actor.isFallFlying();
            case "apoli:creative_flying" -> actor.getAbilities().flying;
            case "apoli:swimming" -> actor.isSwimming();
            case "apoli:riding" -> actor.isPassenger();
            case "apoli:climbing" -> actor.onClimbable();
            case "apoli:health" -> compare(actor.getHealth(), condition);
            case "apoli:armor_value" -> compare(actor.getArmorValue(), condition);
            case "apoli:distance" -> target != null && compare(actor.distanceTo(target), condition);
            case "apoli:on_fire" -> actor.isOnFire();
            case "apoli:collided_horizontally" -> actor.horizontalCollision;
            case "apoli:attacker" -> target != null
                    && (!condition.has("entity_condition")
                    || testEntity(actor, target, condition.getAsJsonObject("entity_condition")));
            case "apoli:raycast" -> raycast(actor, condition);
            case "shape-shifter-curse:barehand_digging" -> barehandDigging(actor);
            case "shape-shifter-curse:chance" -> actor.getRandom().nextFloat()
                    < Math.max(0.0F, Math.min(1.0F, floatValue(condition, "chance", 0.0F)));
            case "shape-shifter-curse:can_render_gui" -> true;
            case "shape-shifter-curse:enable_random_sound" -> SscApi.currentSkin(actor)
                    .map(data -> data.isEnableFormRandomSound()).orElse(true);
            case "shape-shifter-curse:is_item_in_cooldown" -> itemInCooldown(actor, condition);
            case "shape-shifter-curse:last_attack_witch_time" -> compare(
                    lastAttackAge(actor, LastAttackKind.WITCH), condition);
            case "shape-shifter-curse:last_attack_pillager_time" -> compare(
                    lastAttackAge(actor, LastAttackKind.PILLAGER), condition);
            case "shape-shifter-curse:idle_stay" -> idleStay(actor);
            case "apoli:constant" -> !condition.has("value") || condition.get("value").getAsBoolean();
            case "apoli:entity_group" -> matchesEntityGroup(target, stringValue(condition, "group", ""));
            case "apoli:in_block" -> matchesBlockAt(actor, actor.blockPosition(), condition.getAsJsonObject("block_condition"));
            case "apoli:block_collision" -> matchesBlockCollision(actor, condition);
            case "shape-shifter-curse:must_crawling" -> mustCrawl(actor, condition);
            case "shape-shifter-curse:check_accessory" -> checkAccessory(actor, condition);
            case "shape-shifter-curse:has_accessory" -> hasAccessory(actor, condition);
            case "shape-shifter-curse:has_mana" -> FormActivePowerService.hasMana(actor,
                    floatValue(condition, "mana", 0.0F));
            case "shape-shifter-curse:has_mana_percent" -> compare(FormActivePowerService.manaPercent(actor), condition);
            case "shape-shifter-curse:instinct_value" -> compare(InstinctService.value(actor), condition);
            case "shape-shifter-curse:is_sleep" -> actor.isSleeping();
            case "apoli:target_condition" -> target != null && testEntity(actor, target, condition.getAsJsonObject("condition"));
            case "apoli:actor_condition" -> test(actor, target, condition.getAsJsonObject("condition"));
            case "apoli:entity_type" -> target != null && matchesEntityType(target, condition);
            // An unknown condition must not silently grant a power.  This also makes
            // missing Forge handlers visible through the behavior instead of turning
            // them into an always-true condition.
            default -> false;
        };
        return condition.has("inverted") && condition.get("inverted").getAsBoolean() ? !result : result;
    }

    private static boolean barehandDigging(Player actor) {
        ItemStack stack = actor.getMainHandItem();
        if (stack.isEmpty()) return true;
        return stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered
                && tiered.getTier().getLevel() <= 0;
    }

    private static boolean itemInCooldown(Player actor, JsonObject condition) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "item", ""));
        if (id == null) return false;
        net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(id);
        return item != null && actor.getCooldowns().isOnCooldown(item);
    }

    public enum LastAttackKind { WITCH, PILLAGER }

    private static final Map<UUID, Long> LAST_WITCH_ATTACK = new HashMap<>();
    private static final Map<UUID, Long> LAST_PILLAGER_ATTACK = new HashMap<>();
    private static final Map<UUID, Integer> IDLE_STAY_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> IDLE_STAY_LAST_TICK = new HashMap<>();

    public static void recordPlayerAttack(Player player, Entity target) {
        if (target instanceof net.minecraft.world.entity.monster.Witch) {
            LAST_WITCH_ATTACK.put(player.getUUID(), player.level().getGameTime());
        }
        if (target instanceof net.minecraft.world.entity.raid.Raider) {
            LAST_PILLAGER_ATTACK.put(player.getUUID(), player.level().getGameTime());
        }
    }

    private static long lastAttackAge(Player player, LastAttackKind kind) {
        Map<UUID, Long> attacks = kind == LastAttackKind.WITCH ? LAST_WITCH_ATTACK : LAST_PILLAGER_ATTACK;
        Long last = attacks.get(player.getUUID());
        return last == null ? Long.MIN_VALUE / 16 : player.level().getGameTime() - last;
    }

    /** Keeps the custom idle_stay condition in sync with the animation state: five seconds. */
    public static void tickIdleStay(Player player) {
        UUID id = player.getUUID();
        Integer previousTick = IDLE_STAY_LAST_TICK.put(id, player.tickCount);
        if (previousTick != null && previousTick == player.tickCount) return;

        final boolean[] configured = {false};
        FormPowerRegistry.visitActive(player, (powerId, power) -> {
            if (configured[0] || !"shape-shifter-curse:condition_scale".equals(FormPowerRegistry.typeOf(power))) return;
            JsonObject condition = power.getAsJsonObject("condition");
            configured[0] = condition != null
                    && "shape-shifter-curse:idle_stay".equals(FormPowerRegistry.typeOf(condition));
        });
        if (!configured[0]) {
            IDLE_STAY_TICKS.remove(id);
            return;
        }

        boolean idle = player.onGround() && !player.isCrouching() && !player.isPassenger()
                && !player.isSleeping() && !player.isInWaterOrBubble()
                && !player.isUsingItem() && player.getDeltaMovement().lengthSqr() < 1.0E-6D;
        if (idle) IDLE_STAY_TICKS.merge(id, 1, Integer::sum);
        else IDLE_STAY_TICKS.remove(id);
    }

    private static boolean idleStay(Player player) {
        return IDLE_STAY_TICKS.getOrDefault(player.getUUID(), 0) >= 100;
    }

    private static boolean checkAccessory(Player actor, JsonObject condition) {
        if (condition == null) return false;
        String slot = stringValue(condition, "slot", "");
        int slotIndex = condition.has("slot_index") ? condition.get("slot_index").getAsInt() : 0;
        JsonObject ingredientCond = condition.has("condition") ? condition.getAsJsonObject("condition") : null;
        // Try Curios slot lookup
        try {
            var curiosOpt = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(actor).resolve();
            if (curiosOpt.isPresent()) {
                var handler = curiosOpt.get();
                // Try exact slot (e.g., "extra_hand", "belt" etc.)
                var stacksOpt = handler.getStacksHandler(slot);
                if (stacksOpt.isPresent()) {
                    var stacks = stacksOpt.get().getStacks();
                    if (slotIndex >= 0 && slotIndex < stacksOpt.get().getSlots()) {
                        net.minecraft.world.item.ItemStack stack = stacks.getStackInSlot(slotIndex);
                        if (!stack.isEmpty() && ingredientCond != null) {
                            return matchesItem(stack, ingredientCond);
                        } else if (!stack.isEmpty() && ingredientCond == null) {
                            return true;
                        }
                    }
                }
                // Fallback: scan all curios slots for matching ingredient
                if (ingredientCond != null) {
                    for (var entry : handler.getCurios().entrySet()) {
                        var h = entry.getValue();
                        for (int i = 0; i < h.getSlots(); i++) {
                            var s = h.getStacks().getStackInSlot(i);
                            if (!s.isEmpty() && matchesItem(s, ingredientCond)) return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static boolean hasAccessory(Player actor, JsonObject condition) {
        // has_accessory without slot filter: true if any accessory equipped matching ingredient
        if (condition == null) return false;
        JsonObject ingredientCond = condition.has("condition") ? condition.getAsJsonObject("condition") : condition;
        try {
            var curiosOpt = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(actor).resolve();
            if (curiosOpt.isPresent()) {
                var handler = curiosOpt.get();
                for (var entry : handler.getCurios().entrySet()) {
                    var h = entry.getValue();
                    for (int i = 0; i < h.getSlots(); i++) {
                        var s = h.getStacks().getStackInSlot(i);
                        if (!s.isEmpty() && matchesItem(s, ingredientCond)) return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Apoli fluid_height uses the depth intersecting the entity, rather than a boolean
     * in-water check. The Forge port uses the slightly wider 0..0.4 surface window
     * so the launch remains reachable at the vanilla water-surface boundary.
     */
    private static double fluidHeight(Player actor, JsonObject condition) {
        ResourceLocation fluid = ResourceLocation.tryParse(stringValue(condition, "fluid", "minecraft:water"));
        if (fluid == null || "minecraft:water".equals(fluid.toString())) {
            return actor.getFluidHeight(FluidTags.WATER);
        }
        if ("minecraft:lava".equals(fluid.toString())) {
            return actor.getFluidHeight(FluidTags.LAVA);
        }
        return 0.0D;
    }

    public static void execute(Player actor, LivingEntity target, JsonObject action) {
        if (action == null) {
            return;
        }

        String type = FormPowerRegistry.typeOf(action);
        if ("apoli:and".equals(type)) {
            JsonArray actions = action.getAsJsonArray("actions");
            if (actions != null) {
                for (JsonElement child : actions) {
                    if (child.isJsonObject()) {
                        execute(actor, target, child.getAsJsonObject());
                    }
                }
            }
            return;
        }

        LivingEntity recipient = target == null ? actor : target;
        switch (type) {
            case "apoli:and" -> { }
            case "apoli:apply_effect" -> applyEffect(recipient, action.getAsJsonObject("effect"));
            case "apoli:heal" -> recipient.heal(floatValue(action, "amount", 0.0F));
            case "apoli:add_velocity" -> addVelocity(actor, action);
            case "apoli:set_on_fire" -> recipient.setSecondsOnFire(intValue(action, "duration", 1));
            case "apoli:damage" -> recipient.hurt(actor.damageSources().playerAttack(actor), floatValue(action, "amount", 0.0F));
            case "apoli:play_sound" -> playSound(actor, action);
            case "apoli:feed" -> feed(actor, action);
            case "apoli:gain_air" -> actor.setAirSupply(actor.getAirSupply() + intValue(action, "value", 0));
            case "apoli:exhaust" -> actor.causeFoodExhaustion(floatValue(action, "amount", 0.0F));
            case "apoli:consume" -> consumeHeldItem(actor, intValue(action, "amount", 1));
            case "apoli:drop_inventory" -> dropInventory(actor, action);
            case "apoli:equipped_item_action" -> equippedItemAction(actor, recipient, action);
            case "apoli:extinguish" -> recipient.clearFire();
            case "apoli:target_action" -> execute(actor, target, action.getAsJsonObject("action"));
            case "apoli:actor_action" -> execute(actor, actor, action.getAsJsonObject("action"));
            case "apoli:trigger_cooldown" -> triggerCooldown(actor, action);
            case "apoli:modify_resource" -> modifyResource(actor, action);
            case "apoli:execute_command" -> executeCommand(actor, recipient, action);
            case "apoli:spawn_particles" -> spawnParticles(actor, recipient, action);
            case "apoli:spawn_effect_cloud" -> spawnEffectCloud(actor, recipient, action);
            case "apoli:fire_projectile" -> fireProjectile(actor, action);
            case "shape-shifter-curse:invoke_accessory" -> invokeAccessory(actor, action);
            case "shape-shifter-curse:drop_accessory" -> dropAccessory(actor, action);
            case "shape-shifter-curse:set_item_cooldown" -> setItemCooldown(actor, action);
            case "shape-shifter-curse:consume_mana" -> FormActivePowerService.consumeMana(actor,
                    floatValue(action, "mana", 0.0F));
            case "shape-shifter-curse:gain_mana" -> FormActivePowerService.gainMana(actor,
                    floatValue(action, "mana", 0.0F));
            case "shape-shifter-curse:add_instinct" -> InstinctService.add(actor,
                    stringValue(action, "instinct_effect_id", "shape-shifter-curse:unknown"),
                    floatValue(action, "value", 0.0F), intValue(action, "duration", 0), false);
            case "shape-shifter-curse:apply_transformative_effect" -> {
                if (actor instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    ResourceLocation formId = ResourceLocation.tryParse(
                            stringValue(action, "form", stringValue(action, "form_id", "")));
                    if (formId != null) {
                        TransformativeEffectService.apply(serverPlayer, formId);
                    }
                }
            }
            case "shape-shifter-curse:fire_web_bullet" -> WebPowerActions.fireBullet(actor, action);
            case "shape-shifter-curse:web_bridge" -> WebPowerActions.buildBridge(actor, action);
            case "shape-shifter-curse:fire_arrow" -> fireArrow(actor, action);
            case "shape-shifter-curse:explosion_damage_entity" -> explosionDamage(actor, action);
            case "shape-shifter-curse:spawn_particles_in_circle" -> spawnParticlesInCircle(actor, action);
            case "shape-shifter-curse:summon_anubis_wolf_minion", "shape-shifter-curse:bi_summon_anubis_wolf_minion"
                    -> AnubisMinionService.summon(actor, target == null ? actor : target, action);
            case "shape-shifter-curse:set_falling_distance" -> actor.fallDistance = floatValue(action, "distance", 0.0F);
            case "shape-shifter-curse:play_power_animation_with_time" -> playPowerAnimationWithTime(actor, action);
            case "shape-shifter-curse:play_power_animation_with_count" -> playPowerAnimationWithCount(actor, action);
            case "shape-shifter-curse:play_power_animation_loop" -> playPowerAnimationLoop(actor, action);
            case "shape-shifter-curse:stop_power_animation" -> stopPowerAnimation(actor, action);
            default -> {
                // More specialised actions (projectiles, block placement, mana, and custom entities)
                // are intentionally retained in the registry and gain handlers incrementally.
            }
        }
    }

    public static double applyModifier(double value, JsonObject modifier) {
        if (modifier == null) {
            return value;
        }
        double amount = doubleValue(modifier, "value", 0.0D);
        return switch (stringValue(modifier, "operation", "addition")) {
            case "multiply_base", "multiply_total" -> value * (1.0D + amount);
            default -> value + amount;
        };
    }

    /** Small damage-condition subset used by the retained invulnerability powers. */
    public static boolean matchesDamageSource(net.minecraft.world.damagesource.DamageSource source, JsonObject condition) {
        if (condition == null) return true;
        String type = FormPowerRegistry.typeOf(condition);
        boolean result = switch (type) {
            case "apoli:and" -> damageConditions(source, condition.getAsJsonArray("conditions"), true);
            case "apoli:or" -> damageConditions(source, condition.getAsJsonArray("conditions"), false);
            case "apoli:name" -> stringValue(condition, "name", "").equals(source.getMsgId());
            case "apoli:fire" -> source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE);
            default -> false;
        };
        return inverted(condition, result);
    }

    /** Damage conditions need the event amount and source, which ordinary entity conditions do not have. */
    public static boolean testDamageCondition(Player actor, Entity victim,
                                              net.minecraft.world.damagesource.DamageSource source,
                                              float amount, JsonObject condition) {
        if (condition == null) return true;
        String type = FormPowerRegistry.typeOf(condition);
        boolean result = switch (type) {
            case "apoli:and" -> damageConditionList(actor, victim, source, amount,
                    condition.getAsJsonArray("conditions"), true);
            case "apoli:or" -> damageConditionList(actor, victim, source, amount,
                    condition.getAsJsonArray("conditions"), false);
            case "apoli:amount" -> compare(amount, condition);
            case "apoli:attacker" -> source != null && source.getEntity() != null
                    && (!condition.has("entity_condition")
                    || testEntity(actor, source.getEntity(), condition.getAsJsonObject("entity_condition")));
            case "apoli:projectile" -> source != null && source.getDirectEntity() instanceof Projectile;
            default -> test(actor, victim, condition);
        };
        return inverted(condition, result);
    }

    private static boolean damageConditionList(Player actor, Entity victim,
                                               net.minecraft.world.damagesource.DamageSource source,
                                               float amount, JsonArray conditions, boolean all) {
        if (conditions == null) return all;
        for (JsonElement child : conditions) {
            if (!child.isJsonObject()) continue;
            boolean matches = testDamageCondition(actor, victim, source, amount, child.getAsJsonObject());
            if (all != matches) return !all;
        }
        return all;
    }

    private static boolean damageConditions(net.minecraft.world.damagesource.DamageSource source,
                                            JsonArray conditions, boolean all) {
        if (conditions == null) return all;
        for (JsonElement child : conditions) {
            if (!child.isJsonObject()) continue;
            boolean matches = matchesDamageSource(source, child.getAsJsonObject());
            if (all != matches) return !all;
        }
        return all;
    }

    private static boolean testAll(Player actor, Entity target, JsonArray conditions) {
        if (conditions == null) {
            return true;
        }
        for (JsonElement child : conditions) {
            if (child.isJsonObject() && !test(actor, target, child.getAsJsonObject())) {
                return false;
            }
        }
        return true;
    }

    private static boolean testAny(Player actor, Entity target, JsonArray conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }
        for (JsonElement child : conditions) {
            if (child.isJsonObject() && test(actor, target, child.getAsJsonObject())) {
                return true;
            }
        }
        return false;
    }

    private static boolean testEntity(Player actor, Entity target, JsonObject condition) {
        if (condition == null) {
            return true;
        }
        String type = FormPowerRegistry.typeOf(condition);
        boolean result = switch (type) {
            case "apoli:and" -> testEntityAll(actor, target, condition.getAsJsonArray("conditions"));
            case "apoli:or" -> testEntityAny(actor, target, condition.getAsJsonArray("conditions"));
            case "apoli:entity_type" -> matchesEntityType(target, condition);
            case "apoli:in_tag" -> matchesEntityTag(target, condition);
            case "apoli:status_effect" -> target instanceof LivingEntity living && hasEffect(living, condition);
            case "apoli:owner" -> (target instanceof Projectile projectile && projectile.getOwner() == actor)
                    || (target instanceof TamableAnimal tameable && tameable.getOwner() == actor);
            default -> test(actor, target, condition);
        };
        // test() already observes inversion for its fallback branch.
        return !type.equals("apoli:owner") && !type.equals("apoli:entity_type") && !type.equals("apoli:in_tag")
                && !type.equals("apoli:status_effect") && !type.equals("apoli:and") && !type.equals("apoli:or")
                ? result : (condition.has("inverted") && condition.get("inverted").getAsBoolean() ? !result : result);
    }

    private static boolean testEntityAll(Player actor, Entity target, JsonArray conditions) {
        if (conditions == null) return true;
        for (JsonElement child : conditions) if (child.isJsonObject() && !testEntity(actor, target, child.getAsJsonObject())) return false;
        return true;
    }

    private static boolean testEntityAny(Player actor, Entity target, JsonArray conditions) {
        if (conditions == null || conditions.isEmpty()) return false;
        for (JsonElement child : conditions) if (child.isJsonObject() && testEntity(actor, target, child.getAsJsonObject())) return true;
        return false;
    }

    private static boolean compare(double value, JsonObject json) {
        double compared = doubleValue(json, "compare_to", 0.0D);
        return switch (stringValue(json, "comparison", "==")) {
            case ">" -> value > compared;
            case ">=" -> value >= compared;
            case "<" -> value < compared;
            case "<=" -> value <= compared;
            case "!=" -> value != compared;
            default -> value == compared;
        };
    }

    private static boolean hasEffect(LivingEntity actor, JsonObject condition) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "effect", ""));
        MobEffect effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id);
        return effect != null && actor.hasEffect(effect);
    }

    private static boolean matchesBiome(Player actor, JsonObject condition) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "biome", ""));
        var biome = actor.level().getBiome(actor.blockPosition());
        if (id != null) {
            return biome.unwrapKey().map(key -> key.location().equals(id)).orElse(false);
        }
        JsonObject nested = condition.getAsJsonObject("condition");
        return nested == null || matchesBiomeCondition(actor, biome, nested);
    }

    private static boolean matchesBiomeCondition(Player actor, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome,
                                                  JsonObject condition) {
        String type = FormPowerRegistry.typeOf(condition);
        boolean result = switch (type) {
            case "apoli:and" -> testBiomeConditions(actor, biome, condition.getAsJsonArray("conditions"), true);
            case "apoli:or" -> testBiomeConditions(actor, biome, condition.getAsJsonArray("conditions"), false);
            case "apoli:temperature" -> compare(biome.value().getBaseTemperature(), condition);
            case "apoli:in_tag" -> {
                ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "tag", ""));
                yield id != null && biome.is(TagKey.create(Registries.BIOME, id));
            }
            default -> test(actor, actor, condition);
        };
        return inverted(condition, result);
    }

    private static boolean testBiomeConditions(Player actor, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome,
                                               JsonArray conditions, boolean all) {
        if (conditions == null) return all;
        for (JsonElement child : conditions) {
            if (!child.isJsonObject()) continue;
            boolean matches = matchesBiomeCondition(actor, biome, child.getAsJsonObject());
            if (all != matches) return !all;
        }
        return all;
    }

    private static boolean submergedIn(Player actor, JsonObject condition) {
        ResourceLocation fluid = ResourceLocation.tryParse(stringValue(condition, "fluid", "minecraft:water"));
        if (fluid == null || "minecraft:water".equals(fluid.toString())) return actor.isEyeInFluid(FluidTags.WATER);
        if ("minecraft:lava".equals(fluid.toString())) return actor.isEyeInFluid(FluidTags.LAVA);
        return false;
    }

    private static boolean matchesEntityGroup(Entity target, String group) {
        if (!(target instanceof LivingEntity living)) return false;
        return switch (group) {
            case "undead" -> living.getMobType() == net.minecraft.world.entity.MobType.UNDEAD;
            case "arthropod" -> living.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD;
            case "aquatic" -> living.getMobType() == net.minecraft.world.entity.MobType.WATER;
            default -> false;
        };
    }

    private static boolean hasPower(Player actor, JsonObject condition) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "power", ""));
        if (id == null || !FormPowerRegistry.has(actor, id)) return false;
        FormPowerDefinition definition = FormPowerRegistry.all().get(id);
        return definition == null || !"origins:toggle".equals(FormPowerRegistry.typeOf(definition.data()))
                || FormActivePowerService.isToggleActive(actor, id);
    }

    private static boolean matchesResource(Player actor, JsonObject condition) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "resource", ""));
        return id != null && compare(FormActivePowerService.resource(actor, id), condition);
    }

    private static boolean matchesEntityTag(Entity entity, JsonObject condition) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "tag", ""));
        return id != null && entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, id));
    }

    private static boolean matchesEntityType(Entity entity, JsonObject condition) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "entity_type", ""));
        return id != null && BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(id);
    }

    private static boolean matchesInventory(Player actor, JsonObject condition) {
        ItemStack stack = actor.getMainHandItem();
        int matching = stack.isEmpty() ? 0 : (matchesItem(stack, condition.getAsJsonObject("item_condition")) ? 1 : 0);
        return condition.has("comparison") ? compare(matching, condition) : matching > 0;
    }

    public static boolean matchesItem(ItemStack stack, JsonObject condition) {
        if (condition == null) {
            return true;
        }
        boolean result = switch (FormPowerRegistry.typeOf(condition)) {
            case "apoli:and" -> itemConditions(stack, condition.getAsJsonArray("conditions"), true);
            case "apoli:or" -> itemConditions(stack, condition.getAsJsonArray("conditions"), false);
            case "apoli:ingredient" -> {
                JsonObject ingredient = condition.getAsJsonObject("ingredient");
                ResourceLocation itemId = ingredient == null ? null
                        : ResourceLocation.tryParse(stringValue(ingredient, "item", ""));
                ResourceLocation tagId = ingredient == null ? null
                        : ResourceLocation.tryParse(stringValue(ingredient, "tag", ""));
                boolean matches = itemId != null && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId);
                matches |= tagId != null && stack.is(TagKey.create(Registries.ITEM, tagId));
                yield matches;
            }
            case "apoli:enchantment" -> matchesEnchantment(stack, condition);
            case "apoli:meat" -> stack.getItem().isEdible()
                    && stack.getFoodProperties(null) != null
                    && stack.getFoodProperties(null).isMeat();
            case "shape-shifter-curse:is_vegan_ex" -> isVegan(stack, condition);
            case "shape-shifter-curse:is_weapon" -> stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
            case "shape-shifter-curse:is_morph_scale_item", "shape-shifter-curse:is_morph_scale_food"
                    -> isMorphScaleItem(stack);
            case "apoli:armor_value" -> stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor
                    && compare(armor.getDefense(), condition);
            case "apoli:empty" -> stack.isEmpty();
            case "apoli:food" -> stack.isEdible();
            default -> false;
        };
        return inverted(condition, result);
    }

    private static boolean itemConditions(ItemStack stack, JsonArray conditions, boolean all) {
        if (conditions == null) return all;
        for (JsonElement child : conditions) {
            if (!child.isJsonObject()) continue;
            boolean matches = matchesItem(stack, child.getAsJsonObject());
            if (all != matches) return !all;
        }
        return all;
    }

    private static boolean isMorphScaleItem(ItemStack stack) {
        if (!stack.hasTag()) return false;
        var tag = stack.getTag();
        return tag.getBoolean("MorphScale") || tag.getBoolean("morphscale")
                || tag.getBoolean("shape_shifter_curse_morphscale");
    }

    private static boolean isVegan(ItemStack stack, JsonObject condition) {
        boolean fallback = booleanValue(condition, "default", false);
        return stack.hasTag() && stack.getTag().getByte("vegandelight:is_vegan") == 1 || fallback;
    }

    private static boolean inverted(JsonObject condition, boolean value) {
        return condition.has("inverted") && condition.get("inverted").getAsBoolean() ? !value : value;
    }

    private static boolean matchesBlock(Player actor, JsonObject condition) {
        JsonObject blockCondition = condition.getAsJsonObject("block_condition");
        if (blockCondition == null) {
            return actor.onGround();
        }
        return matchesBlockAt(actor, actor.blockPosition().below(), blockCondition);
    }

    private static boolean matchesBlockAt(Player actor, BlockPos pos, JsonObject condition) {
        if (condition == null) return !actor.level().getBlockState(pos).isAir();
        String type = FormPowerRegistry.typeOf(condition);
        BlockState state = actor.level().getBlockState(pos);
        boolean result;
        if ("apoli:in_tag".equals(type)) {
            ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "tag", ""));
            result = id != null && state.is(TagKey.create(Registries.BLOCK, id));
        } else if ("apoli:block".equals(type)) {
            ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "block", ""));
            Block block = id == null ? null : BuiltInRegistries.BLOCK.get(id);
            result = block != null && state.is(block);
        } else if ("apoli:block_state".equals(type)) {
            result = matchesBlockProperty(state, condition);
        } else if ("apoli:hardness".equals(type)) {
            result = compare(state.getDestroySpeed(actor.level(), pos), condition);
        } else if ("apoli:replacable".equals(type)) {
            result = state.canBeReplaced();
        } else if ("apoli:and".equals(type)) {
            result = testBlockList(actor, pos, condition.getAsJsonArray("conditions"), true);
        } else if ("apoli:or".equals(type)) {
            result = testBlockList(actor, pos, condition.getAsJsonArray("conditions"), false);
        } else {
            result = false;
        }
        return inverted(condition, result);
    }

    private static boolean testBlockList(Player actor, BlockPos pos, JsonArray conditions, boolean all) {
        if (conditions == null) return all;
        for (JsonElement child : conditions) {
            if (!child.isJsonObject()) continue;
            boolean matches = matchesBlockAt(actor, pos, child.getAsJsonObject());
            if (all != matches) return !all;
        }
        return all;
    }

    private static boolean matchesBlockProperty(BlockState state, JsonObject condition) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(stringValue(condition, "property", ""));
        if (property == null) return false;
        return property.getValue(stringValue(condition, "value", ""))
                .map(value -> value.equals(state.getValue(property))).orElse(false);
    }

    private static boolean matchesEnchantment(ItemStack stack, JsonObject condition) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "enchantment", ""));
        Enchantment enchantment = id == null ? null : BuiltInRegistries.ENCHANTMENT.get(id);
        return enchantment != null && compare(EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack), condition);
    }

    private static boolean raycast(Player actor, JsonObject condition) {
        double distance = Math.max(0.0D, doubleValue(condition, "distance", 5.0D));
        Vec3 start = actor.getEyePosition();
        Vec3 end = start.add(actor.getLookAngle().scale(distance));
        boolean checkBlock = booleanValue(condition, "block", true);
        boolean checkEntity = booleanValue(condition, "entity", true);
        BlockHitResult blockHit = actor.level().clip(new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, actor));
        double maxDistance = blockHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                ? distance : start.distanceTo(blockHit.getLocation());
        if (checkBlock && blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS
                && matchesBlockState(actor.level(), blockHit.getBlockPos(), condition.getAsJsonObject("block_condition"))) {
            return true;
        }
        if (!checkEntity) return false;

        AABB search = actor.getBoundingBox().expandTowards(actor.getLookAngle().scale(distance)).inflate(1.0D);
        Entity closest = null;
        double closestDistance = maxDistance * maxDistance;
        for (Entity candidate : actor.level().getEntities(actor, search,
                entity -> entity.isPickable() && entity instanceof LivingEntity)) {
            var hit = candidate.getBoundingBox().inflate(0.3D).clip(start, end);
            if (hit.isEmpty()) continue;
            double candidateDistance = start.distanceToSqr(hit.get());
            if (candidateDistance < closestDistance) {
                closest = candidate;
                closestDistance = candidateDistance;
            }
        }
        return closest != null && (!condition.has("hit_bientity_condition")
                || test(actor, closest, condition.getAsJsonObject("hit_bientity_condition")));
    }

    public static boolean matchesBlockState(net.minecraft.world.level.Level level, BlockPos pos, JsonObject condition) {
        if (condition == null) return true;
        String type = FormPowerRegistry.typeOf(condition);
        boolean result;
        if ("apoli:and".equals(type)) {
            result = true;
            for (JsonElement child : condition.getAsJsonArray("conditions")) {
                if (child.isJsonObject() && !matchesBlockState(level, pos, child.getAsJsonObject())) result = false;
            }
        } else if ("apoli:or".equals(type)) {
            result = false;
            for (JsonElement child : condition.getAsJsonArray("conditions")) {
                if (child.isJsonObject() && matchesBlockState(level, pos, child.getAsJsonObject())) result = true;
            }
        } else if ("apoli:in_tag".equals(type)) {
            ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "tag", ""));
            result = id != null && level.getBlockState(pos).is(TagKey.create(Registries.BLOCK, id));
        } else if ("apoli:block".equals(type)) {
            ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "block", ""));
            Block block = id == null ? null : BuiltInRegistries.BLOCK.get(id);
            result = block != null && level.getBlockState(pos).is(block);
        } else if ("apoli:block_state".equals(type)) {
            result = matchesBlockProperty(level.getBlockState(pos), condition);
        } else if ("apoli:hardness".equals(type)) {
            result = compare(level.getBlockState(pos).getDestroySpeed(level, pos), condition);
        } else if ("apoli:replacable".equals(type)) {
            result = level.getBlockState(pos).canBeReplaced();
        } else {
            result = false;
        }
        return inverted(condition, result);
    }

    private static boolean matchesBlockAnywhere(Player actor, JsonObject condition) {
        var box = actor.getBoundingBox();
        BlockPos min = BlockPos.containing(box.minX + 1.0E-4D, box.minY + 1.0E-4D, box.minZ + 1.0E-4D);
        BlockPos max = BlockPos.containing(box.maxX - 1.0E-4D, box.maxY - 1.0E-4D, box.maxZ - 1.0E-4D);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (matchesBlockAt(actor, pos, condition)) return true;
        }
        return false;
    }

    private static int blockCountInRadius(Player actor, JsonObject condition) {
        int radius = Math.max(0, intValue(condition, "radius", 0));
        JsonObject blockCondition = condition.getAsJsonObject("block_condition");
        int count = 0;
        BlockPos origin = actor.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius))) {
            if (matchesBlockAt(actor, pos, blockCondition)) count++;
        }
        return count;
    }

    private static boolean matchesBlockCollision(Player actor, JsonObject condition) {
        if (!actor.horizontalCollision) return false;
        Vec3 direction = actor.getLookAngle();
        BlockPos pos = BlockPos.containing(actor.getX() + direction.x * 0.45D + doubleValue(condition, "offset_x", 0.0D),
                actor.getY() + 0.2D, actor.getZ() + direction.z * 0.45D + doubleValue(condition, "offset_z", 0.0D));
        return matchesBlockAt(actor, pos, condition.getAsJsonObject("block_condition"));
    }

    /**
     * Returns true when the player's current position cannot fit in the
     * dimensions used by the crawling pose.  This is the Forge equivalent of
     * the Fabric must_crawling condition used by the axolotl forms.
     */
    private static boolean mustCrawl(Player actor, JsonObject condition) {
        if (actor.noPhysics || actor.isSpectator() || actor.isPassenger()) {
            return false;
        }

        double width = Math.max(0.0D, doubleValue(condition, "width", 0.6D));
        double height = Math.max(0.0D, doubleValue(condition, "height", 1.5D));
        double halfWidth = width * 0.5D;
        var box = new net.minecraft.world.phys.AABB(
                actor.getX() - halfWidth,
                actor.getY(),
                actor.getZ() - halfWidth,
                actor.getX() + halfWidth,
                actor.getY() + height,
                actor.getZ() + halfWidth).deflate(1.0E-7D);
        return !actor.level().noCollision(actor, box);
    }

    private static void applyEffect(LivingEntity recipient, JsonObject effectData) {
        if (effectData == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(stringValue(effectData, "effect", ""));
        MobEffect effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id);
        if (effect != null) {
            recipient.addEffect(new MobEffectInstance(effect, intValue(effectData, "duration", 0),
                    intValue(effectData, "amplifier", 0), false,
                    !effectData.has("show_particles") || effectData.get("show_particles").getAsBoolean(),
                    !effectData.has("show_icon") || effectData.get("show_icon").getAsBoolean()));
        }
    }

    private static void addVelocity(Player actor, JsonObject action) {
        double x = doubleValue(action, "x", 0.0D);
        double y = doubleValue(action, "y", 0.0D);
        double z = doubleValue(action, "z", 0.0D);
        String space = stringValue(action, "space", "");
        if ("local".equals(space) || "local_horizontal_normalized".equals(space)) {
            Vec3 forward = actor.getLookAngle();
            if ("local_horizontal_normalized".equals(space)) {
                forward = new Vec3(forward.x, 0.0D, forward.z).normalize();
            }
            Vec3 side = new Vec3(forward.z, 0.0D, -forward.x).normalize();
            actor.push(side.x * x + forward.x * z, y, side.z * x + forward.z * z);
        } else {
            actor.push(x, y, z);
        }
    }

    private static void playSound(Player actor, JsonObject action) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(action, "sound", ""));
        SoundEvent sound = id == null ? null : BuiltInRegistries.SOUND_EVENT.get(id);
        if (sound != null) {
            Level level = actor.level();
            level.playSound(null, actor.blockPosition(), sound, actor.getSoundSource(),
                    floatValue(action, "volume", 1.0F), floatValue(action, "pitch", 1.0F));
        }
    }

    private static void feed(Player actor, JsonObject action) {
        int food = intValue(action, "food", 0);
        actor.getFoodData().eat(food, floatValue(action, "saturation", 0.0F));
    }

    /** Item actions in the source data always target the used main-hand stack. */
    public static void consumeHeldItem(Player actor, int amount) {
        if (!actor.getAbilities().instabuild) actor.getMainHandItem().shrink(Math.max(0, amount));
    }

    private static void dropInventory(Player actor, JsonObject action) {
        if (action == null || !action.has("slots") || !action.get("slots").isJsonArray()) return;
        for (JsonElement slot : action.getAsJsonArray("slots")) {
            if (!slot.isJsonPrimitive()) continue;
            ItemStack stack = "weapon.offhand".equals(slot.getAsString())
                    ? actor.getOffhandItem() : actor.getMainHandItem();
            if (stack.isEmpty() || !matchesItem(stack, action.getAsJsonObject("item_condition"))) continue;
            ItemStack dropped = stack.copy();
            if ("weapon.offhand".equals(slot.getAsString())) actor.setItemInHand(
                    net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
            else actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            actor.drop(dropped, false);
        }
    }

    public static boolean matchesHeldItem(Player actor, JsonObject condition) {
        return matchesItem(actor.getMainHandItem(), condition);
    }

    /** Executes the item-side half of Apoli interaction powers against the used main-hand stack. */
    public static void executeHeldItemAction(Player actor, JsonObject action) {
        if (action == null) return;
        ItemStack stack = actor.getMainHandItem();
        switch (FormPowerRegistry.typeOf(action)) {
            case "apoli:consume" -> consumeHeldItem(actor, intValue(action, "amount", 1));
            case "apoli:damage" -> {
                if (!actor.getAbilities().instabuild) {
                    stack.hurtAndBreak(intValue(action, "amount", 1), actor,
                            broken -> broken.broadcastBreakEvent(actor.getUsedItemHand()));
                }
            }
            default -> execute(actor, actor, action);
        }
    }

    private static void equippedItemAction(Player actor, LivingEntity recipient, JsonObject action) {
        String slot = stringValue(action, "equipment_slot", "mainhand");
        ItemStack equipped = switch (slot) {
            case "offhand" -> actor.getOffhandItem();
            case "head" -> actor.getItemBySlot(EquipmentSlot.HEAD);
            case "chest" -> actor.getItemBySlot(EquipmentSlot.CHEST);
            case "legs" -> actor.getItemBySlot(EquipmentSlot.LEGS);
            case "feet" -> actor.getItemBySlot(EquipmentSlot.FEET);
            default -> actor.getMainHandItem();
        };
        if (equipped.isEmpty()) return;
        JsonObject nested = action.getAsJsonObject("action");
        if (nested == null) return;
        if ("apoli:damage".equals(FormPowerRegistry.typeOf(nested))) {
            int amount = Math.max(1, intValue(nested, "amount", 1));
            if (!actor.getAbilities().instabuild) equipped.hurtAndBreak(amount, actor, ignored -> { });
            return;
        }
        execute(actor, recipient, nested);
    }

    private static void invokeAccessory(Player actor, JsonObject action) {
        String mod = stringValue(action, "accessory_mod", "auto");
        String group = stringValue(action, "group", "");
        String slot = stringValue(action, "slot", "");
        int index = intValue(action, "slot_index", 0);
        ItemStack accessory = AccessoryUtils.getEntitySlot(actor, mod, group, slot, index);
        JsonObject nested = action.getAsJsonObject("action");
        if (accessory == null || accessory.isEmpty() || nested == null) return;
        switch (FormPowerRegistry.typeOf(nested)) {
            case "apoli:consume" -> {
                if (!actor.getAbilities().instabuild) accessory.shrink(Math.max(0, intValue(nested, "amount", 1)));
            }
            case "apoli:damage" -> {
                if (!actor.getAbilities().instabuild) accessory.hurtAndBreak(
                        Math.max(1, intValue(nested, "amount", 1)), actor, ignored -> { });
            }
            default -> { }
        }
    }

    private static void dropAccessory(Player actor, JsonObject action) {
        String mod = stringValue(action, "accessory_mod", "auto");
        String group = stringValue(action, "group", "");
        String slot = stringValue(action, "slot", "");
        int index = intValue(action, "slot_index", -1);
        if (index < 0) return;
        ItemStack accessory = AccessoryUtils.getEntitySlot(actor, mod, group, slot, index);
        if (accessory == null || accessory.isEmpty()) return;
        if (!booleanValue(action, "remove", false)) {
            actor.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                    actor.level(), actor.getX(), actor.getY(), actor.getZ(), accessory.copy()));
        }
        AccessoryUtils.setEntitySlot(actor, mod, group, slot, index, ItemStack.EMPTY);
    }

    private static void setItemCooldown(Player actor, JsonObject action) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(action, "item", ""));
        if (id == null) return;
        net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(id);
        if (item != null) actor.getCooldowns().addCooldown(item, Math.max(0, intValue(action, "cooldown", 0)));
    }

    private static void spawnEffectCloud(Player actor, LivingEntity recipient, JsonObject action) {
        if (actor.level().isClientSide) return;
        AreaEffectCloud cloud = new AreaEffectCloud(actor.level(), recipient.getX(), recipient.getY(), recipient.getZ());
        cloud.setOwner(actor);
        cloud.setRadius((float) Math.max(0.0D, doubleValue(action, "radius", 3.0D)));
        cloud.setRadiusOnUse((float) doubleValue(action, "radius_on_use", 0.0D));
        cloud.setWaitTime(Math.max(0, intValue(action, "wait_time", 0)));
        cloud.setDuration(Math.max(1, intValue(action, "duration", 600)));
        JsonObject effect = action.getAsJsonObject("effect");
        if (effect != null) {
            ResourceLocation effectId = ResourceLocation.tryParse(stringValue(effect, "effect", ""));
            MobEffect mobEffect = effectId == null ? null : BuiltInRegistries.MOB_EFFECT.get(effectId);
            if (mobEffect != null) {
                cloud.addEffect(new MobEffectInstance(mobEffect,
                        intValue(effect, "duration", 60), intValue(effect, "amplifier", 0)));
            }
        }
        actor.level().addFreshEntity(cloud);
    }

    private static void triggerCooldown(Player actor, JsonObject action) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(action, "power", ""));
        if (id != null) FormActivePowerService.triggerCooldown(actor, id);
    }

    private static void playPowerAnimationWithTime(Player actor, JsonObject action) {
        if (!(actor instanceof ServerPlayer player) || !actionAllowsServer(action)) return;
        ResourceLocation id = ResourceLocation.tryParse(stringValue(action, "power_animation_id", ""));
        PowerAnimationService.playWithTime(player, id, intValue(action, "animation_time", 0));
    }

    private static void playPowerAnimationWithCount(Player actor, JsonObject action) {
        if (!(actor instanceof ServerPlayer player) || !actionAllowsServer(action)) return;
        ResourceLocation id = ResourceLocation.tryParse(stringValue(action, "power_animation_id", ""));
        PowerAnimationService.playWithCount(player, id, intValue(action, "animation_count", 1));
    }

    private static void playPowerAnimationLoop(Player actor, JsonObject action) {
        if (!(actor instanceof ServerPlayer player) || !actionAllowsServer(action)) return;
        PowerAnimationService.playLoop(player,
                ResourceLocation.tryParse(stringValue(action, "power_animation_id", "")));
    }

    private static void stopPowerAnimation(Player actor, JsonObject action) {
        if (!(actor instanceof ServerPlayer player) || !actionAllowsServer(action)) return;
        if (!action.has("anim_id_list") || !action.get("anim_id_list").isJsonArray()) {
            PowerAnimationService.stop(player);
            return;
        }
        java.util.List<ResourceLocation> ids = new java.util.ArrayList<>();
        for (JsonElement entry : action.getAsJsonArray("anim_id_list")) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
            if (id != null) ids.add(id);
        }
        PowerAnimationService.stopIfMatches(player, ids.toArray(ResourceLocation[]::new));
    }

    private static boolean actionAllowsServer(JsonObject action) {
        return !action.has("can_on_server") || action.get("can_on_server").getAsBoolean();
    }

    private static void modifyResource(Player actor, JsonObject action) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(action, "resource", ""));
        if (id != null) FormActivePowerService.modifyResource(actor, id, action.getAsJsonObject("modifier"));
    }

    private static void executeCommand(Player actor, LivingEntity recipient, JsonObject action) {
        if (!actor.level().isClientSide && actor.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getServer().getCommands().performPrefixedCommand(
                    recipient.createCommandSourceStack().withSuppressedOutput().withPermission(2),
                    stringValue(action, "command", ""));
        }
    }

    private static void spawnParticles(Player actor, LivingEntity recipient, JsonObject action) {
        JsonElement particle = action.get("particle");
        String id = particle != null && particle.isJsonObject()
                ? stringValue(particle.getAsJsonObject(), "type", "minecraft:poof")
                : particle == null ? "minecraft:poof" : particle.getAsString();
        if (particle != null && particle.isJsonObject() && particle.getAsJsonObject().has("params")) {
            id += " " + particle.getAsJsonObject().get("params").getAsString();
        }
        JsonObject spread = action.getAsJsonObject("spread");
        String command = "particle " + id + " ~ ~ ~ " + doubleValue(spread, "x", 0.0D) + " "
                + doubleValue(spread, "y", 0.0D) + " " + doubleValue(spread, "z", 0.0D) + " "
                + doubleValue(action, "speed", 0.0D) + " " + intValue(action, "count", 1)
                + (action.has("force") && action.get("force").getAsBoolean() ? " force" : " normal");
        JsonObject commandAction = new JsonObject();
        commandAction.addProperty("command", command);
        executeCommand(actor, recipient, commandAction);
    }

    private static void fireProjectile(Player actor, JsonObject action) {
        if (actor.level().isClientSide) return;
        int count = Math.max(1, intValue(action, "count", 1));
        String id = stringValue(action, "entity_type", "");
        for (int index = 0; index < count; index++) {
            if ("minecraft:small_fireball".equals(id)) {
                Vec3 look = actor.getLookAngle();
                SmallFireball fireball = new SmallFireball(actor.level(), actor, look.x, look.y, look.z);
                fireball.setPos(actor.getX(), actor.getEyeY() - 0.1D, actor.getZ());
                actor.level().addFreshEntity(fireball);
            } else if ("minecraft:snowball".equals(id)) {
                Snowball snowball = new Snowball(actor.level(), actor);
                snowball.shootFromRotation(actor, actor.getXRot(), actor.getYRot(), 0.0F, 1.5F,
                        floatValue(action, "divergence", 0.0F));
                actor.level().addFreshEntity(snowball);
            }
        }
    }

    private static void fireArrow(Player actor, JsonObject action) {
        if (actor.level().isClientSide) return;
        int count = Math.max(1, intValue(action, "count", 1));
        ArrowItem arrowItem = (ArrowItem) Items.ARROW;
        for (int index = 0; index < count; index++) {
            AbstractArrow arrow = arrowItem.createArrow(actor.level(), new ItemStack(Items.ARROW), actor);
            arrow.setBaseDamage(doubleValue(action, "damage", 2.0D));
            arrow.setCritArrow(action.has("critical") && action.get("critical").getAsBoolean());
            arrow.setNoGravity(action.has("no_gravity") && action.get("no_gravity").getAsBoolean());
            int fireTime = intValue(action, "fire_time", 0);
            if (fireTime > 0) arrow.setSecondsOnFire(fireTime);
            arrow.shootFromRotation(actor, actor.getXRot(), actor.getYRot(), 0.0F,
                    floatValue(action, "speed", 3.0F), floatValue(action, "spread", 0.0F));
            arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            actor.level().addFreshEntity(arrow);
        }
    }

    private static void explosionDamage(Player actor, JsonObject action) {
        if (actor.level().isClientSide) return;
        double radius = Math.max(0.0D, intValue(action, "power", 0) * 2.0D);
        if (radius <= 0.0D) return;
        boolean causesDamage = !action.has("explosion_damage_entity") || action.get("explosion_damage_entity").getAsBoolean();
        double multiplier = doubleValue(action, "damage_multiplier", 1.0D);
        double baseDamage = doubleValue(action, "base_damage", 0.0D);
        for (Entity candidate : actor.level().getEntities(actor, actor.getBoundingBox().inflate(radius))) {
            if (!(candidate instanceof LivingEntity living) || candidate.ignoreExplosion()
                    || !test(actor, candidate, action.getAsJsonObject("entity_condition"))) continue;
            double distance = candidate.position().distanceTo(actor.position());
            if (distance > radius) continue;
            double scale = 1.0D - distance / radius;
            if (causesDamage) {
                float damage = (float) (((scale * scale + scale) * 7.0D * radius + 1.0D) * multiplier + baseDamage);
                living.hurt(actor.damageSources().explosion(actor, actor), damage);
            }
            Vec3 push = candidate.position().subtract(actor.position()).normalize().scale(scale);
            candidate.push(push.x, Math.max(0.1D, push.y), push.z);
            execute(actor, living, action.getAsJsonObject("entity_action"));
        }
    }

    private static void spawnParticlesInCircle(Player actor, JsonObject action) {
        if (!(actor.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        JsonElement particle = action.get("particle");
        String particleId = particle != null && particle.isJsonObject()
                ? stringValue(particle.getAsJsonObject(), "type", "minecraft:poof")
                : particle == null ? "minecraft:poof" : particle.getAsString();
        JsonObject spread = action.getAsJsonObject("spread");
        int samples = Math.max(1, intValue(action, "sample_count", 8));
        double radius = doubleValue(action, "radius", 1.0D);
        for (int sample = 0; sample < samples; sample++) {
            double angle = Math.PI * 2.0D * sample / samples;
            double x = actor.getX() + doubleValue(action, "offset_x", 0.0D) + Math.cos(angle) * radius;
            double y = actor.getY() + doubleValue(action, "offset_y", 0.5D);
            double z = actor.getZ() + doubleValue(action, "offset_z", 0.0D) + Math.sin(angle) * radius;
            String command = "execute positioned " + x + " " + y + " " + z + " run particle " + particleId
                    + " ~ ~ ~ " + doubleValue(spread, "x", 0.5D) + " " + doubleValue(spread, "y", 0.5D)
                    + " " + doubleValue(spread, "z", 0.5D) + " " + doubleValue(action, "speed", 0.0D)
                    + " " + intValue(action, "count", 1) + (action.has("force") && action.get("force").getAsBoolean() ? " force" : " normal");
            serverLevel.getServer().getCommands().performPrefixedCommand(actor.createCommandSourceStack()
                    .withSuppressedOutput().withPermission(2), command);
        }
    }

    public static String stringValue(JsonObject json, String key, String fallback) {
        return json != null && json.has(key) ? json.get(key).getAsString() : fallback;
    }

    public static int intValue(JsonObject json, String key, int fallback) {
        return json != null && json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    public static boolean booleanValue(JsonObject json, String key, boolean fallback) {
        return json != null && json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    public static float floatValue(JsonObject json, String key, float fallback) {
        return json != null && json.has(key) ? json.get(key).getAsFloat() : fallback;
    }

    public static double doubleValue(JsonObject json, String key, double fallback) {
        return json != null && json.has(key) ? json.get(key).getAsDouble() : fallback;
    }
}
