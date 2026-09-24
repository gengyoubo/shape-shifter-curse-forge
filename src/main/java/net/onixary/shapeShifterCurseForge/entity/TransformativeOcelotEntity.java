package net.onixary.shapeShifterCurseForge.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;

import java.util.function.Predicate;

public final class TransformativeOcelotEntity extends Ocelot {
    private SscFleeGoal ssc$fleePlayers;

    public TransformativeOcelotEntity(EntityType<? extends Ocelot> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                candidate -> candidate instanceof Player player && ssc$isOriginalShifter(player)));
    }

    private static boolean ssc$isOriginalShifter(Player player) {
        return FormManager.current(player).id().equals(
                ResourceLocation.fromNamespaceAndPath("shape-shifter-curse", "original_shifter"));
    }

    @Override
    protected void reassessTrustingGoals() {
        if (this.ssc$fleePlayers == null) {
            this.ssc$fleePlayers = new SscFleeGoal(this);
        }
        this.goalSelector.removeGoal(this.ssc$fleePlayers);
        if (!this.isTrusting()) this.goalSelector.addGoal(4, this.ssc$fleePlayers);
    }

    private static boolean ssc$isCatFriendly(Player player) {
        return FormPowerRegistry.has(player, ResourceLocation.fromNamespaceAndPath(
                "shape-shifter-curse", "cat_friendly"));
    }

    private static final class SscFleeGoal extends AvoidEntityGoal<Player> {
        private final TransformativeOcelotEntity ocelot;

        private SscFleeGoal(TransformativeOcelotEntity ocelot) {
            super(ocelot, Player.class, 16.0F, 0.8, 1.33,
                    player -> !(player instanceof Player p
                            && (ssc$isOriginalShifter(p) || ssc$isCatFriendly(p))));
            this.ocelot = ocelot;
        }

        @Override public boolean canUse() { return !this.ocelot.isTrusting() && super.canUse(); }
        @Override public boolean canContinueToUse() { return !this.ocelot.isTrusting() && super.canContinueToUse(); }
    }

    @Override public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) TransformativeMobEffects.tryApply(target, 0.5F, "ocelot_0");
        return hit;
    }
}
