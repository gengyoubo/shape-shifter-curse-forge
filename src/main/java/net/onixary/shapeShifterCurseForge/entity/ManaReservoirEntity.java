package net.onixary.shapeShifterCurseForge.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;
import java.util.UUID;

/** Stationary minion that shares five mana per second for five seconds. */
public final class ManaReservoirEntity extends Mob implements ItemSupplier {
    public static final String OWNER_RESERVOIR = "SscManaReservoir";
    public static final String OWNER_UNTIL = "SscManaReservoirUntil";
    private UUID owner;
    private long expiresAt;
    public ManaReservoirEntity(EntityType<? extends ManaReservoirEntity> type, Level level) { super(type, level); setNoGravity(true); }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10).add(Attributes.MOVEMENT_SPEED, 0);
    }
    public void setOwner(UUID owner) { this.owner = owner; this.expiresAt = level().getGameTime() + 100; }
    public UUID ownerId() { return owner; }
    @Override public ItemStack getItem() { return new ItemStack(Items.AMETHYST_SHARD); }
    @Override public void tick() {
        setNoGravity(true); super.tick();
        if (!(level() instanceof ServerLevel level) || !isAlive()) return;
        if (expiresAt > 0 && level.getGameTime() > expiresAt) { discard(); return; }
        if (tickCount % 20 != 0) return;
        for (ServerPlayer player : level.players()) {
            String form = FormManager.current(player).id().toString();
            if (player.isAlive() && !player.isSpectator() && distanceToSqr(player) <= 16
                    && (form.equals("shape-shifter-curse:familiar_fox_2") || form.equals("shape-shifter-curse:familiar_fox_3")))
                FormActivePowerService.gainMana(player, 5);
        }
        if (getHealth() <= 2) discard(); else setHealth(getHealth() - 2);
    }
    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason != RemovalReason.UNLOADED_TO_CHUNK && owner != null
                && level().getServer() != null) {
            var player = level().getServer().getPlayerList().getPlayer(owner);
            if (player != null && player.getPersistentData().hasUUID(OWNER_RESERVOIR)
                    && player.getPersistentData().getUUID(OWNER_RESERVOIR).equals(getUUID()))
                { player.getPersistentData().remove(OWNER_RESERVOIR); player.getPersistentData().remove(OWNER_UNTIL); }
        }
        super.remove(reason);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); if (owner != null) tag.putUUID("Owner", owner); tag.putLong("ExpiresAt", expiresAt); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null; expiresAt = tag.getLong("ExpiresAt"); }
}
