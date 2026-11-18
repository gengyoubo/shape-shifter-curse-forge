package net.onixary.shapeShifterCurseForge.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Model element bounds; angled decorations use enclosing axis-aligned boxes. */
final class WorkstationShapes {
    static final Map<Direction, VoxelShape> ALTAR = horizontalShapes(Shapes.or(
            Block.box(2.5, 0, 2.5, 13.5, 2, 13.5),
            Block.box(2, 7.5, 2, 14, 9.5, 14),
            Block.box(2, 2, 2, 14, 4, 14),
            Block.box(3, 4, 3, 13, 5, 13),
            Block.box(12.9464, 10.318, 4, 17.1891, 14.5607, 8),
            Block.box(12.9142, 11.5251, -1.0607, 14.9142, 14.3536, 1.7678),
            Block.box(13.5, 12, 10.8, 16.5, 14, 15.8),
            Block.box(0, 9.5, 0, 16, 13.5, 16),
            Block.box(-0.2, 6.7, -0.2, 13.8, 13.7, 15.8)));

    static final VoxelShape FORM_ATTUNER = Shapes.or(
            Block.box(3.5, 0, 3.5, 12.5, 2, 12.5),
            Block.box(2.3431, 5.1, 2.3431, 13.6569, 7.1, 13.6569),
            Block.box(3, 1, 3, 13, 3, 13),
            Block.box(4, 3.8, 4, 12, 4.8, 12),
            Block.box(5.5, 11.2, 5.5, 10.5, 13.2, 10.5),
            Block.box(0.7464, 8.618, 3.4, 4.9891, 12.8607, 7.4),
            Block.box(9.9142, 9.2251, 2.6393, 11.9142, 12.0536, 5.4678),
            Block.box(2.4163, 9.2244, 10.5, 5.0294, 11.8375, 12.5),
            Block.box(8.918, 10.5, 9.718, 14.5749, 12.6, 15.3749),
            Block.box(2, 8, 2, 14, 11, 14),
            Block.box(1.5, 0, 1.5, 14.5, 8, 14.5));

    private static Map<Direction, VoxelShape> horizontalShapes(VoxelShape north) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        VoxelShape current = north;
        for (Direction facing : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            shapes.put(facing, current);
            VoxelShape[] rotated = {Shapes.empty()};
            current.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    rotated[0] = Shapes.or(rotated[0], Shapes.box(
                            1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            current = rotated[0];
        }
        return Map.copyOf(shapes);
    }

    private WorkstationShapes() {}
}
