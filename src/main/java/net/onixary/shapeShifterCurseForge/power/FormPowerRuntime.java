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
import net.minecraft.world.item.ItemStack;
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
import net.onixary.shapeShifterCurseForge.api.PlayerSkinData;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;
import net.onixary.shapeShifterCurseForge.util.Accessory.AccessoryUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Shared condition and action interpreter for the common Origins JSON building blocks.
 *
 */
@SuppressWarnings("deprecation")
public final class FormPowerRuntime {
    private FormPowerRuntime() {
    }

    private static boolean exposedToSun(Entity entity) {
        if (!entity.level().isDay()) return false;
        if (entity.level().isClientSide) entity.level().updateSkyBrightness();
        BlockPos rainPos = entity.blockPosition();
        boolean inRain = entity.level().isRainingAt(rainPos)
                || entity.level().isRainingAt(BlockPos.containing(
                        rainPos.getX(), entity.getBoundingBox().maxY, rainPos.getZ()));
        BlockPos feet = BlockPos.containing(entity.getX(), entity.getBoundingBox().minY, entity.getZ());
        return !inRain && entity.level().getLightLevelDependentMagicValue(feet) > 0.5F
                && entity.level().canSeeSky(feet);
    }

    public static boolean test(Player actor, Entity target, JsonObject condition) {
        if (condition == null) {
            return true;
        }

        String type = FormPowerRegistry.typeOf(condition);
        ResourceLocation typeId = ResourceLocation.tryParse(type);
        Boolean javaResult = typeId == null ? null
                : SscJavaRegistries.testCondition(typeId, actor, target, condition);
        boolean result = javaResult != null ? javaResult : switch (type) {
            case "apoli:and" -> testAll(actor, target, condition.getAsJsonArray("conditions"));
            case "apoli:or" -> testAny(actor, target, condition.getAsJsonArray("conditions"));
            // Apoli's EntityConditions.sneaking reads only the shared Shift flag.
            // A swimming/crawling pose alone must not enable sneaking powers.
            case "apoli:sneaking" -> actor.isShiftKeyDown();
            case "apoli:sprinting" -> actor.isSprinting();
            case "apoli:on_ground" -> actor.onGround();
            case "apoli:moving" -> moving(actor, condition);
            case "apoli:food_level" -> compare(actor.getFoodData().getFoodLevel(), condition);
            case "apoli:fluid_height" -> compare(fluidHeight(actor, condition), condition);
            case "apoli:submerged_in" -> submergedIn(actor, condition);
            case "apoli:exposed_to_sun" -> exposedToSun(actor);
            case "apoli:status_effect" -> hasEffect(actor, condition);
            case "apoli:biome" -> matchesBiome(actor, condition);
            case "apoli:temperature" -> compare(actor.level().getBiome(actor.blockPosition()).value().getBaseTemperature(), condition);
            case "apoli:time_of_day" -> compare(actor.level().getDayTime() % 24000L, condition);
            case "apoli:brightness" -> {
                if (actor.level().isClientSide) actor.level().updateSkyBrightness();
                yield compare(actor.level().getLightLevelDependentMagicValue(
                        BlockPos.containing(actor.getX(), actor.getEyeY(), actor.getZ())), condition);
            }
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
            case "apoli:enchantment" -> matchesEnchantment(actor, condition);
            case "apoli:distance" -> target != null && compare(actor.distanceTo(target), condition);
            case "apoli:on_fire" -> actor.isOnFire();
            case "apoli:collided_horizontally" -> actor.horizontalCollision;
            case "apoli:attacker" -> target != null
                    && (!condition.has("entity_condition")
                    || testEntity(actor, target, condition.getAsJsonObject("entity_condition")));
            case "apoli:raycast" -> raycast(actor, condition);
            case "shape-shifter-curse:barehand_digging" -> barehandDigging(actor);
            case "shape-shifter-curse:chance" -> new java.util.Random().nextFloat()
                    < floatValue(condition, "chance", 0.0F);
            case "shape-shifter-curse:can_render_gui" -> canRenderGui();
            case "shape-shifter-curse:enable_random_sound" -> SscApi.currentSkin(actor)
                    .map(PlayerSkinData::isEnableFormRandomSound).orElse(true);
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
            case "shape-shifter-curse:check_stored_item" -> ItemStoreService.check(
                    target instanceof Player targetPlayer ? targetPlayer : actor, condition);
            case "apoli:target_condition" -> target != null && testEntity(actor, target, condition.getAsJsonObject("condition"));
            case "apoli:actor_condition" -> test(actor, target, condition.getAsJsonObject("condition"));
            case "apoli:entity_type" -> target != null && matchesEntityType(target, condition);
            // An unknown condition must not silently grant a power.  This also makes
            // missing Forge handlers visible through the behavior instead of turning
            // them into an always-true condition.
            default -> false;
        };
        return (condition.has("inverted") && condition.get("inverted").getAsBoolean()) != result;
    }

