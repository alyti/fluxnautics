package dev.alyti.fluxnautics.index;

import mekanism.common.util.EnumUtils;
import mekanism.common.util.VoxelShapeUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockShapes {

    private BlockShapes() {
    }

    private static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return Block.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static final VoxelShape[] THRUSTER = new VoxelShape[EnumUtils.DIRECTIONS.length];

    public static final VoxelShape[] FIELD_DRIVE = new VoxelShape[EnumUtils.HORIZONTAL_DIRECTIONS.length];

    public static final VoxelShape[] HOLO_TABLE = new VoxelShape[EnumUtils.HORIZONTAL_DIRECTIONS.length];

    public static final VoxelShape[] HOLO_GRAM = new VoxelShape[EnumUtils.DIRECTIONS.length];

    public static final VoxelShape[] SPATIAL_ANCHOR = new VoxelShape[EnumUtils.DIRECTIONS.length];

    public static final VoxelShape[] CONDUIT_CONNECTOR_COLLIDER = new VoxelShape[EnumUtils.DIRECTIONS.length];

    static {
        VoxelShapeUtils.setShape(VoxelShapeUtils.combine(
                box(0, 0, 0, 16, 3, 16),
                box(1, 3, 1, 15, 4, 15)
        ), THRUSTER, true);

        VoxelShapeUtils.setShape(VoxelShapeUtils.combine(
                box(1, 0, 1, 15, 1, 15),
                box(2, 2, 2, 14, 30, 14),
                box(1, 31, 1, 15, 32, 15)
        ), FIELD_DRIVE);

        VoxelShapeUtils.setShape(VoxelShapeUtils.combine(
            box(-16, 0, -16, 32, 16, 32)
        ), HOLO_TABLE);

        // 1 pixel = 1.0/16.0
        double w = 1.0 / 16.0;

        // Define the inner cavity to subtract.
        // We inset the X and Z by 1 pixel (w to 1-w).
        // We inset the Top by 1 pixel (up to 1-w), but leave the Bottom open (down to 0).
        VoxelShape innerCavity = Shapes.box(w, 0, w, 1 - w, 1 - w, 1 - w);

        // Use batchCombine with ONLY_FIRST to subtract the innerCavity from the outer block
        VoxelShape hollowBox = VoxelShapeUtils.batchCombine(Shapes.block(), BooleanOp.ONLY_FIRST, true, innerCavity);

//        // Create the 5 walls (Top, North, South, West, East). Bottom is left open.
//        VoxelShape topWall  = Shapes.box(0, 1 - w, 0, 1, 1, 1);
//        VoxelShape northWall = Shapes.box(0, 0, 0, 1, 1, w);
//        VoxelShape southWall = Shapes.box(0, 0, 1 - w, 1, 1, 1);
//        VoxelShape westWall  = Shapes.box(0, 0, 0, w, 1, 1);
//        VoxelShape eastWall  = Shapes.box(1 - w, 0, 0, 1, 1, 1);
//
//        // Combine them into one hollow box shape
//        VoxelShape hollowBox = VoxelShapeUtils.combine(topWall, northWall, southWall, westWall, eastWall);

        // Rotate the shape to cover all 6 directions and store them in the SHAPES array
        VoxelShapeUtils.setShape(hollowBox, CONDUIT_CONNECTOR_COLLIDER, true);

//        VoxelShapeUtils.setShape(VoxelShapeUtils.combine(
////            box(1, 0, 1, 15, 1, 15)
//            box(0, 0, 0, 1, 16, 16),
//            box(0, 0, 1, 15, 1, 15),
//            box(1, 0, 1, 15, 1, 15),
//            box(1, 0, 1, 15, 1, 15),
//            box(1, 0, 1, 15, 1, 15)
//        ), CONDUIT_CONNECTOR_COLLIDER, true);

        VoxelShapeUtils.setShape(VoxelShapeUtils.combine(
                box(-12, 10, -12, 28, 16, 28),
                box(-16, 0, -12, -12, 16, 28),  // Left strip
                box(28, 0, -12, 32, 16, 28),    // Right strip
                box(-12, 0, -16, 28, 16, -12),  // Back strip
                box(-12, 0, 28, 28, 16, 32),    // Front strip
                box(-16, 0, -16, -12, 16, -12), // Back-left corner
                box(28, 0, -16, 32, 16, -12),   // Back-right corner
                box(-16, 0, 28, -12, 16, 32),   // Front-left corner
                box(28, 0, 28, 32, 16, 32)      // Front-right corner
        ), HOLO_GRAM, true);

//        VoxelShapeUtils.setShape(VoxelShapeUtils.combine(
//                box(-12, 10, -12, 28, 16, 28)
//        ), HOLO_GRAM, true);

        VoxelShapeUtils.setShape(VoxelShapeUtils.combine(
                box(0, 0, 0, 3, 3, 16),
                box(0, 3, 0, 3, 16, 3),
                box(0, 3, 13, 3, 16, 16),
                box(0, 13, 3, 3, 16, 13),
                box(3, 0, 0, 16, 3, 3),
                box(3, 0, 13, 16, 3, 16),
                box(3, 13, 0, 16, 16, 3),
                box(3, 13, 13, 16, 16, 16),
                box(13, 0, 3, 16, 3, 13),
                box(13, 3, 0, 16, 13, 3),
                box(13, 3, 13, 16, 13, 16),
                box(13, 13, 3, 16, 16, 13),
                box(12.5, 15, 7.5, 13.5, 15.9, 8.5),//ledTop1
                box(2.5, 15, 7.5, 3.5, 15.9, 8.5),//ledTop2
                box(12.5, 7.5, 0.1, 13.5, 8.5, 1),//ledBack1
                box(2.5, 7.5, 0.1, 3.5, 8.5, 1),//ledBack2
                box(2.5, 0.1, 7.5, 3.5, 1, 8.5),//ledBottom2
                box(12.5, 0.1, 7.5, 13.5, 1, 8.5),//ledBottom1
                box(12.5, 7.5, 15, 13.5, 8.5, 15.9),//ledFront1
                box(2.5, 7.5, 15, 3.5, 8.5, 15.9),//ledFront2
                box(0.1, 7.5, 2.5, 1, 8.5, 3.5),//ledRight2
                box(0.1, 7.5, 12.5, 1, 8.5, 13.5),//ledRight1
                box(15, 7.5, 2.5, 15.9, 8.5, 3.5),//ledLeft1
                box(15, 7.5, 12.5, 15.9, 8.5, 13.5)//ledLeft2
        ), SPATIAL_ANCHOR, true);
    }
}