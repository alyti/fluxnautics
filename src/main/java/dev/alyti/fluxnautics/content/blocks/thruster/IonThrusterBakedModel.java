package dev.alyti.fluxnautics.content.blocks.thruster;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.registry.FNModelCache;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.client.model.baked.ExtensionBakedModel;
import mekanism.client.render.lib.Quad;
import mekanism.client.render.lib.QuadTransformation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NothingNullByDefault
public class IonThrusterBakedModel extends ExtensionBakedModel<Void> {
    public enum ThrusterPart {
        SOLID(Fluxnautics.id("block/thruster_array/ion/solid")),
        EDGE(Fluxnautics.id("block/thruster_array/ion/edge")),
        PLUME(Fluxnautics.id("block/thruster_array/ion/plume")),
        PLUME_BORDER_EDGE(Fluxnautics.id("block/thruster_array/ion/plume_border_edge")),
        PLUME_BORDER_CORNER_CLOSED(Fluxnautics.id("block/thruster_array/ion/plume_border_corner_closed")),
        PLUME_BORDER_CORNER_SIDE_A(Fluxnautics.id("block/thruster_array/ion/plume_border_corner_side_a")),
        PLUME_BORDER_CORNER_SIDE_B(Fluxnautics.id("block/thruster_array/ion/plume_border_corner_side_b")),
        PLUME_BORDER_CORNER_INNER(Fluxnautics.id("block/thruster_array/ion/plume_border_corner_inner")),
        PLUME_SOLID_EDGE(Fluxnautics.id("block/thruster_array/ion/plume_solid_edge")),
        PLUME_SOLID_CORNER(Fluxnautics.id("block/thruster_array/ion/plume_solid_corner"));

        private final ResourceLocation model;

        ThrusterPart(ResourceLocation model) {
            this.model = model;
        }

        public ResourceLocation getModel() {
            return model;
        }
    }

    public static final float Z_OFFSET = 0.0001f;
    public static final float SIDE_OFFSET = 0f;//1f - 13f / 16f;
    public static final ThrusterPart[] PARTIAL_MODELS = ThrusterPart.values();

    public IonThrusterBakedModel(BakedModel original) {
        super(original);
    }

    @Override
    public List<BakedQuad> createQuads(QuadsKey<Void> key) {
        BlockState blockState = Objects.requireNonNull(key.getBlockState());
        RenderType renderType = key.getLayer();
        Direction facing = blockState.getValue(BlockStateProperties.FACING);
        QuadTransformation baseRotation = QuadTransformation.rotate(facing);
        Direction side = key.getSide();
        List<BakedQuad> newQuads = new ArrayList<>();

        assert side != null;
        assert renderType != null;

        if (blockState.getValue(IonThrusterBlock.FRONT)) {
            bakeQuad(key, blockState, side, renderType, newQuads, ThrusterPart.SOLID.ordinal(), baseRotation);
        } else {
            boolean up = blockState.getValue(IonThrusterBlock.UP);
            boolean down = blockState.getValue(IonThrusterBlock.DOWN);
            boolean left = blockState.getValue(IonThrusterBlock.LEFT);
            boolean right = blockState.getValue(IonThrusterBlock.RIGHT);
            boolean up_left = blockState.getValue(IonThrusterBlock.UP_LEFT);
            boolean up_right = blockState.getValue(IonThrusterBlock.UP_RIGHT);
            boolean down_left = blockState.getValue(IonThrusterBlock.DOWN_LEFT);
            boolean down_right = blockState.getValue(IonThrusterBlock.DOWN_RIGHT);


            handleEdge(key, blockState, side, renderType, newQuads, baseRotation, facing, right, 0);
            handleEdge(key, blockState, side, renderType, newQuads, baseRotation, facing, up, 90);
            handleEdge(key, blockState, side, renderType, newQuads, baseRotation, facing, left, 180);
            handleEdge(key, blockState, side, renderType, newQuads, baseRotation, facing, down, 270);

            handleCorner(key, blockState, side, renderType, newQuads, baseRotation, facing,
                right, down, down_right, 0);    // BOTTOM_RIGHT
            handleCorner(key, blockState, side, renderType, newQuads, baseRotation, facing,
                right, up, up_right, 90);       // TOP_RIGHT
            handleCorner(key, blockState, side, renderType, newQuads, baseRotation, facing,
                left, up, up_left, 180);       // TOP_LEFT
            handleCorner(key, blockState, side, renderType, newQuads, baseRotation, facing,
                left, down, down_left, 270);  // BOTTOM_LEFT

            bakeQuad(key, blockState, side, renderType, newQuads, ThrusterPart.PLUME.ordinal(), baseRotation);
        }

        if (!newQuads.isEmpty()) {
            List<BakedQuad> ret = new ArrayList<>(key.getQuads());
            ret.addAll(newQuads);
            return ret;
        }
        return key.getQuads();
    }

