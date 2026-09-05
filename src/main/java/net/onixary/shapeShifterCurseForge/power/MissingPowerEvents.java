package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Forge event bridge for the retained power types that do not have a vanilla event of their own. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class MissingPowerEvents {
    private static final Map<UUID, Set<MobEffect>> OWNED_EFFECTS = new HashMap<>();
    private static final Map<UUID, Set<UUID>> OWNED_LOOT_MODIFIERS = new HashMap<>();
    private static final Map<UUID, Set<UUID>> OWNED_GLOW_TARGETS = new HashMap<>();
    private static final Map<UUID, Integer> CLASH_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Boolean> MAY_FLY_BEFORE_POWER = new HashMap<>();

    private MissingPowerEvents() { }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;

        maintainEffects(player);
        maintainFlight(player);
        maintainArmor(player);
        maintainParticles(player);
        maintainEntityGlow(player);
        maintainSimpleMovement(player);
        maintainLooting(player);
        maintainPotionStacks(player);
        maintainDirtyWaterThirst(player);
        tickJumpClash(player);

        CLASH_COOLDOWNS.computeIfPresent(player.getUUID(), (id, value) -> value <= 1 ? null : value - 1);
    }

    private static void maintainEffects(Player player) {
        Set<MobEffect> wanted = new HashSet<>();
        FormPowerRegistry.visitActive(player, (id, power) -> {
            String type = FormPowerRegistry.typeOf(power);
            if ("shape-shifter-curse:apply_effect".equals(type)
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                if (power.has("status_effects") && power.get("status_effects").isJsonArray()) {
                    for (var element : power.getAsJsonArray("status_effects")) {
                        if (!element.isJsonObject()) continue;
                        MobEffect applied = applyEffect(player, element.getAsJsonObject());
                        if (applied != null) wanted.add(applied);
                    }
                }
            }
            if ("apoli:night_vision".equals(type)
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                ResourceLocation effectId = ResourceLocation.fromNamespaceAndPath("minecraft", "night_vision");
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
                if (effect != null) {
                    wanted.add(effect);
                    player.addEffect(new MobEffectInstance(effect, 50, 0, true, false, false));
                }
            }
        });
        Set<MobEffect> previous = OWNED_EFFECTS.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
        for (MobEffect old : previous) if (!wanted.contains(old)) player.removeEffect(old);
        previous.clear();
        previous.addAll(wanted);
    }

    private static MobEffect applyEffect(Player player, JsonObject data) {
        ResourceLocation id = ResourceLocation.tryParse(FormPowerRuntime.stringValue(data, "effect", ""));
        MobEffect effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id);
        if (effect == null) return null;
        player.addEffect(new MobEffectInstance(effect,
                Math.max(20, FormPowerRuntime.intValue(data, "duration", 40)),
                FormPowerRuntime.intValue(data, "amplifier", 0), false,
                FormPowerRuntime.booleanValue(data, "show_particles", true),
                FormPowerRuntime.booleanValue(data, "show_icon", true)));
        return effect;
    }

    private static void maintainFlight(Player player) {
        boolean creativeFlight = hasActive(player, "apoli:creative_flight");
        boolean elytraFlight = hasActive(player, "apoli:elytra_flight");
        if (creativeFlight) {
            MAY_FLY_BEFORE_POWER.putIfAbsent(player.getUUID(), player.getAbilities().mayfly);
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        } else if (!player.isCreative() && MAY_FLY_BEFORE_POWER.remove(player.getUUID()) != null) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        if (elytraFlight && !player.onGround() && player.isSprinting() && !player.isFallFlying()) {
            player.startFallFlying();
        }
    }

    private static void maintainArmor(Player player) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:restrict_armor".equals(FormPowerRegistry.typeOf(power))) return;
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                JsonObject condition = power.getAsJsonObject(slotName(slot));
                ItemStack stack = player.getItemBySlot(slot);
                if (condition == null || stack.isEmpty() || !matchesArmorCondition(player, stack, condition)) continue;
                player.setItemSlot(slot, ItemStack.EMPTY);
                if (!player.getInventory().add(stack)) {
                    player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack));
                }
            }
        });
    }

    private static String slotName(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "head";
            case CHEST -> "chest";
            case LEGS -> "legs";
            case FEET -> "feet";
            default -> "";
        };
    }

    private static boolean matchesArmorCondition(Player player, ItemStack stack, JsonObject condition) {
        String type = FormPowerRegistry.typeOf(condition);
        if ("apoli:and".equals(type)) {
            if (!condition.has("conditions")) return true;
            for (var child : condition.getAsJsonArray("conditions")) {
                if (child.isJsonObject() && !matchesArmorCondition(player, stack, child.getAsJsonObject())) return false;
            }
            return inverted(condition, true);
        }
        if ("apoli:or".equals(type)) {
            boolean result = false;
            if (condition.has("conditions")) for (var child : condition.getAsJsonArray("conditions")) {
                if (child.isJsonObject() && matchesArmorCondition(player, stack, child.getAsJsonObject())) result = true;
            }
            return inverted(condition, result);
        }
        boolean result;
        if ("apoli:armor_value".equals(type)) {
            result = stack.getItem() instanceof ArmorItem armor
                    && compare(armor.getDefense(), condition);
        } else {
            result = FormPowerRuntime.matchesItem(stack, condition);
        }
        return inverted(condition, result);
    }

    private static boolean compare(double value, JsonObject condition) {
        double compared = FormPowerRuntime.doubleValue(condition, "compare_to", 0.0D);
        return switch (FormPowerRuntime.stringValue(condition, "comparison", "==")) {
            case ">" -> value > compared;
            case ">=" -> value >= compared;
            case "<" -> value < compared;
            case "<=" -> value <= compared;
            case "!=" -> value != compared;
            default -> value == compared;
        };
    }

    private static boolean inverted(JsonObject condition, boolean value) {
        return condition.has("inverted") && condition.get("inverted").getAsBoolean() ? !value : value;
    }

    private static void maintainParticles(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:particle".equals(FormPowerRegistry.typeOf(power))) return;
            int frequency = Math.max(1, FormPowerRuntime.intValue(power, "frequency", 1));
            if (player.tickCount % frequency != 0) return;
            JsonObject action = new JsonObject();
            action.addProperty("type", "apoli:spawn_particles");
            action.add("particle", power.get("particle"));
            action.addProperty("count", 1);
            action.addProperty("speed", 0.0D);
            JsonObject spread = new JsonObject();
            spread.addProperty("x", 0.25D);
            spread.addProperty("y", 0.25D);
            spread.addProperty("z", 0.25D);
            action.add("spread", spread);
            action.addProperty("offset_y", FormPowerRuntime.doubleValue(power, "offset_y", 0.0D));
            FormPowerRuntime.execute(player, player, action);
        });
    }

    private static void maintainEntityGlow(Player player) {
        Set<UUID> wanted = new HashSet<>();
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:entity_glow".equals(FormPowerRegistry.typeOf(power))) return;
            double radius = FormPowerRuntime.doubleValue(power, "radius", 20.0D);
            for (Entity target : player.level().getEntities(player, player.getBoundingBox().inflate(radius),
                    entity -> entity.isAlive() && FormPowerRuntime.test(player, entity,
                            power.getAsJsonObject("bientity_condition")))) {
                target.setGlowingTag(true);
                wanted.add(target.getUUID());
            }
        });
        Set<UUID> previous = OWNED_GLOW_TARGETS.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
        for (UUID old : previous) {
            if (wanted.contains(old)) continue;
            Entity target = player.level().getEntities(player, player.getBoundingBox().inflate(32.0D),
                    entity -> entity.getUUID().equals(old)).stream().findFirst().orElse(null);
            if (target != null) target.setGlowingTag(false);
        }
        previous.clear();
        previous.addAll(wanted);
    }

    private static void maintainSimpleMovement(Player player) {
        if (!hasPowerId(player, "like_water") || !player.isInWater() || player.isShiftKeyDown()) return;
        if (player.getDeltaMovement().y < 0.0D) {
            var velocity = player.getDeltaMovement();
            player.setDeltaMovement(velocity.x, Math.max(velocity.y, -0.02D), velocity.z);
        }
    }

    private static void maintainLooting(Player player) {
        AttributeInstance luck = player.getAttribute(Attributes.LUCK);
        if (luck == null) return;
        Set<UUID> wanted = new HashSet<>();
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:simple_looting".equals(FormPowerRegistry.typeOf(power))) return;
            UUID modifierId = UUID.nameUUIDFromBytes((id + "|looting").getBytes(StandardCharsets.UTF_8));
            wanted.add(modifierId);
            if (luck.getModifier(modifierId) == null) {
                luck.addTransientModifier(new AttributeModifier(modifierId, id.toString(),
                        FormPowerRuntime.doubleValue(power, "level", 1.0D), AttributeModifier.Operation.ADDITION));
            }
        });
        Set<UUID> previous = OWNED_LOOT_MODIFIERS.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
        for (UUID old : previous) if (!wanted.contains(old)) luck.removeModifier(old);
        previous.clear();
        previous.addAll(wanted);
    }

    private static void maintainPotionStacks(Player player) {
        final int[] limits = {0, 0};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:modify_potion_stack".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            int count = Math.max(1, FormPowerRuntime.intValue(power, "count", 1));
            if (FormPowerRuntime.booleanValue(power, "only_water_potion", false)) limits[1] = Math.max(limits[1], count);
            else limits[0] = Math.max(limits[0], count);
        });
        int regularLimit = limits[0];
        int waterLimit = limits[1];
        if (regularLimit == 0 && waterLimit == 0) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack source = player.getInventory().getItem(i);
            if (!(source.getItem() instanceof PotionItem) || source.getCount() >= Math.max(regularLimit, waterLimit)) continue;
            int limit = isWaterPotion(source) ? waterLimit : regularLimit;
            if (limit <= 1) continue;
            for (int j = i + 1; j < player.getInventory().getContainerSize() && source.getCount() < limit; j++) {
                ItemStack other = player.getInventory().getItem(j);
                if (other.isEmpty() || !ItemStack.isSameItemSameTags(source, other)) continue;
                int moved = Math.min(limit - source.getCount(), other.getCount());
                source.grow(moved);
                other.shrink(moved);
            }
        }
    }

    private static boolean isWaterPotion(ItemStack stack) {
        return stack.getItem() == Items.POTION
                && net.minecraft.world.item.alchemy.PotionUtils.getPotion(stack)
                == net.minecraft.world.item.alchemy.Potions.WATER;
    }

    private static void maintainDirtyWaterThirst(Player player) {
        if (!hasPowerId(player, "form_tan_prevent_dirty_water_thirst")) return;
        if (!player.isInWater()) return;
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("dehydration", "thirst_effect");
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
        if (effect != null) player.removeEffect(effect);
    }

    private static void tickJumpClash(Player player) {
        if (!hasPowerId(player, "form_ocelot_3_sneaking_jump_clash") || !player.isCrouching()
                || player.onGround() || CLASH_COOLDOWNS.containsKey(player.getUUID())) return;
        JsonObject power = findPower(player, "form_ocelot_3_sneaking_jump_clash");
        if (power == null) return;
        double distance = FormPowerRuntime.doubleValue(power, "expansion_distance", 1.0D);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(distance), entity -> entity != player && entity.isAlive())) {
            FormPowerRuntime.execute(player, target, power.getAsJsonObject("bientity_action"));
            target.hurt(player.damageSources().playerAttack(player), FormPowerRuntime.floatValue(power, "damage", 0.0F));
            CLASH_COOLDOWNS.put(player.getUUID(), Math.max(1, FormPowerRuntime.intValue(power, "check_duration", 15)));
            break;
        }
    }

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) return;
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:action_on_block_break".equals(FormPowerRegistry.typeOf(power))
                    || !matchesBlockBreak(player, event.getState(), power.getAsJsonObject("block_condition"))) return;
            FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
        });
    }

    private static boolean matchesBlockBreak(Player player, BlockState state, JsonObject condition) {
        if (condition == null) return true;
        String type = FormPowerRegistry.typeOf(condition);
        boolean result;
        if ("apoli:and".equals(type)) {
            result = true;
            for (var child : condition.getAsJsonArray("conditions")) if (child.isJsonObject()) {
                result &= matchesBlockBreak(player, state, child.getAsJsonObject());
            }
        } else if ("apoli:or".equals(type)) {
            result = false;
            for (var child : condition.getAsJsonArray("conditions")) if (child.isJsonObject()) {
                result |= matchesBlockBreak(player, state, child.getAsJsonObject());
            }
        } else if ("apoli:blast_resistance".equals(type)) {
            result = compare(state.getBlock().getExplosionResistance(), condition);
        } else if ("apoli:harvest_level".equals(type)) {
            result = player.hasCorrectToolForDrops(state);
            if (!result && FormPowerRuntime.booleanValue(condition, "allow", false)) result = true;
        } else {
            result = FormPowerRuntime.test(player, player, condition);
        }
        return inverted(condition, result);
    }

    @SubscribeEvent
    public static void itemOnItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        ItemStack using = event.getItemStack();
        ItemStack other = player.getOffhandItem();
        if (using.isEmpty() || other.isEmpty()) return;
        final boolean[] handled = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (handled[0]) return;
            if (!"apoli:item_on_item".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            if (!FormPowerRuntime.matchesItem(using, power.getAsJsonObject("using_item_condition"))
                    || !FormPowerRuntime.matchesItem(other, power.getAsJsonObject("on_item_condition"))) return;
            consume(using, power.getAsJsonObject("using_item_action"), player);
            consume(other, power.getAsJsonObject("on_item_action"), player);
            if (power.has("result") && power.get("result").isJsonObject()) {
                ItemStack result = itemFromJson(power.getAsJsonObject("result"));
                if (!player.getInventory().add(result)) player.level().addFreshEntity(
                        new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), result));
            }
            FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
            event.setCanceled(true);
            handled[0] = true;
        });
    }

    private static void consume(ItemStack stack, JsonObject action, Player player) {
        if (action != null && "apoli:consume".equals(FormPowerRegistry.typeOf(action))
                && !player.getAbilities().instabuild) stack.shrink(FormPowerRuntime.intValue(action, "amount", 1));
    }

    private static ItemStack itemFromJson(JsonObject data) {
        ResourceLocation id = ResourceLocation.tryParse(FormPowerRuntime.stringValue(data, "item", ""));
        var item = id == null ? null : BuiltInRegistries.ITEM.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, FormPowerRuntime.intValue(data, "amount", 1));
    }

    private static boolean hasActive(Player player, String type) {
        final boolean[] found = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!found[0] && type.equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) found[0] = true;
        });
        return found[0];
    }

    private static boolean hasPowerId(Player player, String path) {
        return FormPowerRegistry.has(player, ResourceLocation.fromNamespaceAndPath(
                ShapeShifterCurseForge.RESOURCE_NAMESPACE, path));
    }

    private static JsonObject findPower(Player player, String path) {
        final JsonObject[] found = {null};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (found[0] == null && id.getPath().equals(path)) found[0] = power;
        });
        return found[0];
    }
}
