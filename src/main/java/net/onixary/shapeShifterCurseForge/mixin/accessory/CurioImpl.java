package net.onixary.shapeShifterCurseForge.mixin.accessory;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import org.spongepowered.asm.mixin.Mixin;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

/**
 * Forge Curios bridge: every AccessoryItem automatically behaves as a Curio.
 * Registration is via CuriosApi (auto-registered by the ICurio mixin contract);
 * slot mapping is supplied by data/curios/tags/items/*.json already copied.
 */
@Mixin(AccessoryItem.class)
public abstract class CurioImpl implements ICurio {

    @Override
    public ItemStack getStack() {
        // ICurio default; real stack is supplied by the curio capability wrapper
        return ItemStack.EMPTY;
    }

    private ItemStack resolveStack(SlotContext ctx) {
        try {
            var opt = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(ctx.entity()).resolve();
            if (opt.isPresent()) {
                var handler = opt.get();
                var stacksOpt = handler.getStacksHandler(ctx.identifier());
                if (stacksOpt.isPresent()) {
                    return stacksOpt.get().getStacks().getStackInSlot(ctx.index());
                }
            }
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    private static net.minecraft.resources.ResourceLocation toSlotId(SlotContext ctx) {
        String id = ctx.identifier();
        net.minecraft.resources.ResourceLocation parsed = net.minecraft.resources.ResourceLocation.tryParse(id);
        return parsed != null ? parsed : net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("curios", id);
    }

    @Override
    public void curioTick(SlotContext slotContext) {
        AccessoryItem self = (AccessoryItem) (Object) this;
        self.accessoryTick(resolveStack(slotContext), slotContext.entity(),
                new AccessoryItem.SlotData(toSlotId(slotContext), slotContext.index()));
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack) {
        AccessoryItem self = (AccessoryItem) (Object) this;
        self.onEquip(resolveStack(slotContext), slotContext.entity(),
                new AccessoryItem.SlotData(toSlotId(slotContext), slotContext.index()));
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack stack) {
        AccessoryItem self = (AccessoryItem) (Object) this;
        self.onUnequip(stack, slotContext.entity(),
                new AccessoryItem.SlotData(toSlotId(slotContext), slotContext.index()));
    }

    @Override
    public boolean canEquip(SlotContext slotContext) {
        AccessoryItem self = (AccessoryItem) (Object) this;
        return self.canEquip(resolveStack(slotContext), slotContext.entity(),
                new AccessoryItem.SlotData(toSlotId(slotContext), slotContext.index()));
    }

    @Override
    public boolean canUnequip(SlotContext slotContext) {
        AccessoryItem self = (AccessoryItem) (Object) this;
        return self.canUnequip(resolveStack(slotContext), slotContext.entity(),
                new AccessoryItem.SlotData(toSlotId(slotContext), slotContext.index()));
    }

    @Override
    public void curioBreak(SlotContext slotContext) {
        AccessoryItem self = (AccessoryItem) (Object) this;
        self.onBreak(resolveStack(slotContext), slotContext.entity(),
                new AccessoryItem.SlotData(toSlotId(slotContext), slotContext.index()));
    }
}
