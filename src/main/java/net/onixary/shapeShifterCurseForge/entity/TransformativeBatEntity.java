package net.onixary.shapeShifterCurseForge.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.form.FormManager;

public final class TransformativeBatEntity extends Bat {
    private int attackCooldown;
    public TransformativeBatEntity(EntityType<? extends Bat> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 0.5).add(Attributes.MOVEMENT_SPEED, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.attackCooldown > 0) this.attackCooldown--;
        if (this.level().isClientSide || this.attackCooldown > 0) return;
        LivingEntity target = this.getTarget();
        if (target instanceof Player player && this.distanceToSqr(player) <= 9.0) {
            this.doHurtTarget(player);
            this.attackCooldown = 100;
        }
    }

    @Override public boolean doHurtTarget(Entity target) {
        if (target instanceof Player player && !FormManager.current(player).id().equals(
                ResourceLocation.fromNamespaceAndPath("shape-shifter-curse", "original_shifter"))) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit) TransformativeMobEffects.tryApply(target, 0.5F, "bat_0");
        return hit;
    }
}