    private static void handleEdge(
        QuadsKey<Void> key,
        BlockState blockState,
        Direction side,
        RenderType renderType,
        List<BakedQuad> newQuads,
        QuadTransformation baseRotation,
        Direction facing,
        boolean edgeExists,
        int edgeRotation
    ) {
        QuadTransformation offset = QuadTransformation.translate(Z_OFFSET, Z_OFFSET, Z_OFFSET);
        QuadTransformation rotation = planeAlignedRotate(facing, edgeRotation);

        if (!edgeExists) {
            bakeQuad(key, blockState, side, renderType, newQuads, ThrusterPart.EDGE.ordinal(), baseRotation, rotation, offset);
            bakeQuad(key, blockState, side, renderType, newQuads, ThrusterPart.PLUME_BORDER_EDGE.ordinal(), baseRotation, rotation);
        } else {
            bakeQuad(key, blockState, side, renderType, newQuads, ThrusterPart.PLUME_SOLID_EDGE.ordinal(), baseRotation, rotation);
        }
    }

    private static void handleCorner(
        QuadsKey<Void> key,
        BlockState blockState,
        Direction side,
        RenderType renderType,
        List<BakedQuad> newQuads,
        QuadTransformation baseRotation,
        Direction facing,
        boolean edge1,
        boolean edge2,
        boolean diagonal,
        int cursedRotation
    ) {
        ThrusterPart part;

        if (edge1 && edge2 && diagonal) {
            part = ThrusterPart.PLUME_SOLID_CORNER;
        } else if (!edge1 && !edge2) {
            part = ThrusterPart.PLUME_BORDER_CORNER_CLOSED;
        } else if (!diagonal && edge1 && edge2) {
            part = ThrusterPart.PLUME_BORDER_CORNER_INNER;
        } else {
            boolean useSideA = (edge2 == (cursedRotation % 180 == 0));
            part = ((facing == Direction.DOWN) != useSideA) ? ThrusterPart.PLUME_BORDER_CORNER_SIDE_A : ThrusterPart.PLUME_BORDER_CORNER_SIDE_B;
        }

        QuadTransformation rotation = planeAlignedRotateCursed(facing, cursedRotation);
        bakeQuad(key, blockState, side, renderType, newQuads, part.ordinal(), baseRotation, rotation);
    }

    private static QuadTransformation planeAlignedRotate(Direction facing, double v) {
        double axis = switch (facing.getAxisDirection()) {
            case POSITIVE -> 1.0;
            case NEGATIVE -> -1.0;
        };
        return switch (facing.getAxis()) {
            case X -> QuadTransformation.rotate(v * axis, 0, 0);
            case Y -> QuadTransformation.rotate(0, (180 * axis) + v, 0);
            case Z -> QuadTransformation.rotate(0, 0, v * axis);
        };
    }

    private static QuadTransformation planeAlignedRotateCursed(Direction facing, double v) {
        double axis = switch (facing.getAxisDirection()) {
            case POSITIVE -> 1.0;
            case NEGATIVE -> -1.0;
        };
        return switch (facing.getAxis()) {
            case X -> QuadTransformation.rotate(v * axis, 0, 0);
            case Y -> QuadTransformation.rotate(0, (180 * axis) + v + (axis == -1.0 ? -90 : 0), 0);
            case Z -> QuadTransformation.rotate(0, 0, v * axis);
        };
    }

    private static void bakeQuad(QuadsKey<Void> key, BlockState blockState, Direction side, RenderType renderType, List<BakedQuad> quads, int partIndex, QuadTransformation... transformations) {
        List<BakedQuad> partModel = FNModelCache.INSTANCE.THRUSTER_PARTS[partIndex].getQuads(blockState, side, key.getRandom(), ModelData.EMPTY, renderType);
        for (BakedQuad bakedQuad : partModel) {
            Quad quad = new Quad(bakedQuad);
            if (quad.transform(transformations)) {
                quads.add(quad.bake());
            } else {
                quads.add(bakedQuad);
            }
        }
    }

    @Nullable
    @Override
    public QuadsKey<Void> createKey(QuadsKey<Void> key, ModelData data) {
        if (key.getBlockState() != null && key.getSide() == null) {
            return key;
        }
        return null;
    }

    @Override
    protected IonThrusterBakedModel wrapModel(BakedModel model) {
        return new IonThrusterBakedModel(model);
    }
}