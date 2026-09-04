package net.onixary.shapeShifterCurseForge.util.Accessory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Tuple;
import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;
import java.util.*;

public class AccessoryUtils {
    public interface AccessoryIO {
        default int priority() { return 1000; }
        default boolean canLoaded() { return true; }
        Map<Tuple<String, String>, List<ItemStack>> getEntitySlots(LivingEntity entity);
        List<ItemStack> getEntitySlot(LivingEntity entity, String slotGroup, String slotName);
        @Nullable ItemStack getEntitySlot(LivingEntity entity, String slotGroup, String slotName, int index);
        void setEntitySlot(LivingEntity entity, String slotGroup, String slotName, int index, ItemStack stack);
    }

    // Curios backend
    public static class CuriosIO implements AccessoryIO {
        @Override
        public int priority() { return 100; }
        @Override
        public boolean canLoaded() {
            try { Class.forName("top.theillusivec4.curios.api.CuriosApi"); return true; } catch (ClassNotFoundException e) { return false; }
        }
        @Override
        public Map<Tuple<String, String>, List<ItemStack>> getEntitySlots(LivingEntity entity) {
            Map<Tuple<String, String>, List<ItemStack>> map = new HashMap<>();
            CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
                handler.getCurios().forEach((id, stacksHandler) -> {
                    var stacks = stacksHandler.getStacks();
                    List<ItemStack> list = new ArrayList<>();
                    for (int i = 0; i < stacksHandler.getSlots(); i++) {
                        list.add(stacks.getStackInSlot(i));
                    }
                    map.put(new Tuple<>(null, id), list);
                });
            });
            return map;
        }
        @Override
        public List<ItemStack> getEntitySlot(LivingEntity entity, String slotGroup, String slotName) {
            List<ItemStack> list = new ArrayList<>();
            CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
                handler.getStacksHandler(slotName).ifPresent(h -> {
                    for (int i = 0; i < h.getSlots(); i++) list.add(h.getStacks().getStackInSlot(i));
                });
            });
            return list;
        }
        @Override
        public ItemStack getEntitySlot(LivingEntity entity, String slotGroup, String slotName, int index) {
            List<ItemStack> list = getEntitySlot(entity, slotGroup, slotName);
            if (index < list.size()) return list.get(index);
            return ItemStack.EMPTY;
        }
        @Override
        public void setEntitySlot(LivingEntity entity, String slotGroup, String slotName, int index, ItemStack stack) {
            CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
                handler.getStacksHandler(slotName).ifPresent(h -> h.getStacks().setStackInSlot(index, stack));
            });
        }
    }

    public static final Map<String, AccessoryIO> accessoryModInterfaces = new HashMap<>();
    public static final Map<String, AccessoryIO> activeAccessoryModInterfaces = new HashMap<>();
    public static String nowAccessoryModID = "";
    public static AccessoryIO nowAccessoryMod = null;

    static {
        registerAccessoryMod("curios", new CuriosIO());
        reCalcAccessoryMod();
    }

    public static void registerAccessoryMod(String id, AccessoryIO io) { accessoryModInterfaces.put(id, io); }

    public static void reCalcAccessoryMod() {
        nowAccessoryModID = "";
        nowAccessoryMod = null;
        activeAccessoryModInterfaces.clear();
        if (accessoryModInterfaces.isEmpty()) return;
        List<Tuple<AccessoryIO, Integer>> list = new ArrayList<>();
        for (var e : accessoryModInterfaces.entrySet()) {
            if (e.getValue().canLoaded()) {
                list.add(new Tuple<>(e.getValue(), e.getValue().priority()));
                activeAccessoryModInterfaces.put(e.getKey(), e.getValue());
            }
        }
        list.sort((a,b) -> b.getB() - a.getB());
        if (!list.isEmpty()) nowAccessoryMod = list.get(0).getA();
        for (var e : accessoryModInterfaces.entrySet()) if (e.getValue() == nowAccessoryMod) nowAccessoryModID = e.getKey();
    }

    public static void onPlayerEquip(Player player, ResourceLocation itemID, String pluginID) {}
    public static void onPlayerUnEquip(Player player, ResourceLocation itemID, String pluginID) {}
    public static boolean CanAutoExecute(ResourceLocation itemID, String pluginID) { return true; }
    public static void onStartServer() {}

    @Nullable
    public static Map<Tuple<String, String>, List<ItemStack>> getEntitySlots(LivingEntity entity, @Nullable String modID) {
        if (nowAccessoryMod == null) return null;
        if (modID == null || modID.equals("auto")) return nowAccessoryMod.getEntitySlots(entity);
        var io = accessoryModInterfaces.get(modID);
        return io == null ? null : io.getEntitySlots(entity);
    }
    @Nullable
    public static List<ItemStack> getEntitySlot(LivingEntity entity, @Nullable String modID, String group, String name) {
        if (nowAccessoryMod == null) return null;
        if (modID == null || modID.equals("auto")) return nowAccessoryMod.getEntitySlot(entity, group, name);
        var io = accessoryModInterfaces.get(modID);
        return io == null ? null : io.getEntitySlot(entity, group, name);
    }
    @Nullable
    public static ItemStack getEntitySlot(LivingEntity entity, @Nullable String modID, String group, String name, int idx) {
        if (nowAccessoryMod == null) return null;
        if (modID == null || modID.equals("auto")) return nowAccessoryMod.getEntitySlot(entity, group, name, idx);
        var io = accessoryModInterfaces.get(modID);
        return io == null ? null : io.getEntitySlot(entity, group, name, idx);
    }
    public static void setEntitySlot(LivingEntity entity, @Nullable String modID, String group, String name, int idx, ItemStack stack) {
        if (nowAccessoryMod == null) return;
        if (modID == null || modID.equals("auto")) nowAccessoryMod.setEntitySlot(entity, group, name, idx, stack);
        else { var io = accessoryModInterfaces.get(modID); if (io != null) io.setEntitySlot(entity, group, name, idx, stack); }
    }
}
