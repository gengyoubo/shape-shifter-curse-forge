package net.onixary.shapeShifterCurseForge.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.onixary.shapeShifterCurseForge.cursedmoon.CursedMoonService;
import net.onixary.shapeShifterCurseForge.registry.ModBlockEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** World-side portion of Fabric's FormAttunerBlockEntity. */
@SuppressWarnings("deprecation")
public final class FormAttunerBlockEntity extends BlockEntity {
    /** Fabric defaults this to four and lets the Perk registry raise it. */
    private static int maxLevel = 4;
    private static final Map<UUID, BlockPos> LAST_USED_POSITIONS = new ConcurrentHashMap<>();

    private int level;
    private List<BeamSegment> beamSegments = List.of();

    public FormAttunerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORM_ATTUNER.get(), pos, state);
    }

    public static void allocateMaxLevel(int requestedMaxLevel) {
        maxLevel = Math.max(maxLevel, requestedMaxLevel);
    }

    public static int getMaxLevel() {
        return maxLevel;
    }

    public static void rememberUser(ServerPlayer player, BlockPos pos) {
        LAST_USED_POSITIONS.put(player.getUUID(), pos.immutable());
    }

    @Nullable
    public static FormAttunerBlockEntity getLastUsed(ServerPlayer player) {
        BlockPos pos = LAST_USED_POSITIONS.get(player.getUUID());
        if (pos == null || !player.level().hasChunkAt(pos)) {
            return null;
        }
        return player.level().getBlockEntity(pos) instanceof FormAttunerBlockEntity attuner ? attuner : null;
    }

    public int getAttunementLevel() {
        return level;
    }

    public List<BeamSegment> getBeamSegments() {
        // Fabric hides the beam while the attuner is inactive (no valid base / not cursed moon).
        return level == 0 ? List.of() : beamSegments;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FormAttunerBlockEntity attuner) {
        // Fabric scans on both sides so its beam renderer observes glass changes
        // immediately.  Only the authoritative base-level calculation is server-side.
        attuner.beamSegments = scanBeam(level, pos);
        if (!level.isClientSide && level.getGameTime() % 80L == 0L) {
            int updated = attuner.beamSegments.isEmpty() ? 0 : calculateLevel(level, pos);
            if (updated != attuner.level) {
                attuner.level = updated;
                attuner.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
    }

    private static List<BeamSegment> scanBeam(Level level, BlockPos origin) {
        List<BeamSegment> segments = new ArrayList<>();
        // The Attuner implements BeaconBeamBlock on Fabric and emits the first
        // purple beam segment itself.  Starting with it is essential: clear
        // sky is a valid beam path, not an empty/invalid one.
        BeamSegment current = new BeamSegment(DyeColor.PURPLE.getTextureDiffuseColors().clone(), 1);
        segments.add(current);
        for (int y = origin.getY() + 1; y < level.getMaxBuildHeight(); y++) {
            BlockPos scanPos = new BlockPos(origin.getX(), y, origin.getZ());
            BlockState scanState = level.getBlockState(scanPos);
            if (scanState.getBlock() instanceof BeaconBeamBlock beamBlock) {
                float[] color = beamBlock.getColor().getTextureDiffuseColors().clone();
                if (Arrays.equals(current.color(), color)) {
                    current = current.increaseHeight();
                    segments.set(segments.size() - 1, current);
                } else {
                    float[] mixed = new float[] {
                            (current.color()[0] + color[0]) / 2.0F,
                            (current.color()[1] + color[1]) / 2.0F,
                            (current.color()[2] + color[2]) / 2.0F
                    };
                    current = new BeamSegment(mixed, 1);
                    segments.add(current);
                }
                continue;
            }
            if (scanState.getLightBlock(level, scanPos) >= 15 && !scanState.is(Blocks.BEDROCK)) {
                return List.of();
            }
            current = current.increaseHeight();
            segments.set(segments.size() - 1, current);
        }
        return Collections.unmodifiableList(segments);
    }

    private static int calculateLevel(Level level, BlockPos origin) {
        if (level.dimension() != Level.OVERWORLD || !CursedMoonService.isInCursedMoon(level)) {
            return 0;
        }
        int result = 0;
        for (int candidate = 1; candidate <= maxLevel; candidate++) {
            int y = origin.getY() - candidate;
            if (y < level.getMinBuildHeight()) {
                break;
            }
            for (int x = origin.getX() - candidate; x <= origin.getX() + candidate; x++) {
                for (int z = origin.getZ() - candidate; z <= origin.getZ() + candidate; z++) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).is(BlockTags.BEACON_BASE_BLOCKS)) {
                        return result;
                    }
                }
            }
            result = candidate;
        }
        return result;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Level", level);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        level = tag.getInt("Level");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public record BeamSegment(float[] color, int height) {
        private BeamSegment increaseHeight() {
            return new BeamSegment(color, height + 1);
        }
    }
}
