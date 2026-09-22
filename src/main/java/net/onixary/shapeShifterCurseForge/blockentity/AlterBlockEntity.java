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
import net.onixary.shapeShifterCurseForge.recipe.alter.AlterShapelessRecipe;
import net.onixary.shapeShifterCurseForge.registry.ModBlockEntities;
import net.onixary.shapeShifterCurseForge.registry.ModRecipeSerializers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AlterBlockEntity extends BaseContainerBlockEntity implements MenuProvider {
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
    @Override protected Component getDefaultName(){ return Component.translatable("block.shape-shifter-curse.alter"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inv){ return new AltarMenu(id, inv, this, data, true); }
    @Override public int getContainerSize(){ return 11; }
    @Override protected NonNullList<ItemStack> getItems(){ return items; }
    @Override protected void setItems(NonNullList<ItemStack> l){ items = l; }
    @Override public void load(CompoundTag t){ super.load(t); items = NonNullList.withSize(11, ItemStack.EMPTY); ContainerHelper.loadAllItems(t, items); if(t.hasUUID("LastUser")) lastUser=t.getUUID("LastUser"); fuelTime=t.getInt("FuelTime"); progress=t.getInt("Progress"); totalProgress=t.getInt("TotalProgress"); }
    @Override protected void saveAdditional(CompoundTag t){ super.saveAdditional(t); ContainerHelper.saveAllItems(t, items); if(lastUser!=null) t.putUUID("LastUser", lastUser); t.putInt("FuelTime", fuelTime); t.putInt("Progress", progress); t.putInt("TotalProgress", totalProgress); }

    public static void tick(Level lvl, BlockPos pos, BlockState st, AlterBlockEntity be){
        if(lvl.isClientSide) return;
        if(be.needCheck){ be.checkRecipe(); be.needCheck=false; }
        boolean dirty=false;
        // fuel
        var fuelStack = be.items.get(9);
        if(!fuelStack.isEmpty() && AltarBlockEntity.canFuel(fuelStack) && be.fuelTime + AltarBlockEntity.getFuelTime(fuelStack) <= AltarBlockEntity.MAX_FUEL){
            if(be.current==null || be.current.getCatalyst()==null || !be.current.getCatalyst().test(fuelStack)){
                be.fuelTime += AltarBlockEntity.getFuelTime(fuelStack); fuelStack.shrink(1); dirty=true;
            }
        }
        if(be.current!=null){
            int cost = be.current.getFuelCost();
            if(be.fuelTime >= cost){ be.fuelTime-=cost; be.progress++; dirty=true; }
            else if(be.progress>0){ be.progress--; dirty=true; }
            if(be.progress >= be.current.getRecipeTime()){
                if(be.craft()){ be.progress=0; dirty=true; }
            }
        } else if(be.progress>0){ be.progress=0; dirty=true; }
        if(dirty){ be.checkRecipe(); be.setChanged(); }
    }
    private void checkRecipe(){
        if(level==null) return;
        if(current!=null && current.matches(this, level) && current.canCraft(getPlayer()) && canOutput(current)){ return; }
        current=null; totalProgress=0;
        var opt = level.getRecipeManager().getRecipeFor((net.minecraft.world.item.crafting.RecipeType<AlterShapelessRecipe>)(net.minecraft.world.item.crafting.RecipeType)ModRecipeSerializers.ALTER_SHAPELESS_TYPE.get(), this, level);
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
    @Override public void setItem(int s, ItemStack v){ super.setItem(s,v); needCheck=true; }
    @Override public ItemStack removeItem(int s,int c){ needCheck=true; return super.removeItem(s,c); }
    @Override public ItemStack removeItemNoUpdate(int s){ needCheck=true; return super.removeItemNoUpdate(s); }
}
