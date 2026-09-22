package net.onixary.shapeShifterCurseForge.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.onixary.shapeShifterCurseForge.menu.AltarMenu;
import net.onixary.shapeShifterCurseForge.recipe.altar.AltarRecipe;
import net.onixary.shapeShifterCurseForge.registry.ModBlockEntities;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AltarBlockEntity extends BaseContainerBlockEntity implements MenuProvider {
    public static final int MAX_FUEL = 102400;
    // lazy fuel map to avoid early registry access
    private static Map<net.minecraft.world.item.Item, Integer> fuelMap = null;
    private static Map<net.minecraft.world.item.Item, Integer> getFuelMap() {
        if (fuelMap == null) {
            fuelMap = new HashMap<>();
            try {
                var item = ModItems.UNTREATED_MOONDUST.get();
                fuelMap.put(item, 800);
            } catch (Exception ignored) {}
            // also allow moondust as item by registry name fallback
            try {
                var reg = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge.RESOURCE_NAMESPACE, "untreated_moondust"));
                if (reg != null) fuelMap.putIfAbsent(reg, 800);
            } catch (Exception ignored) {}
        }
        return fuelMap;
    }
    public static boolean canFuel(ItemStack s) { return getFuelMap().containsKey(s.getItem()); }
    public static int getFuelTime(ItemStack s) { return getFuelMap().getOrDefault(s.getItem(), 0); }

    private NonNullList<ItemStack> items = NonNullList.withSize(11, ItemStack.EMPTY);
    public UUID lastUser;
    private AltarRecipe currentRecipe;
    public int progress;
    public int totalProgress;
    public int fuelTime;
    private boolean needCheckRecipe = true;

    public final ContainerData dataAccess = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> totalProgress;
                case 2 -> fuelTime;
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {
            switch (i) {
                case 0 -> progress = v;
                case 1 -> totalProgress = v;
                case 2 -> fuelTime = v;
            }
        }
        @Override public int getCount() { return 3; }
    };

    public AltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALTAR.get(), pos, state);
    }

    @Override protected Component getDefaultName() { return Component.translatable("block.shape-shifter-curse.altar"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inv) { return new AltarMenu(id, inv, this, dataAccess); }
    @Override public int getContainerSize() { return items.size(); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> list) { items = list; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < 9) return true;
        if (slot == 9) return canFuel(stack) || (currentRecipe != null && currentRecipe.getCatalyst() != null && currentRecipe.getCatalyst().test(stack));
        return false;
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        if (tag.hasUUID("LastUser")) lastUser = tag.getUUID("LastUser");
        fuelTime = tag.getInt("FuelTime");
        progress = tag.getInt("Progress");
        totalProgress = tag.getInt("TotalProgress");
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        if (lastUser != null) tag.putUUID("LastUser", lastUser);
        tag.putInt("FuelTime", fuelTime);
        tag.putInt("Progress", progress);
        tag.putInt("TotalProgress", totalProgress);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AltarBlockEntity be) {
        if (level.isClientSide) return;
        if (be.needCheckRecipe) { be.checkRecipe(); be.needCheckRecipe = false; }
        boolean dirty = false;
        // fuel handling
        ItemStack fuelStack = be.items.get(9);
        // Note: slot 9 is shared for catalyst/fuel, fabric uses same slot. We treat fuel only if catalyst not required or already matched.
        // For simplicity, if fuelStack is fuel and not catalyst for current recipe, consume it.
        if (!fuelStack.isEmpty() && canFuel(fuelStack) && be.fuelTime + getFuelTime(fuelStack) <= MAX_FUEL) {
            // only consume if we need fuel and catalyst test would still pass after 1 consumption?
            // fabric consumes 1 fuel per tick batch, we mimic: if recipe needs fuel, consume one fuel item to add fuelTime
            if (be.currentRecipe == null || be.currentRecipe.getCatalyst() == null || !be.currentRecipe.getCatalyst().test(fuelStack)) {
                // fuel item not catalyst, safe to consume
                be.fuelTime += getFuelTime(fuelStack);
                fuelStack.shrink(1);
                dirty = true;
            } else {
                // catalyst is in slot 9 and also fuel? check if stack count >1, we can split
                if (fuelStack.getCount() > 1) {
                    // keep one for catalyst, consume one for fuel not possible without extra slot, skip
                }
            }
        }
        if (be.currentRecipe != null) {
            int fuelCost = be.currentRecipe.getFuelCost();
            if (be.fuelTime >= fuelCost) {
                be.fuelTime -= fuelCost;
                be.progress++;
                dirty = true;
            } else if (be.progress > 0) {
                be.progress--;
                dirty = true;
            }
            if (be.progress >= be.currentRecipe.getRecipeTime()) {
                if (be.craftRecipe()) {
                    be.progress = 0;
                    dirty = true;
                }
            }
        } else if (be.progress > 0) {
            be.progress = 0; dirty = true;
        }
        if (dirty) { be.checkRecipe(); be.setChanged(); }
    }

    private void checkRecipe() {
        if (level == null) return;
        if (currentRecipe != null) {
            // quick validate still matches
            if (currentRecipe.matches(this, level) && currentRecipe.canCraft(getLastPlayer())) {
                // also need output space
                if (canOutput(currentRecipe)) return;
            }
            currentRecipe = null; totalProgress = 0;
        }
        // lazy lookup to avoid early registry access
        net.minecraft.world.item.crafting.RecipeType<AltarRecipe> type = (net.minecraft.world.item.crafting.RecipeType<AltarRecipe>)(net.minecraft.world.item.crafting.RecipeType<?>)
                net.onixary.shapeShifterCurseForge.registry.ModRecipeSerializers.ALTAR_SHAPELESS_TYPE.get();
        Optional<AltarRecipe> opt = level.getRecipeManager().getRecipeFor(type, this, level);
        if (opt.isEmpty()) {
            currentRecipe = null; totalProgress = 0; progress = 0;
            return;
        }
        AltarRecipe r = opt.get();
        if (!r.canCraft(getLastPlayer())) { currentRecipe = null; totalProgress = 0; return; }
        if (!canOutput(r)) { currentRecipe = null; totalProgress = 0; return; }
        currentRecipe = r;
        totalProgress = r.getRecipeTime();
        progress = 0;
    }

    private net.minecraft.world.entity.player.Player getLastPlayer() {
        if (level == null || lastUser == null) return null;
        return level.getPlayerByUUID(lastUser);
    }

    private boolean canOutput(AltarRecipe r) {
        ItemStack outSlot = items.get(10);
        ItemStack result = r.assemble(this, level.registryAccess());
        if (result.isEmpty()) return false;
        if (outSlot.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(result, outSlot)) return false;
        return outSlot.getCount() + result.getCount() <= outSlot.getMaxStackSize() && outSlot.getCount() + result.getCount() <= getMaxStackSize();
    }

    private boolean craftRecipe() {
        if (currentRecipe == null || level == null) return false;
        if (!canOutput(currentRecipe)) return false;
        ItemStack result = currentRecipe.assemble(this, level.registryAccess());
        ItemStack outSlot = items.get(10);
        if (outSlot.isEmpty()) items.set(10, result.copy());
        else outSlot.grow(result.getCount());
        var ings = ((net.onixary.shapeShifterCurseForge.recipe.altar.AltarShapelessRecipe)currentRecipe).getIngredients();
        for (var ing : ings) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = items.get(i);
                if (!s.isEmpty() && ing.test(s)) { s.shrink(1); break; }
            }
        }
        if (currentRecipe.getCatalyst() != null && !currentRecipe.getCatalyst().isEmpty()) {
            ItemStack cat = items.get(9);
            if (!cat.isEmpty() && currentRecipe.getCatalyst().test(cat)) cat.shrink(1);
        }
        return true;
    }

    @Override public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        needCheckRecipe = true;
    }
    @Override public ItemStack removeItem(int slot, int count) { needCheckRecipe = true; return super.removeItem(slot, count); }
    @Override public ItemStack removeItemNoUpdate(int slot) { needCheckRecipe = true; return super.removeItemNoUpdate(slot); }
}
