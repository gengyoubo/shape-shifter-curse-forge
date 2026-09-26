package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Forge event bridge for the retained power types that do not have a vanilla event of their own. */
@SuppressWarnings("deprecation")
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class MissingPowerEvents {
    private static final Map<UUID, Set<MobEffect>> OWNED_EFFECTS = new HashMap<>();
    /** Pre-power MobEffectInstance per (player, effect), restored when the power stops applying it. */
    private static final Map<UUID, Map<MobEffect, MobEffectInstance>> OWNED_EFFECT_SNAPSHOTS = new HashMap<>();
    private static final Map<UUID, Set<UUID>> OWNED_GLOW_TARGETS = new HashMap<>();
    /** Glow flag of an entity before the first power-driven glow, so other sources are not cleared. */
    private static final Map<UUID, Boolean> GLOW_PREVIOUS_STATE = new HashMap<>();
    private static final Map<UUID, Boolean> MAY_FLY_BEFORE_POWER = new HashMap<>();

    private MissingPowerEvents() { }

    /** Remove owned effects and flight permissions that the new form no longer grants. */
    public static void onFormChanged(Player player) {
        if (player.level().isClientSide) return;
        maintainEffects(player);
        maintainFlight(player);
        ItemStoreService.tick(player);
        maintainArmor(player, true);
        CLASH_STATE.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) {
            return;
        }

        maintainEffects(player);
        maintainFlight(player);
        maintainArmor(player, false);
        ItemStoreService.tick(player);
        tickJumpClash(player);

    }

    private static void maintainEffects(Player player) {
        Set<MobEffect> wanted = new HashSet<>();
        Set<MobEffect> previous = OWNED_EFFECTS.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
        FormPowerRegistry.visitActive(player, (id, power) -> {
            String type = FormPowerRegistry.typeOf(power);
            if ("shape-shifter-curse:apply_effect".equals(type)
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                if (power.has("status_effects") && power.get("status_effects").isJsonArray()) {
                    for (var element : power.getAsJsonArray("status_effects")) {
                        if (!element.isJsonObject()) continue;
                        JsonObject effectData = element.getAsJsonObject();
                        ResourceLocation effectId = ResourceLocation.tryParse(
                                FormPowerRuntime.stringValue(effectData, "effect", ""));
                        MobEffect effect = effectId == null ? null : BuiltInRegistries.MOB_EFFECT.get(effectId);
                        if (effect == null) continue;
                        wanted.add(effect);
                        if (!previous.contains(effect) || player.tickCount % 20 == 0 && !player.hasEffect(effect)) {
                            applyEffect(player, effectData);
                        }
                    }
                }
            }
        });
        for (MobEffect old : previous) {
            if (wanted.contains(old)) continue;
            // Restore any effect instance that existed before the power was applied.
            MobEffectInstance snapshot = OWNED_EFFECT_SNAPSHOTS
                    .getOrDefault(player.getUUID(), java.util.Collections.emptyMap()).remove(old);
            player.removeEffect(old);
            if (snapshot != null) player.addEffect(snapshot);
        }
        previous.clear();
        previous.addAll(wanted);
    }

    /** Records the pre-power instance of an effect once, so cleanup can restore it. */
    private static void snapshotEffect(Player player, MobEffect effect) {
        Map<MobEffect, MobEffectInstance> snapshots = OWNED_EFFECT_SNAPSHOTS
                .computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        // null means no effect existed before this power. putIfAbsent treats null as
        // absent and would capture the power's own effect on the following tick.
        if (!snapshots.containsKey(effect)) snapshots.put(effect, player.getEffect(effect));
    }

    private static MobEffect applyEffect(Player player, JsonObject data) {
        ResourceLocation id = ResourceLocation.tryParse(FormPowerRuntime.stringValue(data, "effect", ""));
        MobEffect effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id);
        if (effect == null) return null;
        snapshotEffect(player, effect);
        player.removeEffect(effect);
        player.addEffect(new MobEffectInstance(effect,
                Math.max(20, FormPowerRuntime.intValue(data, "duration", 40)),
                FormPowerRuntime.intValue(data, "amplifier", 0), false,
                FormPowerRuntime.booleanValue(data, "show_particles", true),
                FormPowerRuntime.booleanValue(data, "show_icon", true)));
        return effect;
    }

    private static void maintainFlight(Player player) {
        boolean creativeFlight = hasActive(player, "apoli:creative_flight");
        if (creativeFlight) {
            MAY_FLY_BEFORE_POWER.putIfAbsent(player.getUUID(), player.getAbilities().mayfly);
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        } else if (!player.isCreative()) {
            // Restore the mayfly flag captured before the power was granted (it may have been true).
            Boolean previous = MAY_FLY_BEFORE_POWER.remove(player.getUUID());
            if (previous != null) {
                player.getAbilities().mayfly = previous;
                if (!previous) {
                    player.getAbilities().flying = false;
                }
                player.onUpdateAbilities();
            }
        }
    }

    private static void maintainArmor(Player player, boolean justGainedPower) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !isArmorRestricted(player, slot, stack)) continue;
            player.setItemSlot(slot, ItemStack.EMPTY);
            // Apoli drops armor that was already equipped when the power is gained.
            // Later, unexpected programmatic equips are returned to inventory.
            if (justGainedPower || !player.getInventory().add(stack)) {
                player.spawnAtLocation(stack, player.getEyeHeight(player.getPose()));
            }
        }
    }

    /** Shared by the armor slot, right-click equip and server fallback paths. */
    public static boolean isArmorRestricted(Player player, EquipmentSlot slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        final boolean[] restricted = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (restricted[0] || !"apoli:restrict_armor".equals(FormPowerRegistry.typeOf(power))) return;
            JsonObject condition = power.getAsJsonObject(slotName(slot));
            if (condition != null && matchesArmorCondition(player, stack, condition)) restricted[0] = true;
        });
        return restricted[0];
    }

    @SubscribeEvent
    public static void preventRightClickArmorEquip(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof net.minecraft.world.item.Equipable)) return;
        EquipmentSlot slot = net.minecraft.world.entity.Mob.getEquipmentSlotForItem(stack);
        if (!isArmorRestricted(player, slot, stack)) return;
        event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        event.setCanceled(true);
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
        // Leaf conditions (apoli:ingredient, is_morph_scale_item, apoli:armor_value, ...).
        // matchesItem already applies the "inverted" flag internally, so do not invert here again.
        return FormPowerRuntime.matchesItem(stack, condition);
    }

    private static boolean compare(double value, JsonObject condition) {
        // Delegates to the shared interpreter so both entry points use the same tolerance.
        return FormPowerRuntime.compare(value, condition);
    }

    private static boolean inverted(JsonObject condition, boolean value) {
        return (condition.has("inverted") && condition.get("inverted").getAsBoolean()) != value;
    }

    private static void maintainEntityGlow(Player player) {
        Set<UUID> wanted = new HashSet<>();
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:entity_glow".equals(FormPowerRegistry.typeOf(power))) return;
            double radius = FormPowerRuntime.doubleValue(power, "radius", 20.0D);
            for (Entity target : player.level().getEntities(player, player.getBoundingBox().inflate(radius),
                    entity -> entity.isAlive() && FormPowerRuntime.test(player, entity,
                            power.getAsJsonObject("bientity_condition")))) {
                // Record the pre-power glow flag once, so removing our glow restores other sources.
                GLOW_PREVIOUS_STATE.putIfAbsent(target.getUUID(), target.hasGlowingTag());
                target.setGlowingTag(true);
                wanted.add(target.getUUID());
            }
        });
        Set<UUID> previous = OWNED_GLOW_TARGETS.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
        for (UUID old : previous) {
            if (wanted.contains(old)) continue;
            // A target may have moved beyond the old search radius before the form changes.
            Entity target = null;
            if (player.getServer() != null) {
                for (ServerLevel level : player.getServer().getAllLevels()) {
                    target = level.getEntity(old);
                    if (target != null) break;
                }
            }
            Boolean priorState = GLOW_PREVIOUS_STATE.remove(old);
            if (target != null) target.setGlowingTag(priorState != null && priorState);
        }
        previous.clear();
        previous.addAll(wanted);
    }

    private static void tickJumpClash(Player player) {
        // Fabric's power has an active window after takeoff, with no extra cooldown on hit.
        final java.util.List<JsonObject> clashPowers = new java.util.ArrayList<>();
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:sneaking_jump_clash".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                clashPowers.add(power);
            }
        });
        if (clashPowers.isEmpty()) {
            CLASH_STATE.remove(player.getUUID());
            return;
        }
        ClashState state = CLASH_STATE.computeIfAbsent(player.getUUID(), ignored -> new ClashState());
        // Fabric edge: trigger when leaving ground while sneaking with upward motion.
        if (player.onGround()) {
            state.wasOnGround = true;
            state.isActive = false;
            state.activeTicks = 0;
            return;
        }
        boolean sneaking = player.isCrouching()
                || MovementPowerService.shouldForceSneaking(player);
        if (state.wasOnGround && sneaking && player.getDeltaMovement().y > 0.0D) {
            state.isActive = true;
            state.activeTicks = 0;
            state.wasOnGround = false;
        }
        if (!state.isActive) return;
        state.activeTicks++;
        for (JsonObject power : clashPowers) {
            int duration = Math.max(1, FormPowerRuntime.intValue(power, "check_duration", 15));
            if (state.activeTicks > duration) {
                state.isActive = false;
                state.activeTicks = 0;
                return;
            }
            double expansion = FormPowerRuntime.doubleValue(power, "expansion_distance", 1.0D);
            if (checkClashCollision(player, power, expansion)) {
                state.isActive = false;
                state.activeTicks = 0;
                return;
            }
        }
    }

    private static boolean checkClashCollision(Player player, JsonObject power, double expansion) {
        net.minecraft.core.Direction facing = player.getDirection();
        net.minecraft.world.phys.Vec3 facingVec = new net.minecraft.world.phys.Vec3(
                facing.getStepX(), 0.0D, facing.getStepZ());
        net.minecraft.world.phys.AABB box = player.getBoundingBox()
                .expandTowards(facingVec.scale(expansion)).inflate(0.5D);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != player && entity.isAlive() && !entity.isRemoved())) {
            net.minecraft.world.phys.Vec3 from = new net.minecraft.world.phys.Vec3(
                    player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ());
            net.minecraft.world.phys.Vec3 to = new net.minecraft.world.phys.Vec3(
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
            net.minecraft.world.level.ClipContext context = new net.minecraft.world.level.ClipContext(
                    from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, player);
            net.minecraft.world.phys.BlockHitResult hit = player.level().clip(context);
            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) continue;
            FormPowerRuntime.execute(player, target, power.getAsJsonObject("bientity_action"));
            target.hurt(player.damageSources().playerAttack(player),
                    FormPowerRuntime.floatValue(power, "damage", 0.0F));
            return true;
        }
        return false;
    }

    private static final Map<UUID, ClashState> CLASH_STATE = new HashMap<>();

    private static final class ClashState {
        private boolean wasOnGround = true;
        private boolean isActive;
        private int activeTicks;
    }

    /**
     * Forge counterpart of Fabric's {@code bypass_stepping_effect}: holders do not
     * trample farmland. The power definition carries no condition in this pack, but
     * the handler still honors an optional {@code condition} object for datapack use.
     */
    @SubscribeEvent
    public static void trampleFarmland(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        final boolean[] bypass = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!bypass[0] && "shape-shifter-curse:bypass_stepping_effect".equals(FormPowerRegistry.typeOf(power))
                    && FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) {
                bypass[0] = true;
            }
        });
        if (bypass[0]) event.setCanceled(true);
    }

    /** Called only after Forge has harvested the block with a valid tool. */
    public static void onHarvestedBlock(Player player, BlockState state) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:action_on_block_break".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))
                    || !matchesBlockBreak(player, state, power.getAsJsonObject("block_condition"))) return;
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

    /** Apoli item_on_item runs when the cursor stack is clicked onto an inventory slot. */
    public static boolean itemOnItem(Player player, ItemStack using, ItemStack other, Slot slot) {
        if (using.isEmpty() || other.isEmpty()) return false;
        final boolean[] handled = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (handled[0]) return;
            if (!"apoli:item_on_item".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            if (!FormPowerRuntime.matchesItem(using, power.getAsJsonObject("using_item_condition"))
                    || !FormPowerRuntime.matchesItem(other, power.getAsJsonObject("on_item_condition"))) return;
            if (!player.level().isClientSide) {
                consume(using, power.getAsJsonObject("using_item_action"));
                consume(other, power.getAsJsonObject("on_item_action"));
                if (power.has("result") && power.get("result").isJsonObject()) {
                    ItemStack result = itemFromJson(power.getAsJsonObject("result"));
                    if (!player.getInventory().add(result)) player.level().addFreshEntity(
                            new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), result));
                }
                FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
                slot.setChanged();
            }
            handled[0] = true;
        });
        return handled[0];
    }

    private static void consume(ItemStack stack, JsonObject action) {
        if (action != null && "apoli:consume".equals(FormPowerRegistry.typeOf(action))) {
            stack.shrink(FormPowerRuntime.intValue(action, "amount", 1));
        }
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
        return !FormPowerRegistry.has(player, ResourceLocation.fromNamespaceAndPath(
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
