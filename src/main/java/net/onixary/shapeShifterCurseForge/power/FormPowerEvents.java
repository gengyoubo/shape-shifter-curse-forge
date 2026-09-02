package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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
        FormPowerRegistry.visitActive(player, (id, power) -> tickPower(player, power));
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
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
            });
        }
    }

    @SubscribeEvent
    public static void jump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
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
            runInteraction(event.getEntity(), null, "apoli:action_on_item_use");
        }
    }

    @SubscribeEvent
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().level().isClientSide) {
            runInteraction(event.getEntity(), null, "apoli:action_on_block_use");
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
    }

    private static void runInteraction(Player player, LivingEntity target, String expectedType) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (expectedType.equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, target, power.getAsJsonObject("condition"))) {
                FormPowerRuntime.execute(player, target, power.getAsJsonObject("entity_action"));
            }
        });
    }

    private static void refreshAttributes(Player player) {
        FormPowerRegistry.all().forEach((id, definition) -> refreshAttribute(player, id, definition.data(), false));
        FormPowerRegistry.visitActive(player, (id, power) -> refreshAttribute(player, id, power, true));
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
