package net.onixary.shapeShifterCurseForge.mixin.accessory;

import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import org.spongepowered.asm.mixin.Mixin;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Forge Curios bridge: every AccessoryItem automatically behaves as a Curio item.
 * Curios wraps this interface in its item capability and supplies the real stack
 * to every callback. Slot mapping is supplied by data/curios/tags/items/*.json.
 */
@Mixin(AccessoryItem.class)
public abstract class CurioImpl implements ICurioItem {

    private static net.minecraft.resources.ResourceLocation toSlotId(SlotContext ctx) {
        String id = ctx.identifier();
        net.minecraft.resources.ResourceLocation parsed = net.minecraft.resources.ResourceLocation.tryParse(id);
        return parsed != null ? parsed : net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("curios", id);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        AccessoryItem self = (AccessoryItem) (Object) this;
        self.accessoryTick(stack, slotContext.entity(),
                new AccessoryItem.SlotData(toSlotId(slotContext), slotContext.index()));
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack stack, ItemStack prevStack) {
        AccessoryItem self = (AccessoryItem) (Object) this;
        self.onEquip(stack, slotContext.entity(),
                new AccessoryItem.SlotData(toSlotId(slotContext), slotContext.index()));
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack stack, ItemStack newStack) {
        AccessoryItem self = (AccessoryItem) (Object) this;
        self.onUnequip(stack, slotContext.entity(),
                new AccessoryItem.SlotData(toSlotId(slotContext), slotContext.index()));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        AccessoryItem self = (AccessoryItem) (Object) this;
        return self.canEquip(stack, slotContext.entity(),
                new AccessoryItem.SlotData(toSlotId(slotContext), slotContext.index()));
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        AccessoryItem self = (AccessoryItem) (Object) this;
        return self.canUnequip(stack, slotContext.entity(),
                new AccessoryItem.SlotData(toSlotId(slotContext), slotContext.index()));
    }

    @Override
    public void curioBreak(SlotContext slotContext, ItemStack stack) {
        AccessoryItem self = (AccessoryItem) (Object) this;
        self.onBreak(stack, slotContext.entity(),
                new AccessoryItem.SlotData(toSlotId(slotContext), slotContext.index()));
    }
}
