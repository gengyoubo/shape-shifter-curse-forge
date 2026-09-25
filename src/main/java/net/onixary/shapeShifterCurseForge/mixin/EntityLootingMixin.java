package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.power.CombatLootEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Consumer;

/** Matches Fabric's loot-table consumer hook, leaving other death drops alone. */
@Mixin(LivingEntity.class)
public abstract class EntityLootingMixin {
    @ModifyArg(method = "dropFromLootTable",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"),
            index = 2, require = 1)
    private Consumer<ItemStack> ssc$modifyGeneratedLoot(Consumer<ItemStack> original) {
        LivingEntity victim = (LivingEntity) (Object) this;
        return stack -> original.accept(CombatLootEvents.modifyEntityLoot(victim, stack));
    }
}
