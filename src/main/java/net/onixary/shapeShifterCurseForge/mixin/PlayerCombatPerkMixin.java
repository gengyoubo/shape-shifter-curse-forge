package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.CombatPerkService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerCombatPerkMixin {
    @Unique private boolean ssc$criticalAttack;

    // Player.attack stores the final Forge-adjusted critical flag in local slot 8.
    @ModifyVariable(method = "attack", at = @At("STORE"), index = 8)
    private boolean ssc$captureCritical(boolean critical) {
        ssc$criticalAttack = critical;
        return critical;
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean ssc$primaryHit(Entity target, DamageSource source, float amount) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide) return target.hurt(source, amount);
        float before = CombatPerkService.health(target);
        boolean critical = ssc$criticalAttack;
        boolean success = target.hurt(source, amount + CombatPerkService.bonus(player, target, critical));
        if (success && target instanceof LivingEntity living) {
            float dealt = before - CombatPerkService.health(target);
            CombatPerkService.fire(player, living, "melee", dealt);
            CombatPerkService.fire(player, living, critical ? "critical" : "non_critical_melee", dealt);
        }
        return success;
    }
}
