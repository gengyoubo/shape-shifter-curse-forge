package net.onixary.shapeShifterCurseForge.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

/** An arrow carrying the appearance and tipped-arrow effects of a potion charm. */
public final class PotionCharmArrowEntity extends Arrow implements ItemSupplier {
    private static final EntityDataAccessor<ItemStack> CHARM = SynchedEntityData.defineId(
            PotionCharmArrowEntity.class, EntityDataSerializers.ITEM_STACK);

    public PotionCharmArrowEntity(EntityType<? extends PotionCharmArrowEntity> type, Level level) {
        super(type, level);
        pickup = AbstractArrow.Pickup.DISALLOWED;
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CHARM, new ItemStack(Items.PAPER));
    }

    public void setCharmStack(ItemStack stack) { entityData.set(CHARM, stack.copyWithCount(1)); }
    @Override public ItemStack getItem() { return entityData.get(CHARM); }

    @Override protected void onHitEntity(EntityHitResult hit) {
        if (!getItem().is(ModItems.HEALING_CHARM.get())) {
            super.onHitEntity(hit);
            return;
        }
        if (!level().isClientSide) {
            if (hit.getEntity() instanceof LivingEntity target) doPostHurtEffects(target);
            discard();
        }
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("CharmStack", getItem().save(new CompoundTag()));
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CharmStack")) setCharmStack(ItemStack.of(tag.getCompound("CharmStack")));
        pickup = AbstractArrow.Pickup.DISALLOWED;
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
