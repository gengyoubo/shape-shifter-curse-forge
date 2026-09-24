package net.onixary.shapeShifterCurseForge.mixin;

import com.google.gson.JsonObject;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.other.config.SscCommonConfig;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import net.onixary.shapeShifterCurseForge.registry.ModPotions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mirrors Fabric's custom familiar-fox potion attack for eligible players. */
@Mixin(Witch.class)
public abstract class WitchEntityMixin {
    private static final float SSC_POTION_REPLACE_CHANCE = 0.6F;

    @Inject(method = "performRangedAttack", at = @At("HEAD"), cancellable = true)
    private void ssc$throwFamiliarFoxPotion(LivingEntity target, float pullProgress, CallbackInfo callback) {
        if (!(target instanceof Player player)) {
            return;
        }

        Witch witch = (Witch) (Object) this;
        if (ssc$isWitchFriendly(player, witch)) {
            callback.cancel();
            return;
        }

        var currentForm = FormManager.current(player);
        boolean eligible = currentForm.id().equals(FormRegistry.ORIGINAL_SHIFTER)
                || (SscCommonConfig.WITCH_POTION_FOR_PRE_BOOK.get()
                && currentForm.id().equals(FormRegistry.ORIGINAL_BEFORE_ENABLE));
        if (!eligible || witch.getRandom().nextFloat() >= SSC_POTION_REPLACE_CHANCE) {
            return;
        }

        Level level = witch.level();
        Vec3 targetVelocity = target.getDeltaMovement();
        double dx = target.getX() + targetVelocity.x - witch.getX();
        double dy = target.getEyeY() - 1.1D - witch.getY();
        double dz = target.getZ() + targetVelocity.z - witch.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        ThrownPotion potion = new ThrownPotion(level, witch);
        ItemStack potionStack = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION),
                ModPotions.TO_FAMILIAR_FOX.get());
        potion.setItem(potionStack);
        potion.setXRot(potion.getXRot() + 20.0F);
        potion.shoot(dx, dy + horizontalDistance * 0.2D, dz, 0.75F, 8.0F);

        if (!witch.isSilent()) {
            level.playSound(null, witch.getX(), witch.getY(), witch.getZ(), SoundEvents.WITCH_THROW,
                    witch.getSoundSource(), 1.0F, 0.8F);
        }
        level.addFreshEntity(potion);
        callback.cancel();
    }

    private static boolean ssc$isWitchFriendly(Player player, Witch witch) {
        boolean[] friendly = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:witch_friendly".equals(FormPowerRegistry.typeOf(power))) {
                JsonObject condition = power.getAsJsonObject("condition");
                if (FormPowerRuntime.test(player, witch, condition)) {
                    friendly[0] = true;
                }
            }
        });
        return friendly[0];
    }
}
