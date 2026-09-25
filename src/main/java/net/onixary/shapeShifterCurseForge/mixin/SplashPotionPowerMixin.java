package net.onixary.shapeShifterCurseForge.mixin;

import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Matches Fabric's three potion-entry hooks, including potions with no applied effects. */
@Mixin(ThrownPotion.class)
public abstract class SplashPotionPowerMixin {
    @Inject(method = "applyWater", at = @At("HEAD"))
    private void ssc$onWaterSplash(CallbackInfo ci) {
        ThrownPotion potion = (ThrownPotion) (Object) this;
        if (potion.level().isClientSide) return;
        for (LivingEntity living : potion.level().getEntitiesOfClass(LivingEntity.class,
                potion.getBoundingBox().inflate(4.0D, 2.0D, 4.0D))) {
            if (living instanceof Player player && potion.distanceToSqr(player) < 16.0D) {
                ssc$activate(player, true);
            }
        }
    }

    @Inject(method = "applySplash", at = @At("HEAD"))
    private void ssc$onSplash(List<MobEffectInstance> effects, Entity directHit, CallbackInfo ci) {
        ThrownPotion potion = (ThrownPotion) (Object) this;
        if (potion.level().isClientSide) return;
        for (LivingEntity living : potion.level().getEntitiesOfClass(LivingEntity.class,
                potion.getBoundingBox().inflate(4.0D, 2.0D, 4.0D))) {
            if (living instanceof Player player && potion.distanceToSqr(player) < 16.0D) {
                ssc$activate(player, false);
            }
        }
    }

    @Inject(method = "makeAreaOfEffectCloud", at = @At("HEAD"))
    private void ssc$onLingeringSplash(ItemStack stack, Potion potionType, CallbackInfo ci) {
        if (!PotionUtils.getCustomEffects(stack).isEmpty()) return;
        ThrownPotion potion = (ThrownPotion) (Object) this;
        if (potion.level().isClientSide) return;
        for (LivingEntity living : potion.level().getEntitiesOfClass(LivingEntity.class,
                potion.getBoundingBox().inflate(3.0D, 2.0D, 3.0D))) {
            if (living instanceof Player player) ssc$activate(player, false);
        }
    }

    private static void ssc$activate(Player player, boolean requireNoEffectTrigger) {
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:action_on_splash_potion_take_effect".equals(
                    FormPowerRegistry.typeOf(power))) return;
            if (requireNoEffectTrigger
                    && !FormPowerRuntime.booleanValue(power, "trigger_on_no_effect", false)) return;
            JsonObject condition = power.getAsJsonObject("entity_condition");
            if (FormPowerRuntime.test(player, player, condition)) {
                FormPowerRuntime.execute(player, player, power.getAsJsonObject("entity_action"));
            }
        });
    }
}
