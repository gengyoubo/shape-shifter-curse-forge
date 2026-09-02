package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Server event bridge for the high-frequency Apoli power families used by the forms. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class FormPowerEvents {
    private FormPowerEvents() {
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }

        refreshAttributes(player);
        FormActivePowerService.tick(player);
        InstinctService.tick((net.minecraft.server.level.ServerPlayer) player);
        BatAttachService.tick(player);
        FormPowerRegistry.visitActive(player, (id, power) -> tickPower(player, power));
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
        Entity directEntity = event.getSource().getDirectEntity();
        if (event.getEntity() instanceof Player defender) {
            FormPowerRegistry.visitActive(defender, (id, power) -> {
                String type = FormPowerRegistry.typeOf(power);
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

    private static boolean matchesProjectile(Projectile projectile, JsonObject condition) {
        if (condition == null) return true;
        if (!"apoli:projectile".equals(FormPowerRegistry.typeOf(condition))) return true;
        ResourceLocation id = ResourceLocation.tryParse(FormPowerRuntime.stringValue(condition, "projectile", ""));
        return id != null && id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType()));
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
