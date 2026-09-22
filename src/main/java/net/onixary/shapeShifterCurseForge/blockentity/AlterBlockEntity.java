package net.onixary.shapeShifterCurseForge.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.onixary.shapeShifterCurseForge.recipe.alter.AlterShapelessRecipe;
import net.onixary.shapeShifterCurseForge.registry.ModBlockEntities;
import net.onixary.shapeShifterCurseForge.registry.ModRecipeSerializers;

import java.util.UUID;

public class AlterBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    private NonNullList<ItemStack> items = NonNullList.withSize(11, ItemStack.EMPTY);
    public UUID lastUser;
    private AlterShapelessRecipe current;
    public int progress, totalProgress, fuelTime;
    private boolean needCheck = true;
    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) { return switch(i){ case 0->progress; case 1->totalProgress; case 2->fuelTime; default->0; }; }
        @Override public void set(int i,int v){ switch(i){ case 0->progress=v; case 1->totalProgress=v; case 2->fuelTime=v; } }
        @Override public int getCount(){ return 3; }
    };
    public AlterBlockEntity(BlockPos p, BlockState s){ super(ModBlockEntities.ALTER.get(), p, s); }
    @Override public Component getDisplayName(){ return Component.translatable("block.shape-shifter-curse.alter"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player){ return new AltarMenu(id, inv, this, data, true); }
    @Override public int getContainerSize(){ return 11; }
    @Override public boolean isEmpty(){ for(var s: items) if(!s.isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int i){ return items.get(i); }
    @Override public ItemStack removeItem(int slot, int count){ var s = ContainerHelper.removeItem(items, slot, count); if(!s.isEmpty()){ needCheck=true; setChanged(); } return s; }
    @Override public ItemStack removeItemNoUpdate(int slot){ var s = ContainerHelper.takeItem(items, slot); needCheck=true; return s; }
    @Override public void setItem(int slot, ItemStack stack){ items.set(slot, stack); if(stack.getCount()>getMaxStackSize()) stack.setCount(getMaxStackSize()); needCheck=true; setChanged(); }
    @Override public boolean stillValid(Player p){ if(level==null||level.getBlockEntity(worldPosition)!=this) return false; return p.distanceToSqr(worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5)<=64; }
    @Override public void clearContent(){ items.clear(); needCheck=true; }
    @Override public int getMaxStackSize(){ return 64; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack){ if(slot<9) return true; if(slot==9) return AltarBlockEntity.canFuel(stack) || (current!=null && current.getCatalyst()!=null && current.getCatalyst().test(stack)); return false; }
    @Override public int[] getSlotsForFace(net.minecraft.core.Direction dir){ if(dir==net.minecraft.core.Direction.UP) return new int[]{0,1,2,3,4,5,6,7,8}; if(dir==net.minecraft.core.Direction.DOWN) return new int[]{10}; return new int[]{9}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction dir){ return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction dir){ return slot==10; }
    @Override public void load(CompoundTag t){ super.load(t); items = NonNullList.withSize(11, ItemStack.EMPTY); ContainerHelper.loadAllItems(t, items); if(t.hasUUID("LastUser")) lastUser=t.getUUID("LastUser"); fuelTime=t.getInt("FuelTime"); progress=t.getInt("Progress"); totalProgress=t.getInt("TotalProgress"); }
    @Override protected void saveAdditional(CompoundTag t){ super.saveAdditional(t); ContainerHelper.saveAllItems(t, items); if(lastUser!=null) t.putUUID("LastUser", lastUser); t.putInt("FuelTime", fuelTime); t.putInt("Progress", progress); t.putInt("TotalProgress", totalProgress); }

    public static void tick(Level lvl, BlockPos pos, BlockState st, AlterBlockEntity be){
        if(lvl.isClientSide) return;
        if(be.needCheck){ be.checkRecipe(); be.needCheck=false; }
        boolean dirty=false;
        if(be.current!=null){
            be.progress++; dirty=true;
            if(be.progress >= be.current.getRecipeTime()){
                if(be.craft()){ be.progress=0; dirty=true; } else be.progress=0;
            }
        } else if(be.progress>0){ be.progress=0; dirty=true; }
        if(dirty){ be.checkRecipe(); be.setChanged(); }
    }
    private void checkRecipe(){
        if(level==null) return;
        if(current!=null && current.matches(this, level) && current.canCraft(getPlayer()) && canOutput(current)){ return; }
        current=null; totalProgress=0;
        var type = (net.minecraft.world.item.crafting.RecipeType<AlterShapelessRecipe>)(net.minecraft.world.item.crafting.RecipeType<?>)ModRecipeSerializers.ALTER_SHAPELESS_TYPE.get();
        var opt = level.getRecipeManager().getRecipeFor(type, this, level);
        if(opt.isEmpty()){ progress=0; return; }
        var r = opt.get();
        if(!r.canCraft(getPlayer()) || !canOutput(r)){ progress=0; return; }
        current=r; totalProgress=r.getRecipeTime(); progress=0;
    }
    private Player getPlayer(){ if(level==null||lastUser==null) return null; return level.getPlayerByUUID(lastUser); }
    private boolean canOutput(AlterShapelessRecipe r){
        var out = items.get(10); var res = r.assemble(this, level.registryAccess()); if(res.isEmpty()) return false;
        if(out.isEmpty()) return true;
        if(!ItemStack.isSameItemSameTags(res, out)) return false;
        return out.getCount()+res.getCount() <= out.getMaxStackSize();
    }
    private boolean craft(){
        if(current==null||level==null) return false;
        if(!canOutput(current)) return false;
        var res = current.assemble(this, level.registryAccess());
        var out = items.get(10);
        if(out.isEmpty()) items.set(10, res.copy()); else out.grow(res.getCount());
        for(var ing: current.getIngredients()){
            for(int i=0;i<9;i++){ var s=items.get(i); if(!s.isEmpty()&&ing.test(s)){ s.shrink(1); break; } }
        }
        if(current.getCatalyst()!=null && !current.getCatalyst().isEmpty()){
            var c=items.get(9); if(!c.isEmpty()&&current.getCatalyst().test(c)) c.shrink(1);
        }
        return true;
    }
}
