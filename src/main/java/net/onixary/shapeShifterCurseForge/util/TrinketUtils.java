package net.onixary.shapeShifterCurseForge.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import net.onixary.shapeShifterCurseForge.util.Accessory.AccessoryUtils;

import javax.annotation.Nullable;
import java.util.*;

// Forge port: retains data structures for accessory_powers JSON, but power apply is via FormPowerRegistry conditions
// rather than Apoli PowerHolderComponent. Curios inventory is the source of truth for isEquipped.
public class TrinketUtils {
    public interface CustomPowerTrinketInterface {
        void onFormChange(ItemStack stack, AccessoryItem.SlotData slot, Player entity);
    }

    public static class TrinketPowerData {
        public final List<ResourceLocation> accessoryPowers;
        public final List<ResourceLocation> allFormPowerAdd;
        public final List<ResourceLocation> allFormPowerRemove;
        public final HashMap<ResourceLocation, List<ResourceLocation>> formPowerAdd;
        public final HashMap<ResourceLocation, List<ResourceLocation>> formPowerRemove;
        public final HashMap<ResourceLocation, HashMap<ResourceLocation, List<ResourceLocation>>> layerPowerAddMap;
        public final HashMap<ResourceLocation, HashMap<ResourceLocation, List<ResourceLocation>>> layerPowerRemoveMap;

        private net.minecraft.util.Tuple<List<ResourceLocation>, List<ResourceLocation>> parsePowerList(JsonObject jsonObject) {
            List<ResourceLocation> add = new ArrayList<>();
            List<ResourceLocation> remove = new ArrayList<>();
            if (jsonObject.has("add")) {
                jsonObject.get("add").getAsJsonArray().forEach(e -> {
                    ResourceLocation id = ResourceLocation.tryParse(e.getAsString());
                    if (id != null) add.add(id);
                });
            }
            if (jsonObject.has("remove")) {
                jsonObject.get("remove").getAsJsonArray().forEach(e -> {
                    ResourceLocation id = ResourceLocation.tryParse(e.getAsString());
                    if (id != null) remove.add(id);
                });
            }
            return new net.minecraft.util.Tuple<>(add, remove);
        }

        public TrinketPowerData(JsonObject jsonObject) {
            if (jsonObject == null) {
                this.accessoryPowers = new ArrayList<>();
                this.allFormPowerAdd = new ArrayList<>();
                this.allFormPowerRemove = new ArrayList<>();
                this.formPowerAdd = new HashMap<>();
                this.formPowerRemove = new HashMap<>();
                this.layerPowerAddMap = new HashMap<>();
                this.layerPowerRemoveMap = new HashMap<>();
                return;
            }
            List<ResourceLocation> accessoryPowers = new ArrayList<>();
            List<ResourceLocation> allFormPowerAdd = new ArrayList<>();
            List<ResourceLocation> allFormPowerRemove = new ArrayList<>();
            HashMap<ResourceLocation, List<ResourceLocation>> formPowerAdd = new HashMap<>();
            HashMap<ResourceLocation, List<ResourceLocation>> formPowerRemove = new HashMap<>();
            HashMap<ResourceLocation, HashMap<ResourceLocation, List<ResourceLocation>>> layerPowerAddMap = new HashMap<>();
            HashMap<ResourceLocation, HashMap<ResourceLocation, List<ResourceLocation>>> layerPowerRemoveMap = new HashMap<>();
            if (jsonObject.has("accessory_powers") && jsonObject.get("accessory_powers").isJsonArray()) {
                jsonObject.get("accessory_powers").getAsJsonArray().forEach(e -> {
                    ResourceLocation id = ResourceLocation.tryParse(e.getAsString());
                    if (id != null) accessoryPowers.add(id);
                });
            }
            if (jsonObject.has("all_form") && jsonObject.get("all_form").isJsonObject()) {
                var pair = parsePowerList(jsonObject.get("all_form").getAsJsonObject());
                allFormPowerAdd = pair.getA();
                allFormPowerRemove = pair.getB();
            }
            if (jsonObject.has("forms") && jsonObject.get("forms").isJsonObject()) {
                JsonObject formData = jsonObject.get("forms").getAsJsonObject();
                for (String formID : formData.keySet()) {
                    ResourceLocation currentFormID = ResourceLocation.tryParse(formID);
                    if (currentFormID == null) continue;
                    var pair = parsePowerList(formData.get(formID).getAsJsonObject());
                    formPowerAdd.put(currentFormID, pair.getA());
                    formPowerRemove.put(currentFormID, pair.getB());
                }
            }
            if (jsonObject.has("layers") && jsonObject.get("layers").isJsonObject()) {
                JsonObject layerGroupData = jsonObject.get("layers").getAsJsonObject();
                for (String layerGroupID : layerGroupData.keySet()) {
                    ResourceLocation currentLayerGroupID = ResourceLocation.tryParse(layerGroupID);
                    if (currentLayerGroupID == null) continue;
                    HashMap<ResourceLocation, List<ResourceLocation>> addMap = new HashMap<>();
                    HashMap<ResourceLocation, List<ResourceLocation>> removeMap = new HashMap<>();
                    JsonObject layerData = layerGroupData.get(layerGroupID).getAsJsonObject();
                    for (String layerID : layerData.keySet()) {
                        ResourceLocation currentLayerID = ResourceLocation.tryParse(layerID);
                        if (currentLayerID == null) continue;
                        var pair = parsePowerList(layerData.get(layerID).getAsJsonObject());
                        addMap.put(currentLayerID, pair.getA());
                        removeMap.put(currentLayerID, pair.getB());
                    }
                    layerPowerAddMap.put(currentLayerGroupID, addMap);
                    layerPowerRemoveMap.put(currentLayerGroupID, removeMap);
                }
            }
            this.accessoryPowers = accessoryPowers;
            this.allFormPowerAdd = allFormPowerAdd;
            this.allFormPowerRemove = allFormPowerRemove;
            this.formPowerAdd = formPowerAdd;
            this.formPowerRemove = formPowerRemove;
            this.layerPowerAddMap = layerPowerAddMap;
            this.layerPowerRemoveMap = layerPowerRemoveMap;
        }