    /** Apoli's moving condition honours per-axis flags (both default true). */
    private static boolean moving(Player actor, JsonObject condition) {
        boolean horizontally = !condition.has("horizontally") || condition.get("horizontally").getAsBoolean();
        boolean vertically = !condition.has("vertically") || condition.get("vertically").getAsBoolean();
        net.minecraft.world.phys.Vec3 velocity = actor.getDeltaMovement();
        boolean movingHorizontally = velocity.x * velocity.x + velocity.z * velocity.z > 0.0004D;
        boolean movingVertically = Math.abs(velocity.y) > 0.0004D;
        return (horizontally && movingHorizontally) || (vertically && movingVertically);
    }

    /** Apoli's entity enchantment condition reads the enchantment's equipment slots. */
    private static boolean matchesEnchantment(Player actor, JsonObject condition) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "enchantment", ""));
        if (id == null) return false;
        Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(id);
        if (enchantment == null || !BuiltInRegistries.ENCHANTMENT.containsKey(id)) return false;
        int level = 0;
        if ("max".equals(stringValue(condition, "calculation", "sum"))) {
            level = EnchantmentHelper.getEnchantmentLevel(enchantment, actor);
        } else if ("sum".equals(stringValue(condition, "calculation", "sum"))) {
            for (ItemStack stack : enchantment.getSlotItems(actor).values()) {
                level += EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
            }
        } else {
            return false;
        }
        return compare(level, condition);
    }

    /**
     * Fabric's {@code can_render_gui} returns the client HUD preference on the client and true on
     * the server. The client-only lookup is reached only after the dist check, so it is never
     * loaded on a dedicated server.
     */
    private static boolean canRenderGui() {
        if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            return true;
        }
        return net.onixary.shapeShifterCurseForge.client.ClientHudState.canDisplayGui();
    }

    private static boolean barehandDigging(Player actor) {        ItemStack stack = actor.getMainHandItem();
        if (stack.isEmpty()) return true;
        return stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered
                && tiered.getTier().getLevel() <= 0;
    }

    private static boolean itemInCooldown(Player actor, JsonObject condition) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "item", ""));
        if (id == null) return false;
        net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(id);
        return actor.getCooldowns().isOnCooldown(item);
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
        // use safe sentinel <2^53 to avoid double precision loss (was Long.MIN_VALUE/16)
        return last == null ? 1_000_000_000L : player.level().getGameTime() - last;
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
        String mod = stringValue(condition, "accessory_mod", "auto");
        String group = stringValue(condition, "group", "");
        String slot = stringValue(condition, "slot", "");
        int slotIndex = condition.has("slot_index") ? condition.get("slot_index").getAsInt() : 0;
        JsonObject ingredientCond = condition.has("condition") ? condition.getAsJsonObject("condition") : null;
        // The backend is optional. AccessoryUtils safely returns null when Curios
        // (or another accessory provider) is not installed.
        ItemStack stack = AccessoryUtils.getEntitySlot(actor, mod, group, slot, slotIndex);
        if (stack != null && !stack.isEmpty()) {
            return ingredientCond == null || matchesItem(stack, ingredientCond);
        }
        if (ingredientCond == null) return false;

        var allSlots = AccessoryUtils.getEntitySlots(actor, mod);
        if (allSlots != null) {
            for (var stacks : allSlots.values()) {
                for (ItemStack candidate : stacks) {
                    if (!candidate.isEmpty() && matchesItem(candidate, ingredientCond)) return true;
                }
            }
        }
        return false;
    }

    private static boolean hasAccessory(Player actor, JsonObject condition) {
        // has_accessory without slot filter: true if any accessory equipped matching ingredient
        if (condition == null) return false;
        String mod = stringValue(condition, "accessory_mod", "auto");
        JsonObject ingredientCond = condition.has("condition") ? condition.getAsJsonObject("condition") : condition;
        var allSlots = AccessoryUtils.getEntitySlots(actor, mod);
        if (allSlots != null) {
            for (var stacks : allSlots.values()) {
                for (ItemStack stack : stacks) {
                    if (!stack.isEmpty() && matchesItem(stack, ingredientCond)) return true;
                }
            }
        }
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
        ResourceLocation typeId = ResourceLocation.tryParse(type);
        if (typeId != null && SscJavaRegistries.executeAction(typeId, actor, recipient, action)) {
            return;
        }
        switch (type) {
            case "apoli:apply_effect" -> applyEffect(recipient, action.getAsJsonObject("effect"));
            case "apoli:heal" -> recipient.heal(floatValue(action, "amount", 0.0F));
            case "apoli:add_velocity" -> addVelocity(recipient, action);
            case "apoli:set_on_fire" -> recipient.setSecondsOnFire(intValue(action, "duration", 1));
            case "apoli:damage" -> dealActionDamage(actor, recipient, action);
            case "apoli:play_sound" -> playSound(recipient, action);
            case "apoli:feed" -> feed(actor, action);
            case "apoli:gain_air" -> recipient.setAirSupply(recipient.getAirSupply() + intValue(action, "value", 0));
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
            case "shape-shifter-curse:tan_add_thirst" ->
                    net.onixary.shapeShifterCurseForge.integration.toughasnails.ToughAsNailsIntegration
                            .addThirst(actor, intValue(action, "amount", 0));
            case "shape-shifter-curse:gain_store_power_item",
                 "shape-shifter-curse:drop_store_power_item",
                 "shape-shifter-curse:swap_store_power_item",
                 "shape-shifter-curse:invoke_store_power_item" -> ItemStoreService.execute(
                    recipient instanceof Player targetPlayer ? targetPlayer : actor, action);
            default -> {
                // Unhandled action types are warned about once and ignored instead of being executed.
                if (!type.isBlank() && WARNED_ACTIONS.add(type)) {
                    LOGGER.warn("[ssc-power] No handler for action type '{}'; the action is ignored.", type);
                }
            }
        }
    }

    /** Apoli bi-entity actions keep actor and target separate; entity actions do not. */
    public static void executeBiEntity(Player actor, LivingEntity target, JsonObject action) {
        if (action == null) return;
        switch (FormPowerRegistry.typeOf(action)) {
            case "apoli:and" -> {
                JsonArray children = action.getAsJsonArray("actions");
                if (children != null) for (JsonElement child : children) {
                    if (child.isJsonObject()) executeBiEntity(actor, target, child.getAsJsonObject());
                }
            }
            case "apoli:actor_action" -> execute(actor, actor, action.getAsJsonObject("action"));
            case "apoli:target_action" -> execute(actor, target, action.getAsJsonObject("action"));
            case "apoli:add_velocity" -> {
                if ((target.level().isClientSide && !booleanValue(action, "client", true))
                        || (!target.level().isClientSide && !booleanValue(action, "server", true))) return;
                Vec3 forward = target.position().subtract(actor.position());
                if (forward.length() <= 0.007D) return;
                forward = forward.normalize();
                double sideX;
                double sideZ;
                if (Math.abs(forward.y) != 1.0D) {
                    double factor = 1.0D / Math.sqrt(forward.x * forward.x + forward.z * forward.z);
                    sideX = forward.z * factor;
                    sideZ = -forward.x * factor;
                } else {
                    float yaw = -actor.getYRot() * 0.0174532925F;
                    sideX = net.minecraft.util.Mth.cos(yaw);
                    sideZ = -net.minecraft.util.Mth.sin(yaw);
                }
                double x = doubleValue(action, "x", 0.0D);
                double y = doubleValue(action, "y", 0.0D);
                double z = doubleValue(action, "z", 0.0D);
                double vx = sideX * x + forward.y * sideZ * y + forward.x * z;
                double vy = (forward.z * sideX - forward.x * sideZ) * y + forward.y * z;
                double vz = sideZ * x - forward.y * sideX * y + forward.z * z;
                if (booleanValue(action, "set", false)) target.setDeltaMovement(vx, vy, vz);
                else target.push(vx, vy, vz);
                target.hurtMarked = true;
            }
            default -> { }
        }
    }

    /** Unknown action types already reported, so the warning is emitted at most once per type. */
    private static final java.util.Set<String> WARNED_ACTIONS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(FormPowerRuntime.class);

    /** Apoli modifier operations in application order ((base phase, order) then (total phase, order)). */
    private static final java.util.List<String> MODIFIER_ORDER = java.util.List.of(
            "add_base_early", "multiply_base_additive", "multiply_base_multiplicative", "add_base_late",
            "min_base", "max_base", "set_base",
            "multiply_total_additive", "multiply_total_multiplicative", "add_total_late",
            "min_total", "max_total", "set_total");
    private static final java.util.Set<String> BASE_PHASE_OPS = java.util.Set.of(
            "add_base_early", "multiply_base_additive", "multiply_base_multiplicative", "add_base_late",
            "min_base", "max_base", "set_base");

    /** Legacy Apoli aliases for the three vanilla attribute operations. */
    private static String normalizeOperation(String operation) {
        return switch (operation) {
            case "addition" -> "add_base_early";
            case "multiply_base" -> "multiply_base_additive";
            case "multiply_total" -> "multiply_total_multiplicative";
            default -> operation;
        };
    }

    public static double applyModifier(double value, JsonObject modifier) {
        if (modifier == null) {
            return value;
        }
        return applyModifierList(value, java.util.List.of(modifier));
    }

    /**
     * Apoli's full modifier pipeline (ModifierUtil.applyModifiers): modifiers are grouped by
     * operation, then applied in (phase, order) order with the base reset between the BASE and
     * TOTAL phases. This differs from naively folding every modifier in list order.
     */
    public static double applyModifierList(double baseValue, java.util.List<JsonObject> modifiers) {
        return applyModifierList(null, baseValue, modifiers);
    }

    public static double applyModifierList(Player entity, double baseValue, java.util.List<JsonObject> modifiers) {
        // TODO[TEST] Mirrors Apoli's ModifierUtil phase/order pipeline; not yet verified in-game
        //   against mixed-operation modifier lists.
        if (modifiers == null || modifiers.isEmpty()) {
            return baseValue;
        }
        java.util.Map<String, java.util.List<Double>> buckets = new java.util.LinkedHashMap<>();
        for (JsonObject modifier : modifiers) {
            if (modifier == null) continue;
            String op = normalizeOperation(stringValue(modifier, "operation", "addition"));
            buckets.computeIfAbsent(op, ignored -> new java.util.ArrayList<>())
                    .add(resolveModifierValue(entity, modifier));
        }
        double currentBase = baseValue;
        double currentValue = baseValue;
        boolean basePhase = true;
        for (String op : MODIFIER_ORDER) {
            java.util.List<Double> values = buckets.get(op);
            if (values == null) continue;
            boolean opIsBase = BASE_PHASE_OPS.contains(op);
            if (opIsBase != basePhase) {
                currentBase = currentValue;
                basePhase = opIsBase;
            }
            currentValue = applyOperation(op, values, currentBase, currentValue);
        }
        for (java.util.Map.Entry<String, java.util.List<Double>> entry : buckets.entrySet()) {
            if (MODIFIER_ORDER.contains(entry.getKey())) continue;
            for (double value : entry.getValue()) currentValue += value;
        }
        return currentValue;
    }

    /**
     * Apoli resolves a modifier's value from a referenced power "resource" (variable/cooldown power)
     * and then applies any nested "modifier" list to that value.
     */
    private static double resolveModifierValue(Player entity, JsonObject modifier) {
        double value = doubleValue(modifier, "value", 0.0D);
        if (modifier.has("resource")) {
            ResourceLocation id = ResourceLocation.tryParse(stringValue(modifier, "resource", ""));
            if (id != null && entity != null && FormPowerRegistry.has(entity, id)) {
                value = FormActivePowerService.resource(entity, id);
            }
        }
        if (modifier.has("modifier") && modifier.get("modifier").isJsonArray()) {
            java.util.List<JsonObject> nested = new java.util.ArrayList<>();
            for (JsonElement entry : modifier.getAsJsonArray("modifier")) {
                if (entry.isJsonObject()) nested.add(entry.getAsJsonObject());
            }
            value = applyModifierList(entity, value, nested);
        }
        return value;
    }

    private static double applyOperation(String operation, java.util.List<Double> values, double base, double current) {
        switch (operation) {
            case "add_base_early" -> {
                double value = base;
                for (double v : values) value += v;
                return value;
            }
            case "multiply_base_additive", "multiply_total_additive" -> {
                double value = current;
                for (double v : values) value += base * v;
                return value;
            }
            case "multiply_base_multiplicative", "multiply_total_multiplicative" -> {
                double value = current;
                for (double v : values) value *= (1.0D + v);
                return value;
            }
            case "min_base", "min_total" -> {
                double value = current;
                for (double v : values) value = Math.max(v, value);
                return value;
            }
            case "max_base", "max_total" -> {
                double value = current;
                for (double v : values) value = Math.min(v, value);
                return value;
            }
            case "set_base", "set_total", "add_total_late" -> {
                double value = current;
                for (double v : values) value = v;
                return value;
            }
            default -> {
                double value = current;
                for (double v : values) value += v;
                return value;
            }
        }
    }

    /** Apoli's full damage-condition set. */
    public static boolean matchesDamageSource(net.minecraft.world.damagesource.DamageSource source, JsonObject condition) {
        if (condition == null) return true;
        if (source == null) return false;
        String type = FormPowerRegistry.typeOf(condition);
        boolean result = switch (type) {
            case "apoli:and" -> damageConditions(source, condition.getAsJsonArray("conditions"), true);
            case "apoli:or" -> damageConditions(source, condition.getAsJsonArray("conditions"), false);
            case "apoli:name" -> stringValue(condition, "name", "").equals(source.getMsgId());
            case "apoli:fire" -> source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE);
            case "apoli:projectile" -> source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE);
            case "apoli:bypasses_armor" -> source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR);
            case "apoli:explosive" -> source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
            case "apoli:from_falling" -> source.is(net.minecraft.tags.DamageTypeTags.IS_FALL);
            case "apoli:unblockable" -> source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD);
            case "apoli:out_of_world" -> source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
            case "apoli:in_tag" -> {
                ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "tag", ""));
                yield id != null && source.is(TagKey.create(Registries.DAMAGE_TYPE, id));
            }
            case "apoli:type" -> {
                ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "damage_type", ""));
                yield id != null && source.is(net.minecraft.resources.ResourceKey.create(
                        Registries.DAMAGE_TYPE, id));
            }
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
        if ("apoli:and".equals(type)) {
            return inverted(condition, damageConditionList(actor, victim, source, amount,
                    condition.getAsJsonArray("conditions"), true));
        }
        if ("apoli:or".equals(type)) {
            return inverted(condition, damageConditionList(actor, victim, source, amount,
                    condition.getAsJsonArray("conditions"), false));
        }
        if ("apoli:amount".equals(type)) {
            return inverted(condition, compare(amount, condition));
        }
        if ("apoli:attacker".equals(type)) {
            return inverted(condition, source != null && source.getEntity() != null
                    && (!condition.has("entity_condition")
                    || testEntity(actor, source.getEntity(), condition.getAsJsonObject("entity_condition"))));
        }
        if ("apoli:projectile".equals(type)) {
            return inverted(condition, testProjectileDamageCondition(actor, source, condition));
        }
        if ("apoli:fire".equals(type)) {
            return inverted(condition, source != null && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE));
        }
        if ("apoli:bypasses_armor".equals(type)) {
            return inverted(condition, source != null && source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR));
        }
        if ("apoli:explosive".equals(type)) {
            return inverted(condition, source != null && source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION));
        }
        if ("apoli:from_falling".equals(type)) {
            return inverted(condition, source != null && source.is(net.minecraft.tags.DamageTypeTags.IS_FALL));
        }
        if ("apoli:unblockable".equals(type)) {
            return inverted(condition, source != null && source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD));
        }
        if ("apoli:out_of_world".equals(type)) {
            return inverted(condition, source != null
                    && source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY));
        }
        if ("apoli:name".equals(type)) {
            return inverted(condition, source != null && stringValue(condition, "name", "").equals(source.getMsgId()));
        }
        if ("apoli:in_tag".equals(type)) {
            ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "tag", ""));
            return inverted(condition, source != null && id != null
                    && source.is(TagKey.create(Registries.DAMAGE_TYPE, id)));
        }
        if ("apoli:type".equals(type)) {
            ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "damage_type", ""));
            return inverted(condition, source != null && id != null
                    && source.is(net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, id)));
        }
        // Unknown damage condition: delegate to the generic interpreter, which applies inversion itself.
        return test(actor, victim, condition);
    }

    private static boolean testProjectileDamageCondition(Player actor,
                                                         net.minecraft.world.damagesource.DamageSource source,
                                                         JsonObject condition) {
        if (source == null || !source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
            return false;
        }
        Entity projectile = source.getDirectEntity();
        if (projectile == null) {
            return false;
        }
        if (condition.has("projectile")) {
            ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "projectile", ""));
            if (id == null || !BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType()).equals(id)) {
                return false;
            }
        }
        return !condition.has("projectile_condition")
                || test(actor, projectile, condition.getAsJsonObject("projectile_condition"));
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
                ? result : ((condition.has("inverted") && condition.get("inverted").getAsBoolean()) != result);
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

    private static final double EPS = 1e-6;
    public static boolean compare(double value, JsonObject json) {
        double compared = doubleValue(json, "compare_to", 0.0D);
        return switch (stringValue(json, "comparison", "==")) {
            case ">" -> value > compared;
            case ">=" -> value >= compared - EPS;
            case "<" -> value < compared;
            case "<=" -> value <= compared + EPS;
            case "!=" -> Math.abs(value - compared) > EPS;
            default -> Math.abs(value - compared) <= EPS;
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
        if ("apoli:and".equals(type)) {
            return inverted(condition, testBiomeConditions(actor, biome, condition.getAsJsonArray("conditions"), true));
        }
        if ("apoli:or".equals(type)) {
            return inverted(condition, testBiomeConditions(actor, biome, condition.getAsJsonArray("conditions"), false));
        }
        if ("apoli:temperature".equals(type)) {
            return inverted(condition, compare(biome.value().getBaseTemperature(), condition));
        }
        if ("apoli:in_tag".equals(type)) {
            ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "tag", ""));
            return inverted(condition, id != null && biome.is(TagKey.create(Registries.BIOME, id)));
        }
        // Unknown biome condition: delegate to the generic interpreter, which applies inversion itself.
        return test(actor, actor, condition);
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
        // SSC marks a player's artificial entity group with marker powers (undead_group,
        // aquatic, form_spider_entity_group); Apoli's entity_group condition sees those.
        if (target instanceof Player player && playerGroupMarker(player, group)) {
            return true;
        }
        if (!(target instanceof LivingEntity living)) return false;
        return switch (group) {
            case "undead" -> living.getMobType() == net.minecraft.world.entity.MobType.UNDEAD;
            case "arthropod" -> living.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD;
            case "aquatic" -> living.getMobType() == net.minecraft.world.entity.MobType.WATER;
            case "illager" -> living.getMobType() == net.minecraft.world.entity.MobType.ILLAGER;
            default -> false;
        };
    }

    private static boolean playerGroupMarker(Player player, String group) {
        String marker = switch (group) {
            case "undead" -> "undead_group";
            case "arthropod" -> "form_spider_entity_group";
            case "aquatic" -> "aquatic";
            default -> null;
        };
        return marker != null && FormPowerRegistry.has(player, ResourceLocation.fromNamespaceAndPath(
                net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge.RESOURCE_NAMESPACE, marker));
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
        JsonObject itemCondition = condition.getAsJsonObject("item_condition");
        boolean countItems = !"stacks".equals(stringValue(condition, "process_mode", "items"));
        int matching = 0;
        for (ItemStack stack : inventoryStacks(actor, condition)) {
            if (stack.isEmpty()) continue;
            if (itemCondition != null && !matchesItem(stack, itemCondition)) continue;
            matching += countItems ? stack.getCount() : 1;
        }
        return condition.has("comparison") ? compare(matching, condition) : matching > 0;
    }

    /** Apoli inventory "slots" (inventory type) selection, subset. */
    private static java.util.List<ItemStack> inventoryStacks(Player actor, JsonObject condition) {
        java.util.List<String> slots = new java.util.ArrayList<>();
        if (condition.has("slots") && condition.get("slots").isJsonArray()) {
            for (JsonElement entry : condition.getAsJsonArray("slots")) {
                if (entry.isJsonPrimitive()) slots.add(entry.getAsString());
            }
        }
        if (slots.isEmpty()) slots.add("inventory");
        java.util.List<ItemStack> result = new java.util.ArrayList<>();
        for (String slot : slots) {
            switch (slot) {
                case "weapon.mainhand" -> result.add(actor.getMainHandItem());
                case "weapon.offhand" -> result.add(actor.getOffhandItem());
                case "inventory.hotbar" -> {
                    for (int i = 0; i < 9; i++) result.add(actor.getInventory().getItem(i));
                }
                case "inventory.ender_chest" -> {
                    for (int i = 0; i < actor.getEnderChestInventory().getContainerSize(); i++) {
                        result.add(actor.getEnderChestInventory().getItem(i));
                    }
                }
                default -> {
                    for (int i = 0; i < actor.getInventory().getContainerSize(); i++) {
                        result.add(actor.getInventory().getItem(i));
                    }
                }
            }
        }
        return result;
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
                    && Objects.requireNonNull(stack.getFoodProperties(null)).isMeat();
            case "shape-shifter-curse:is_vegan_ex" -> isVegan(stack, condition);
            case "shape-shifter-curse:is_weapon" -> isWeapon(stack);
            case "shape-shifter-curse:is_morph_scale_item", "shape-shifter-curse:is_morph_scale_food"
                    -> isMorphScaleItem(stack);
            case "apoli:armor_value" -> stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor
                    && compare(armor.getDefense(), condition);
            case "apoli:harvest_level" -> {
                // Apoli: a tool's mining level, 0 for anything that is not a tool.
                int level = stack.getItem() instanceof net.minecraft.world.item.TieredItem tiered
                        ? tiered.getTier().getLevel() : 0;
                yield compare(level, condition);
            }
            case "apoli:empty" -> stack.isEmpty();
            case "apoli:food" -> stack.isEdible();
            // Item conditions outside the handled subset evaluate false.
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
        // Fabric marks morphscale items with the shape-shifter-curse:morph_scale_item tag.
        if (stack.is(TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("shape-shifter-curse", "morph_scale_item")))) {
            return true;
        }
        // Custom items may still opt in through NBT.
        if (!stack.hasTag()) return false;
        var tag = stack.getTag();
        return Objects.requireNonNull(tag).getBoolean("MorphScale") || tag.getBoolean("morphscale")
                || tag.getBoolean("shape_shifter_curse_morphscale");
    }

    /** Fabric's is_weapon checks for an attack-damage modifier rather than a specific item class. */
    private static boolean isWeapon(ItemStack stack) {
        return stack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                .containsKey(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
    }

    private static boolean isVegan(ItemStack stack, JsonObject condition) {
        boolean fallback = booleanValue(condition, "default", false);
        if (!stack.hasTag()) return fallback;
        byte v = Objects.requireNonNull(stack.getTag()).getByte("vegandelight:is_vegan");
        // tag present: 1 => vegan, 0 => not vegan, missing => fallback
        if (stack.getTag().contains("vegandelight:is_vegan")) return v == 1;
        return fallback;
    }

    private static boolean inverted(JsonObject condition, boolean value) {
        return (condition.has("inverted") && condition.get("inverted").getAsBoolean()) != value;
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
        return enchantment != null && compare(EnchantmentHelper.getTagEnchantmentLevel(enchantment, stack), condition);
    }

    private static boolean raycast(Player actor, JsonObject condition) {
        double distance = Math.max(0.0D, doubleValue(condition, "distance", 5.0D));
        Vec3 start = actor.getEyePosition();
        Vec3 end = start.add(actor.getLookAngle().scale(distance));
        boolean checkBlock = booleanValue(condition, "block", true);
        boolean checkEntity = booleanValue(condition, "entity", true);
        ClipContext.Block shape = switch (stringValue(condition, "shape_type", "outline")) {
            case "collider" -> ClipContext.Block.COLLIDER;
            case "visual" -> ClipContext.Block.VISUAL;
            default -> ClipContext.Block.OUTLINE;
        };
        ClipContext.Fluid fluid = switch (stringValue(condition, "fluid_handling", "none")) {
            case "any" -> ClipContext.Fluid.ANY;
            case "source_only" -> ClipContext.Fluid.SOURCE_ONLY;
            default -> ClipContext.Fluid.NONE;
        };
        BlockHitResult blockHit = actor.level().clip(new ClipContext(start, end, shape, fluid, actor));
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

    private static void dealActionDamage(Player actor, LivingEntity recipient, JsonObject action) {
        String damageType = stringValue(action, "damage_type", "minecraft:player_attack");
        var sources = actor.damageSources();
        net.minecraft.world.damagesource.DamageSource source = switch (damageType) {
            case "minecraft:on_fire" -> sources.onFire();
            case "minecraft:starve" -> sources.starve();
            case "minecraft:magic" -> sources.magic();
            case "minecraft:generic" -> sources.generic();
            default -> sources.playerAttack(actor);
        };
        recipient.hurt(source, floatValue(action, "amount", 0.0F));
    }

    private static void addVelocity(LivingEntity actor, JsonObject action) {
        double x = doubleValue(action, "x", 0.0D);
        double y = doubleValue(action, "y", 0.0D);
        double z = doubleValue(action, "z", 0.0D);
        String space = stringValue(action, "space", "");
        double dx = x;
        double dz = z;
        if ("local".equals(space) || "local_horizontal".equals(space) || "local_horizontal_normalized".equals(space)) {
            Vec3 forward = actor.getLookAngle();
            if ("local_horizontal".equals(space) || "local_horizontal_normalized".equals(space)) {
                forward = new Vec3(forward.x, 0.0D, forward.z);
                if ("local_horizontal_normalized".equals(space)) {
                    double len2 = forward.x * forward.x + forward.z * forward.z;
                    forward = len2 < 1e-8 ? new Vec3(0, 0, 1) : forward.normalize();
                }
            }
            Vec3 side = new Vec3(forward.z, 0.0D, -forward.x);
            double sideLen2 = side.x*side.x + side.z*side.z;
            if (sideLen2 > 1e-8) side = side.normalize(); else side = new Vec3(1,0,0);
            dx = side.x * x + forward.x * z;
            dz = side.z * x + forward.z * z;
        }
        // Apoli's add_velocity "set" flag replaces the velocity instead of adding to it.
        if (action.has("set") && action.get("set").getAsBoolean()) {
            actor.setDeltaMovement(dx, y, dz);
            if (!actor.level().isClientSide) actor.hurtMarked = true;
        } else {
            actor.push(dx, y, dz);
        }
    }

    private static void playSound(LivingEntity actor, JsonObject action) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(action, "sound", ""));
        SoundEvent sound = id == null ? null : BuiltInRegistries.SOUND_EVENT.get(id);
        if (sound != null) {
            Level level = actor.level();
            net.minecraft.sounds.SoundSource category = actor instanceof Player
                    ? net.minecraft.sounds.SoundSource.PLAYERS
                    : actor instanceof net.minecraft.world.entity.monster.Monster
                    ? net.minecraft.sounds.SoundSource.HOSTILE
                    : net.minecraft.sounds.SoundSource.NEUTRAL;
            level.playSound(null, actor.getX(), actor.getY(), actor.getZ(), sound, category,
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
            case "apoli:damage" -> damageStack(actor, stack, intValue(action, "amount", 1),
                    booleanValue(action, "ignore_unbreaking", false),
                    actor.getUsedItemHand() == net.minecraft.world.InteractionHand.OFF_HAND
                            ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
            default -> execute(actor, actor, action);
        }
    }

    /**
     * Apoli's {@code damage} action. With {@code ignore_unbreaking} the vanilla Unbreaking
     * reduction from {@code hurtAndBreak} is bypassed by applying the durability loss directly.
     */
    private static void damageStack(Player actor, ItemStack stack, int amount,
                                    boolean ignoreUnbreaking, EquipmentSlot slot) {
        if (actor.getAbilities().instabuild || stack.isEmpty() || !stack.isDamageableItem()) return;
        int applied = Math.max(1, amount);
        if (!ignoreUnbreaking) {
            stack.hurtAndBreak(applied, actor, broken -> broken.broadcastBreakEvent(slot));
            return;
        }
        int next = stack.getDamageValue() + applied;
        if (next >= stack.getMaxDamage()) {
            stack.shrink(1);
            actor.broadcastBreakEvent(slot);
        } else {
            stack.setDamageValue(next);
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
            damageStack(actor, equipped, intValue(nested, "amount", 1),
                    booleanValue(nested, "ignore_unbreaking", false), equipmentSlot(slot));
            return;
        }
        execute(actor, recipient, nested);
    }

    private static EquipmentSlot equipmentSlot(String slot) {
        return switch (slot) {
            case "offhand" -> EquipmentSlot.OFFHAND;
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> EquipmentSlot.MAINHAND;
        };
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
            case "apoli:damage" -> damageStack(actor, accessory, intValue(nested, "amount", 1),
                    booleanValue(nested, "ignore_unbreaking", false), EquipmentSlot.MAINHAND);
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
        actor.getCooldowns().addCooldown(item, Math.max(0, intValue(action, "cooldown", 0)));
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
        if (!(actor instanceof ServerPlayer player) || actionAllowsServer(action)) return;
        ResourceLocation id = ResourceLocation.tryParse(stringValue(action, "power_animation_id", ""));
        PowerAnimationService.playWithTime(player, id, intValue(action, "animation_time", 0));
    }

    private static void playPowerAnimationWithCount(Player actor, JsonObject action) {
        if (!(actor instanceof ServerPlayer player) || actionAllowsServer(action)) return;
        ResourceLocation id = ResourceLocation.tryParse(stringValue(action, "power_animation_id", ""));
        PowerAnimationService.playWithCount(player, id, intValue(action, "animation_count", 1));
    }

    private static void playPowerAnimationLoop(Player actor, JsonObject action) {
        if (!(actor instanceof ServerPlayer player) || actionAllowsServer(action)) return;
        PowerAnimationService.playLoop(player,
                ResourceLocation.tryParse(stringValue(action, "power_animation_id", "")));
    }

    private static void stopPowerAnimation(Player actor, JsonObject action) {
        if (!(actor instanceof ServerPlayer player) || actionAllowsServer(action)) return;
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
        return action.has("can_on_server") && !action.get("can_on_server").getAsBoolean();
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
        if (!(recipient.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        JsonElement particle = action.get("particle");
        String id = particle != null && particle.isJsonObject()
                ? stringValue(particle.getAsJsonObject(), "type", "minecraft:poof")
                : particle == null ? "minecraft:poof" : particle.getAsString();
        ResourceLocation particleId = ResourceLocation.tryParse(id);
        if (particleId == null) return;
        net.minecraft.core.particles.ParticleOptions options;
        if ("minecraft:dust".equals(id) && particle != null && particle.isJsonObject()) {
            String[] values = stringValue(particle.getAsJsonObject(), "params", "").trim().split("\\s+");
            if (values.length != 4) return;
            try {
                options = new net.minecraft.core.particles.DustParticleOptions(
                        new org.joml.Vector3f(Float.parseFloat(values[0]),
                                Float.parseFloat(values[1]), Float.parseFloat(values[2])),
                        Float.parseFloat(values[3]));
            } catch (NumberFormatException invalid) {
                return;
            }
        } else {
            net.minecraft.core.particles.ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(particleId);
            if (!(type instanceof net.minecraft.core.particles.SimpleParticleType simple)) {
                spawnParticlesViaCommand(actor, recipient, action, particle, id);
                return;
            }
            options = simple;
        }
        JsonObject spread = action.getAsJsonObject("spread");
        int count = intValue(action, "count", 0);
        if (count <= 0) return;
        double width = recipient.getBbWidth();
        double height = recipient.getBbHeight();
        serverLevel.sendParticles(options, recipient.getX(),
                recipient.getY() + height * floatValue(action, "offset_y", 0.5F), recipient.getZ(),
                count,
                (float) (width * doubleValue(spread, "x", 0.5D)),
                (float) (height * doubleValue(spread, "y", 0.25D)),
                (float) (width * doubleValue(spread, "z", 0.5D)),
                floatValue(action, "speed", 0.0F));
    }

    /** Preserve support for parameterized non-dust particles used by other forms. */
    private static void spawnParticlesViaCommand(Player actor, LivingEntity recipient, JsonObject action,
                                                  JsonElement particle, String id) {
        if (particle != null && particle.isJsonObject() && particle.getAsJsonObject().has("params")) {
            id += " " + particle.getAsJsonObject().get("params").getAsString();
        }
        JsonObject spread = action.getAsJsonObject("spread");
        String command = "particle " + id + " ~" + doubleValue(action, "offset_x", 0.0D)
                + " ~" + doubleValue(action, "offset_y", 0.0D)
                + " ~" + doubleValue(action, "offset_z", 0.0D)
                + " " + doubleValue(spread, "x", 0.0D)
                + " " + doubleValue(spread, "y", 0.0D)
                + " " + doubleValue(spread, "z", 0.0D)
                + " " + doubleValue(action, "speed", 0.0D)
                + " " + intValue(action, "count", 1)
                + (booleanValue(action, "force", false) ? " force" : " normal");
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
        Vec3 center = actor.position();
        float diameter = intValue(action, "power", 0) * 2.0F;
        actor.level().gameEvent(actor, net.minecraft.world.level.gameevent.GameEvent.EXPLODE, center);
        if (diameter <= 0.0F) return;
        int minX = net.minecraft.util.Mth.floor(center.x - diameter - 1.0D);
        int maxX = net.minecraft.util.Mth.floor(center.x + diameter + 1.0D);
        int minY = net.minecraft.util.Mth.floor(center.y - diameter - 1.0D);
        int maxY = net.minecraft.util.Mth.floor(center.y + diameter + 1.0D);
        int minZ = net.minecraft.util.Mth.floor(center.z - diameter - 1.0D);
        int maxZ = net.minecraft.util.Mth.floor(center.z + diameter + 1.0D);
        boolean causesDamage = booleanValue(action, "explosion_damage_entity", true);
        float multiplier = floatValue(action, "damage_multiplier", 1.0F);
        float baseDamage = floatValue(action, "base_damage", 0.0F);
        JsonObject condition = action.getAsJsonObject("entity_condition");
        for (Entity candidate : actor.level().getEntities(actor,
                new AABB(minX, minY, minZ, maxX, maxY, maxZ))) {
            if (candidate.ignoreExplosion() || !testEntity(actor, candidate, condition)) continue;
            double distance = Math.sqrt(candidate.distanceToSqr(center)) / diameter;
            if (distance > 1.0D) continue;
            double dx = candidate.getX() - center.x;
            double dy = (candidate instanceof net.minecraft.world.entity.item.PrimedTnt
                    ? candidate.getY() : candidate.getEyeY()) - center.y;
            double dz = candidate.getZ() - center.z;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length == 0.0D) continue;
            dx /= length;
            dy /= length;
            dz /= length;
            double exposure = net.minecraft.world.level.Explosion.getSeenPercent(center, candidate);
            double impact = (1.0D - distance) * exposure;
            if (causesDamage) {
                float vanillaDamage = (float) ((int) ((impact * impact + impact) / 2.0D
                        * 7.0D * diameter + 1.0D));
                candidate.hurt(actor.damageSources().explosion(actor, actor),
                        vanillaDamage * multiplier + baseDamage);
            }
            double knockback = candidate instanceof LivingEntity living
                    ? net.minecraft.world.item.enchantment.ProtectionEnchantment
                            .getExplosionKnockbackAfterDampener(living, impact)
                    : impact;
            candidate.setDeltaMovement(candidate.getDeltaMovement().add(
                    dx * knockback, dy * knockback, dz * knockback));
            if (candidate instanceof LivingEntity living) {
                execute(actor, living, action.getAsJsonObject("entity_action"));
            }
            candidate.hurtMarked = true;
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
