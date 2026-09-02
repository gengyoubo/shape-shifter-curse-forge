package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
            case "shape-shifter-curse:has_mana" -> FormActivePowerService.hasMana(actor,
                    floatValue(condition, "mana", 0.0F));
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
            case "apoli:apply_effect" -> applyEffect(recipient, action.getAsJsonObject("effect"));
            case "apoli:heal" -> recipient.heal(floatValue(action, "amount", 0.0F));
            case "apoli:add_velocity" -> addVelocity(actor, action);
            case "apoli:set_on_fire" -> recipient.setSecondsOnFire(intValue(action, "duration", 1));
            case "apoli:damage" -> recipient.hurt(actor.damageSources().playerAttack(actor), floatValue(action, "amount", 0.0F));
            case "apoli:play_sound" -> playSound(actor, action);
            case "shape-shifter-curse:consume_mana" -> FormActivePowerService.consumeMana(actor,
                    floatValue(action, "mana", 0.0F));
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
        String type = FormPowerRegistry.typeOf(condition);
        boolean result = switch (type) {
            case "apoli:entity_type" -> matchesEntityType(target, condition);
            default -> test(actor, target, condition);
        };
        return condition.has("inverted") && condition.get("inverted").getAsBoolean() ? !result : result;
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

    private static boolean hasEffect(Player actor, JsonObject condition) {
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

    private static boolean matchesEntityType(Entity entity, JsonObject condition) {
        ResourceLocation id = ResourceLocation.tryParse(stringValue(condition, "entity_type", ""));
        return id != null && BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(id);
    }

    private static boolean matchesInventory(Player actor, JsonObject condition) {
        ItemStack stack = actor.getMainHandItem();
        int matching = stack.isEmpty() ? 0 : (matchesItem(stack, condition.getAsJsonObject("item_condition")) ? 1 : 0);
        return condition.has("comparison") ? compare(matching, condition) : matching > 0;
    }

    private static boolean matchesItem(ItemStack stack, JsonObject condition) {
        if (condition == null) {
            return true;
        }
        return switch (FormPowerRegistry.typeOf(condition)) {
            case "apoli:ingredient" -> {
                JsonObject ingredient = condition.getAsJsonObject("ingredient");
                ResourceLocation itemId = ingredient == null ? null
                        : ResourceLocation.tryParse(stringValue(ingredient, "item", ""));
                yield itemId != null && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId);
            }
            case "shape-shifter-curse:is_weapon" -> stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
            default -> true;
        };
    }

    private static boolean matchesBlock(Player actor, JsonObject condition) {
        JsonObject blockCondition = condition.getAsJsonObject("block_condition");
        if (blockCondition == null) {
            return actor.onGround();
        }
        if (!"apoli:block".equals(FormPowerRegistry.typeOf(blockCondition))) {
            return true;
        }
        ResourceLocation id = ResourceLocation.tryParse(stringValue(blockCondition, "block", ""));
        Block block = id == null ? null : BuiltInRegistries.BLOCK.get(id);
        return block != null && actor.level().getBlockState(actor.blockPosition().below()).is(block);
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
        if ("local".equals(stringValue(action, "space", ""))) {
            Vec3 forward = actor.getLookAngle();
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
