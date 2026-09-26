package net.onixary.shapeShifterCurseForge.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric adds a normal target goal for players with hostile_iron_golem. */
@Mixin(IronGolem.class)
public abstract class IronGolemTargetMixin extends AbstractGolem {
    private static final ResourceLocation SSC_HOSTILE_GOLEM = ResourceLocation.fromNamespaceAndPath(
            "shape-shifter-curse", "hostile_iron_golem");

    protected IronGolemTargetMixin(EntityType<? extends AbstractGolem> type, Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ssc$addHostileSpiderTarget(CallbackInfo ci) {
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class,
                10, true, false, entity -> entity instanceof Player player
                && FormPowerRegistry.has(player, SSC_HOSTILE_GOLEM)));
    }
}
