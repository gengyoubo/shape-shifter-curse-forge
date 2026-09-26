package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class ScareCreeperMixin extends Monster {
    private static final ResourceLocation SSC_SCARE_CREEPERS =
            ResourceLocation.fromNamespaceAndPath("shape-shifter-curse", "scare_creepers");

    protected ScareCreeperMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ssc$scareCreepers(CallbackInfo ci) {
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Player.class, 3.0F, 1.0D, 1.2D,
                entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator()
                        && FormPowerRegistry.has(player, SSC_SCARE_CREEPERS)));
        for (WrappedGoal wrapped : this.targetSelector.getAvailableGoals().toArray(WrappedGoal[]::new)) {
            if (wrapped.getPriority() == 1 && wrapped.getGoal() instanceof NearestAttackableTargetGoal<?>) {
                this.targetSelector.removeGoal(wrapped.getGoal());
            }
        }
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true,
                entity -> !(entity instanceof Player player && FormPowerRegistry.has(player, SSC_SCARE_CREEPERS))));
    }
}
