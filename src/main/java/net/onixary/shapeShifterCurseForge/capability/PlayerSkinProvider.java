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
public final class PlayerSkinProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final PlayerSkinData data = new PlayerSkinData();
    private LazyOptional<IPlayerSkinData> optional = LazyOptional.of(() -> data);

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        if (capability != ModCapabilities.PLAYER_SKIN) return LazyOptional.empty();
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
