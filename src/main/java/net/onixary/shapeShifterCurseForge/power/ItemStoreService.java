package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;

import java.util.HashSet;
import java.util.Set;

/** Persistent virtual inventory behind Fabric's item_store power and its four entity actions. */
public final class ItemStoreService {
    private ItemStoreService() { }

    public static void tick(Player player) {
        if (player.level().isClientSide) return;
        SscApi.currentForm(player).ifPresent(data -> {
            CompoundTag stores = data.getItemStores();
            Set<String> assigned = new HashSet<>();
            final boolean[] changed = {false};
            FormPowerRegistry.visitActive(player, (powerId, power) -> {
                if (!"shape-shifter-curse:item_store".equals(FormPowerRegistry.typeOf(power))) return;
                String key = powerId.toString();
                assigned.add(key);
                if (!FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
                CompoundTag entry = stores.getCompound(key);
                ItemStack stack = readStack(entry);
                if (!stack.isEmpty()) {
                    stack.inventoryTick(player.level(), player,
                            2800 + FormPowerRuntime.intValue(power, "slot", 0), false);
                    if (!ItemStack.matches(stack, readStack(entry))) {
                        entry.put("Stack", stack.save(new CompoundTag()));
                        stores.put(key, entry);
                        changed[0] = true;
                    }
                }
                if (entry.getInt("Bobbing") > 0) {
                    entry.putInt("Bobbing", entry.getInt("Bobbing") - 1);
                    stores.put(key, entry);
                    changed[0] = true;
                }
            });
            for (String key : new HashSet<>(stores.getAllKeys())) {
                if (assigned.contains(key)) continue;
                ItemStack stack = readStack(stores.getCompound(key));
                if (!stack.isEmpty()) player.drop(stack, false);
                stores.remove(key);
                changed[0] = true;
            }
            if (changed[0]) save(player, stores);
        });
    }

    public static boolean check(Player player, JsonObject condition) {
        StorePower power = find(player, condition);
        boolean fallback = FormPowerRuntime.booleanValue(condition, "default", false);
        if (power == null || !condition.has("item_condition")
                || !condition.get("item_condition").isJsonObject()) return fallback;
        return FormPowerRuntime.matchesItem(get(player, power.definitionId),
                condition.getAsJsonObject("item_condition"));
    }

    public static void execute(Player player, JsonObject action) {
        if (player.level().isClientSide) return;
        String type = FormPowerRegistry.typeOf(action);
        StorePower power = find(player, action);
        if (power == null) {
            if ("shape-shifter-curse:gain_store_power_item".equals(type)
                    && FormPowerRuntime.booleanValue(action, "if_no_power_drop", true)) {
                ItemStack stack = itemFromJson(action.get("item"));
                if (!stack.isEmpty()) player.drop(stack, false);
            }
            return;
        }
        ItemStack old = get(player, power.definitionId);
        switch (type) {
            case "shape-shifter-curse:gain_store_power_item" -> {
                ItemStack incoming = itemFromJson(action.get("item"));
                if (incoming.isEmpty()) return;
                if (!old.isEmpty()) player.drop(old, false);
                set(player, power.definitionId, incoming);
            }
            case "shape-shifter-curse:drop_store_power_item" -> {
                if (!FormPowerRuntime.booleanValue(action, "remove_item", false) && !old.isEmpty())
                    player.drop(old, false);
                set(player, power.definitionId, ItemStack.EMPTY);
            }
            case "shape-shifter-curse:swap_store_power_item" -> {
                EquipmentSlot slot = slot(FormPowerRuntime.stringValue(action, "slot", "mainhand"));
                ItemStack equipped = player.getItemBySlot(slot).copy();
                player.setItemSlot(slot, old);
                set(player, power.definitionId, equipped);
            }
            case "shape-shifter-curse:invoke_store_power_item" -> {
                JsonObject itemAction = action.getAsJsonObject("action");
                if (itemAction == null) return;
                applyItemAction(player, old, itemAction);
                set(player, power.definitionId, old);
            }
        }
    }

    public static ItemStack get(Player player, ResourceLocation definitionId) {
        return SscApi.currentForm(player)
                .map(data -> readStack(data.getItemStores().getCompound(definitionId.toString())))
                .orElse(ItemStack.EMPTY);
    }

    private static void set(Player player, ResourceLocation definitionId, ItemStack stack) {
        SscApi.currentForm(player).ifPresent(data -> {
            CompoundTag stores = data.getItemStores();
            CompoundTag entry = stores.getCompound(definitionId.toString());
            if (stack.isEmpty()) entry.remove("Stack");
            else entry.put("Stack", stack.copy().save(new CompoundTag()));
            entry.putInt("Bobbing", 5);
            stores.put(definitionId.toString(), entry);
            save(player, stores);
        });
    }

    private static void save(Player player, CompoundTag stores) {
        SscApi.currentForm(player).ifPresent(data -> data.setItemStores(stores));
        if (player instanceof ServerPlayer serverPlayer) ModNetwork.sendItemStores(serverPlayer, stores);
    }

    private static ItemStack readStack(CompoundTag entry) {
        return entry.contains("Stack", Tag.TAG_COMPOUND)
                ? ItemStack.of(entry.getCompound("Stack")) : ItemStack.EMPTY;
    }

    private static StorePower find(Player player, JsonObject json) {
        ResourceLocation logicalId = ResourceLocation.tryParse(FormPowerRuntime.stringValue(json, "id", ""));
        if (logicalId == null) return null;
        final StorePower[] result = {null};
        FormPowerRegistry.visitActive(player, (definitionId, power) -> {
            ResourceLocation powerLogicalId = ResourceLocation.tryParse(
                    FormPowerRuntime.stringValue(power, "id", ""));
            if (result[0] != null || !"shape-shifter-curse:item_store".equals(FormPowerRegistry.typeOf(power))
                    || !logicalId.equals(powerLogicalId)
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            result[0] = new StorePower(definitionId);
        });
        return result[0];
    }

    private record StorePower(ResourceLocation definitionId) { }

    private static ItemStack itemFromJson(JsonElement json) {
        if (json == null || json.isJsonNull()) return ItemStack.EMPTY;
        JsonObject object;
        if (json.isJsonPrimitive()) {
            object = new JsonObject();
            object.add("item", json);
        } else if (json.isJsonObject()) object = json.getAsJsonObject();
        else return ItemStack.EMPTY;
        ResourceLocation id = ResourceLocation.tryParse(FormPowerRuntime.stringValue(object, "item", ""));
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item, Math.max(1, FormPowerRuntime.intValue(object, "count", 1)));
        if (object.has("tag") && object.get("tag").isJsonPrimitive()) {
            try { stack.setTag(TagParser.parseTag(object.get("tag").getAsString())); }
            catch (Exception ignored) { }
        }
        return stack;
    }

    private static EquipmentSlot slot(String name) {
        return switch (name) {
            case "offhand" -> EquipmentSlot.OFFHAND;
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> EquipmentSlot.MAINHAND;
        };
    }

    private static void applyItemAction(Player player, ItemStack stack, JsonObject action) {
        String type = FormPowerRegistry.typeOf(action);
        if ("apoli:and".equals(type) && action.has("actions")) {
            for (JsonElement child : action.getAsJsonArray("actions"))
                if (child.isJsonObject()) applyItemAction(player, stack, child.getAsJsonObject());
        } else if ("apoli:consume".equals(type)) {
            stack.shrink(Math.max(0, FormPowerRuntime.intValue(action, "amount", 1)));
        } else if ("apoli:damage".equals(type) && stack.isDamageableItem()) {
            int amount = Math.max(0, FormPowerRuntime.intValue(action, "amount", 1));
            if (FormPowerRuntime.booleanValue(action, "ignore_unbreaking", false)) {
                int damage = stack.getDamageValue() + amount;
                if (damage >= stack.getMaxDamage()) stack.shrink(1);
                else stack.setDamageValue(damage);
            } else stack.hurtAndBreak(amount, player, ignored -> { });
        } else if (!"apoli:and".equals(type)) {
            FormPowerRuntime.execute(player, player, action);
        }
    }
}
