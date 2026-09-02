package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

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
            case "apoli:fluid_height" -> compare(actor.isInWater() ? 1.0D : 0.0D, condition);
            case "apoli:exposed_to_sun" -> actor.level().canSeeSky(actor.blockPosition())
                    && actor.level().isDay() && actor.level().getMaxLocalRawBrightness(actor.blockPosition()) >= 12;
            case "apoli:status_effect" -> hasEffect(actor, condition);
            case "apoli:biome" -> matchesBiome(actor, condition);
            case "apoli:brightness" -> compare(actor.level().getMaxLocalRawBrightness(actor.blockPosition()) / 15.0D, condition);
            case "apoli:inventory" -> matchesInventory(actor, condition);
            case "apoli:on_block" -> matchesBlock(actor, condition);
            case "apoli:power_active" -> hasPower(actor, condition);
            case "apoli:resource" -> matchesResource(actor, condition);
            case "apoli:air" -> compare(actor.getAirSupply(), condition);
            case "apoli:in_tag" -> target != null && matchesEntityTag(target, condition);
            case "apoli:empty" -> true;
            case "apoli:fall_distance" -> compare(actor.fallDistance, condition);
            case "apoli:fall_flying" -> actor.isFallFlying();
            case "apoli:swimming" -> actor.isSwimming();
            case "apoli:on_fire" -> actor.isOnFire();
            case "apoli:collided_horizontally" -> actor.horizontalCollision;
            case "apoli:constant" -> !condition.has("value") || condition.get("value").getAsBoolean();
            case "apoli:entity_group" -> target instanceof LivingEntity living
                    && "undead".equals(stringValue(condition, "group", "")) && living.getMobType() == net.minecraft.world.entity.MobType.UNDEAD;
            case "apoli:in_block" -> matchesBlockAt(actor, actor.blockPosition(), condition.getAsJsonObject("block_condition"));
            case "apoli:block_collision" -> matchesBlockCollision(actor, condition);
            case "shape-shifter-curse:check_accessory", "shape-shifter-curse:has_accessory" -> false;
            case "shape-shifter-curse:has_mana" -> FormActivePowerService.hasMana(actor,
                    floatValue(condition, "mana", 0.0F));
            case "shape-shifter-curse:instinct_value" -> compare(InstinctService.value(actor), condition);
            case "apoli:target_condition" -> target != null && testEntity(actor, target, condition.getAsJsonObject("condition"));
            case "apoli:actor_condition" -> test(actor, target, condition.getAsJsonObject("condition"));
            case "apoli:entity_type" -> target != null && matchesEntityType(target, condition);
            default -> true;
        };
        return condition.has("inverted") && condition.get("inverted").getAsBoolean() ? !result : result;
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
            case "apoli:target_action" -> execute(actor, target, action.getAsJsonObject("action"));
            case "apoli:actor_action" -> execute(actor, actor, action.getAsJsonObject("action"));
            case "apoli:trigger_cooldown" -> triggerCooldown(actor, action);
            case "apoli:modify_resource" -> modifyResource(actor, action);
            case "apoli:execute_command" -> executeCommand(actor, recipient, action);
            case "apoli:spawn_particles" -> spawnParticles(actor, recipient, action);
            case "apoli:fire_projectile" -> fireProjectile(actor, action);
            case "shape-shifter-curse:consume_mana" -> FormActivePowerService.consumeMana(actor,
                    floatValue(action, "mana", 0.0F));
            case "shape-shifter-curse:gain_mana" -> FormActivePowerService.gainMana(actor,
                    floatValue(action, "mana", 0.0F));
            case "shape-shifter-curse:add_instinct" -> InstinctService.add(actor,
                    stringValue(action, "instinct_effect_id", "shape-shifter-curse:unknown"),
                    floatValue(action, "value", 0.0F), intValue(action, "duration", 0), false);
            case "shape-shifter-curse:fire_web_bullet" -> WebPowerActions.fireBullet(actor, action);
            case "shape-shifter-curse:web_bridge" -> WebPowerActions.buildBridge(actor, action);
            case "shape-shifter-curse:fire_arrow" -> fireArrow(actor, action);
            case "shape-shifter-curse:explosion_damage_entity" -> explosionDamage(actor, action);
            case "shape-shifter-curse:spawn_particles_in_circle" -> spawnParticlesInCircle(actor, action);
            case "shape-shifter-curse:summon_anubis_wolf_minion", "shape-shifter-curse:bi_summon_anubis_wolf_minion"
                    -> AnubisMinionService.summon(actor, target == null ? actor : target, action);
            case "shape-shifter-curse:set_falling_distance" -> actor.fallDistance = floatValue(action, "distance", 0.0F);
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
        return id != null && actor.level().getBiome(actor.blockPosition()).unwrapKey()
                .map(key -> key.location().equals(id)).orElse(false);
    }

    private static boolean hasPower(Player actor, JsonObject condition) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "power", ""));
        return id != null && FormPowerRegistry.has(actor, id);
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
            case "shape-shifter-curse:is_weapon" -> stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
            case "apoli:empty" -> stack.isEmpty();
            case "apoli:food" -> stack.isEdible();
            default -> true;
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
        if ("apoli:in_tag".equals(type)) {
            ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "tag", ""));
            return id != null && actor.level().getBlockState(pos).is(TagKey.create(Registries.BLOCK, id));
        }
        if (!"apoli:block".equals(type)) return true;
        ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "block", ""));
        Block block = id == null ? null : BuiltInRegistries.BLOCK.get(id);
        return block != null && actor.level().getBlockState(pos).is(block);
    }

    private static boolean matchesBlockCollision(Player actor, JsonObject condition) {
        if (!actor.horizontalCollision) return false;
        Vec3 direction = actor.getLookAngle();
        BlockPos pos = BlockPos.containing(actor.getX() + direction.x * 0.45D + doubleValue(condition, "offset_x", 0.0D),
                actor.getY() + 0.2D, actor.getZ() + direction.z * 0.45D + doubleValue(condition, "offset_z", 0.0D));
        return matchesBlockAt(actor, pos, condition.getAsJsonObject("block_condition"));
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

    private static void triggerCooldown(Player actor, JsonObject action) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(action, "power", ""));
        if (id != null) FormActivePowerService.triggerCooldown(actor, id);
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

    public static float floatValue(JsonObject json, String key, float fallback) {
        return json != null && json.has(key) ? json.get(key).getAsFloat() : fallback;
    }

    public static double doubleValue(JsonObject json, String key, double fallback) {
        return json != null && json.has(key) ? json.get(key).getAsDouble() : fallback;
    }
}
