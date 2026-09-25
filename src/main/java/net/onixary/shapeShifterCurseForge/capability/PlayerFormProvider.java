package net.onixary.shapeShifterCurseForge.capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

@SuppressWarnings("deprecation")
public final class PlayerFormProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final PlayerFormData data = new PlayerFormData();
    private LazyOptional<IPlayerFormData> optional = LazyOptional.of(() -> data);

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        if (capability != ModCapabilities.PLAYER_FORM) return LazyOptional.empty();
        // Forge invalidates the old player's optional before PlayerEvent.Clone.
        // reviveCaps() reopens the entity, so create a fresh optional for the copy.
        if (!optional.isPresent()) optional = LazyOptional.of(() -> data);
        return optional.cast();
    }

    public void invalidate() {
        optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        data.deserializeNBT(tag);
    }
}
