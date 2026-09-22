package net.onixary.shapeShifterCurseForge.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.onixary.shapeShifterCurseForge.menu.AltarMenu;
import net.onixary.shapeShifterCurseForge.recipe.altar.AltarRecipe;
import net.onixary.shapeShifterCurseForge.registry.ModBlockEntities;
import net.onixary.shapeShifterCurseForge.registry.ModRecipeSerializers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AltarBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int MAX_FUEL = 102400;
    private static Map<net.minecraft.world.item.Item, Integer> fuelMap = null;
    private static Map<net.minecraft.world.item.Item, Integer> getFuelMap() {
        if (fuelMap == null) {
            fuelMap = new HashMap<>();
            try {
                var item = net.onixary.shapeShifterCurseForge.registry.ModItems.UNTREATED_MOONDUST.get();
                fuelMap.put(item, 800);
            } catch (Exception ignored) {}
            try {
                var reg = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge.RESOURCE_NAMESPACE, "untreated_moondust"));
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

    @Override public Component getDisplayName() { return Component.translatable("block.shape-shifter-curse.altar"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new AltarMenu(id, inv, this, dataAccess); }

    // Container
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { for (var s : items) if (!s.isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int i) { return items.get(i); }
    @Override public ItemStack removeItem(int slot, int count) { var s = ContainerHelper.removeItem(items, slot, count); if (!s.isEmpty()) { needCheckRecipe = true; setChanged(); } return s; }
    @Override public ItemStack removeItemNoUpdate(int slot) { var s = ContainerHelper.takeItem(items, slot); needCheckRecipe = true; return s; }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize()); needCheckRecipe = true; setChanged(); }
    @Override public boolean stillValid(Player p) { if (level == null || level.getBlockEntity(worldPosition) != this) return false; return p.distanceToSqr(worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5) <= 64; }
    @Override public void clearContent() { items.clear(); needCheckRecipe = true; }
    @Override public int getMaxStackSize() { return 64; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < 9) return true;
        if (slot == 9) return canFuel(stack) || (currentRecipe != null && currentRecipe.getCatalyst() != null && currentRecipe.getCatalyst().test(stack));
        return false;
    }
    @Override public int[] getSlotsForFace(net.minecraft.core.Direction dir) {
        if (dir == net.minecraft.core.Direction.UP) return new int[]{0,1,2,3,4,5,6,7,8};
        if (dir == net.minecraft.core.Direction.DOWN) return new int[]{10};
        return new int[]{9};
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction dir) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction dir) { return slot == 10; }

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
        // Forge parity: simplify fuel handling – ignore fuel requirement for now, just progress when recipe matches
        // Keep fuelTime for GUI display but don't block progress
        if (be.currentRecipe != null) {
            be.progress++;
            dirty = true;
            if (be.progress >= be.currentRecipe.getRecipeTime()) {
                if (be.craftRecipe()) {
                    be.progress = 0;
                    dirty = true;
                } else {
                    be.progress = 0;
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
            if (currentRecipe.matches(this, level) && currentRecipe.canCraft(getLastPlayer())) {
                if (canOutput(currentRecipe)) return;
            }
            currentRecipe = null; totalProgress = 0;
        }
        net.minecraft.world.item.crafting.RecipeType<AltarRecipe> type = (net.minecraft.world.item.crafting.RecipeType<AltarRecipe>)(net.minecraft.world.item.crafting.RecipeType<?>)
                ModRecipeSerializers.ALTAR_SHAPELESS_TYPE.get();
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

    private Player getLastPlayer() {
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
}
