package net.onixary.shapeShifterCurseForge.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

public final class TransformativeAxolotlEntity extends Axolotl {
    public TransformativeAxolotlEntity(EntityType<? extends Axolotl> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0).add(Attributes.MOVEMENT_SPEED, 1.0);
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(ModItems.TRANSFORMATIVE_AXOLOTL_BUCKET.get());
    }

    @Override public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) TransformativeMobEffects.tryApply(target, 0.7F, "axolotl_0");
        return hit;
    }
}
