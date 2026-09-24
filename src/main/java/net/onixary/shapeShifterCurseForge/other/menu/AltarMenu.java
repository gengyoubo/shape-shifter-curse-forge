package net.onixary.shapeShifterCurseForge.other.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.registry.ModMenuTypes;

public class AltarMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    public AltarMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenuTypes.ALTAR.get(), id);
        this.container = container; this.data = data;
        // 3x3 input 0-8
        for (int y=0;y<3;y++) for(int x=0;x<3;x++) addSlot(new Slot(container, x+y*3, 30 + x*18, 17 + y*18));
        // catalyst/fuel 9
        addSlot(new Slot(container, 9, 152, 57));
        // output 10
        addSlot(new Slot(container, 10, 124, 35) { @Override public boolean mayPlace(ItemStack s){ return false; }
        });
        // player
        for(int y=0;y<3;y++) for(int x=0;x<9;x++) addSlot(new Slot(inv, x+y*9+9, 8+x*18, 84+y*18));
        for(int x=0;x<9;x++) addSlot(new Slot(inv, x, 8+x*18, 142));
        addDataSlots(data);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem(); copy = stack.copy();
            if (index < 11) {
                if (!moveItemStackTo(stack, 11, 47, true)) return ItemStack.EMPTY;
            } else {
                // try fuel/catalyst slot 9 or inputs 0-8
                if (!moveItemStackTo(stack, 0, 9, false) && !moveItemStackTo(stack, 9, 10, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        }
        return copy;
    }
    @Override public boolean stillValid(Player p) { return container.stillValid(p); }
    public int getProgress(){ return data.get(0); }
    public int getTotal(){ return data.get(1); }
    public int getFuel(){ return data.get(2); }
}
