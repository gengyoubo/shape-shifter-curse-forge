package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies simple_looting powers to the actual looting enchantment level, matching Fabric. */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperLootingMixin {
    @Inject(method = "getEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;)I",
            at = @At("RETURN"), cancellable = true)
    private static void ssc$applySimpleLooting(Enchantment enchantment, LivingEntity entity,
                                               CallbackInfoReturnable<Integer> cir) {
        if (!(entity instanceof Player player) || enchantment != Enchantments.MOB_LOOTING) return;

        int[] level = {cir.getReturnValue()};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:simple_looting".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            int addition = FormPowerRuntime.intValue(power, "level", 1);
            int maximum = FormPowerRuntime.intValue(power, "max_level", Integer.MAX_VALUE);
            level[0] = Math.min(level[0] + addition, maximum);
        });
        cir.setReturnValue(level[0]);
    }
}