        public void onPlayerEquip(Player player, ResourceLocation itemID) {}
        public void onPlayerUnEquip(Player player, ResourceLocation itemID) {}
        public void onPlayerFormChangeReApply(Player player) {}
    }

    public static final HashMap<ResourceLocation, TrinketPowerData> accessoryPowerRegistry = new HashMap<>();
    private static final HashMap<ResourceLocation, Boolean> accessoryMixinAutoRegistry = new HashMap<>();

    public static void registerAccessoryPower(ResourceLocation itemIdentifier, TrinketPowerData powerData) {
        if (accessoryPowerRegistry.containsKey(itemIdentifier)) {
            // merge omitted for brevity
        } else {
            accessoryPowerRegistry.put(itemIdentifier, powerData);
        }
    }

    public static void registerAccessoryMixinAuto(ResourceLocation itemIdentifier, boolean auto) {
        accessoryMixinAutoRegistry.put(itemIdentifier, auto);
    }

    @Nullable
    public static TrinketPowerData getAccessoryPower(ResourceLocation itemIdentifier) {
        return accessoryPowerRegistry.get(itemIdentifier);
    }

    public static boolean getAccessoryMixinAuto(ResourceLocation itemIdentifier) {
        return accessoryMixinAutoRegistry.getOrDefault(itemIdentifier, true);
    }

    public static void ApplyAccessoryPowerOnEquip(Player player, ResourceLocation accessoryID) {}
    public static void ApplyAccessoryPowerOnUnEquip(Player player, ResourceLocation accessoryID) {}
    public static void ApplyAccessoryPowerOnPlayerFormChange(Player player, ResourceLocation accessoryID) {}

    public static List<net.minecraft.util.Tuple<AccessoryItem.SlotData, ItemStack>> getAllAccessory(Player player) {
        List<net.minecraft.util.Tuple<AccessoryItem.SlotData, ItemStack>> allAccessory = new ArrayList<>();
        for (Map.Entry<String, AccessoryUtils.AccessoryIO> entry : AccessoryUtils.activeAccessoryModInterfaces.entrySet()) {
            String ioName = entry.getKey();
            AccessoryUtils.AccessoryIO io = entry.getValue();
            var allSlots = io.getEntitySlots(player);
            if (allSlots != null) {
                for (Map.Entry<net.minecraft.util.Tuple<String, String>, List<ItemStack>> e : allSlots.entrySet()) {
                    var slotPair = e.getKey();
                    List<ItemStack> stacks = e.getValue();
                    int idx = 0;
                    for (ItemStack stack : stacks) {
                        if (stack.getItem() instanceof AccessoryItem && io != AccessoryUtils.nowAccessoryMod) continue;
                        AccessoryItem.SlotData data;
                        if (slotPair.getA() == null) {
                            data = new AccessoryItem.SlotData(ResourceLocation.fromNamespaceAndPath(ioName, slotPair.getB()), idx);
                        } else {
                            data = new AccessoryItem.SlotData(ResourceLocation.fromNamespaceAndPath(ioName, "%s/%s".formatted(slotPair.getA(), slotPair.getB())), idx);
                        }
                        allAccessory.add(new net.minecraft.util.Tuple<>(data, stack));
                        idx++;
                    }
                }
            }
        }
        return allAccessory;
    }

    public static void ReApplyAccessoryPowerOnPlayerFormChange(Player player) {
        List<net.minecraft.util.Tuple<AccessoryItem.SlotData, ItemStack>> allAccessory = getAllAccessory(player);
        for (var pair : allAccessory) {
            ItemStack stack = pair.getB();
            if (stack.getItem() instanceof CustomPowerTrinketInterface cpti) {
                cpti.onFormChange(stack, pair.getA(), player);
            }
        }
    }

    public static void loadAccessoryPowerData(JsonObject jsonObject) {
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            ResourceLocation itemID = ResourceLocation.tryParse(entry.getKey());
            if (itemID == null) continue;
            if (!entry.getValue().isJsonObject()) continue;
            TrinketPowerData powerData = new TrinketPowerData(entry.getValue().getAsJsonObject());
            registerAccessoryPower(itemID, powerData);
        }
    }
}
