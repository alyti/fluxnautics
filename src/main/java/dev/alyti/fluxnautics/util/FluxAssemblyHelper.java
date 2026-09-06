package dev.alyti.fluxnautics.util;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager;
import foundry.veil.Veil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class FluxAssemblyHelper {

    public static void register() {
        if (!Veil.platform().isModLoaded("simulated"))
            SubLevelHeatMapManager.addSplitListener(FluxAssemblyHelper::addSplitBlocks);
    }

    private static void addSplitBlocks(final Level level, final BoundingBox3ic boundingBox3ic, final Collection<BlockPos> blocks) {
        fluxnautics$disassembleIntersectingContraptions(level, boundingBox3ic, blocks, false, null);
    }

    public static void disassembleSubLevel(@NotNull final Level level,
                                           @NotNull final SubLevel toDisassemble,
                                           @NotNull final BlockPos subLevelAnchor,
                                           @NotNull final BlockPos disassemblyGoal,
                                           @NotNull final Rotation rotation) {
        final BoundingBox3i plotBounds = new BoundingBox3i(toDisassemble.getPlot().getBoundingBox());
        final SubLevelAssemblyHelper.AssemblyTransform transform = new SubLevelAssemblyHelper.AssemblyTransform(subLevelAnchor, disassemblyGoal, rotation == Rotation.NONE ? 0 : (4 - rotation.ordinal()), rotation, (ServerLevel) level);

        final ObjectArrayList<BlockPos> blocks = new ObjectArrayList<>();
        final LevelPlot plot = toDisassemble.getPlot();
        for (final PlotChunkHolder chunk : plot.getLoadedChunks()) {
            final BoundingBox3ic localChunkBounds = chunk.getBoundingBox();
            if (localChunkBounds == null || localChunkBounds == BoundingBox3i.EMPTY) continue;

            for (int x = localChunkBounds.minX(); x <= localChunkBounds.maxX(); x++) {
                for (int y = localChunkBounds.minY(); y <= localChunkBounds.maxY(); y++) {
                    for (int z = localChunkBounds.minZ(); z <= localChunkBounds.maxZ(); z++) {
                        final BlockPos pos = new BlockPos(
                                x + chunk.getPos().getMinBlockX(),
                                y,
                                z + chunk.getPos().getMinBlockZ()
                        );
                        final BlockState state = level.getBlockState(pos);
                        if (!state.isAir()) {
                            blocks.add(pos);
                        }
                    }
                }
            }
        }

        fluxnautics$disassembleIntersectingContraptions(level, plot.getBoundingBox(), blocks, false, null);

        final PersistentEntitySectionManager<Entity> manager = ((ServerLevel) toDisassemble.getLevel()).entityManager;
        for (final PlotChunkHolder chunk : toDisassemble.getPlot().getLoadedChunks()) {
            final Stream<EntitySection<Entity>> sections = manager.sectionStorage.getExistingSectionsInChunk(chunk.getPos().toLong());

            for (final EntitySection<Entity> section : sections.toList()) {
                final List<Entity> entities = section.getEntities().toList();

                for (final Entity entity : entities) {
                    if (fluxnautics$handleGlueEntityDisassemble(level, entity, transform)) {
                        continue;
                    }

                    AABB box = entity.getBoundingBox();
                    box = new AABB(transform.apply(new Vec3(box.minX, box.minY, box.minZ)),
                            transform.apply(new Vec3(box.maxX, box.maxY, box.maxZ)));

                    final Vec3 newPos = transform.apply(entity.position());
                    entity.setPos(newPos);
                    entity.setYRot(entity.rotate(transform.getRotation()));
                    entity.yRotO = entity.getYRot();

                    if (entity instanceof final HangingEntity hangingEntity) {
                        hangingEntity.recalculateBoundingBox();
                    }

                    entity.levelCallback.onRemove(Entity.RemovalReason.CHANGED_DIMENSION);
                    ((ServerLevel) level).addDuringTeleport(entity);
                }
            }
        }

        if (!blocks.isEmpty()) {
            ((ServerLevelPlot) toDisassemble.getPlot()).kickAllEntities();
            SubLevelAssemblyHelper.moveBlocks((ServerLevel) level, transform, blocks);
        }

        SubLevelAssemblyHelper.moveTrackingPoints((ServerLevel) level, plotBounds, null, transform);
    }

    public static AssemblyResult assembleFromSingleBlock(final Level level, final BlockPos selfPos, final BlockPos toAssemble, final boolean includeStart, final boolean includeEncasingGlue) throws FluxAssemblyException {
        if (level.getBlockState(toAssemble).isAir()) {
            return null;
        }

        final ObjectArrayList<BlockPos> blocks = new ObjectArrayList<>();
        final ObjectArrayList<Object> superGlues = new ObjectArrayList<>();
        final ObjectArrayList<Object> honeyGlues = new ObjectArrayList<>();

        fluxnautics$searchStructure(level, selfPos, toAssemble, includeStart, includeEncasingGlue, blocks, superGlues, honeyGlues);

        if (blocks.isEmpty()) {
            return null;
        }

        final BoundingBox3i bounds = BoundingBox3i.from(blocks);
        final ObjectArrayList<Object> collectedContraptionGlues = new ObjectArrayList<>();

        fluxnautics$disassembleIntersectingContraptions(level, bounds, blocks, true, collectedContraptionGlues);

        final BlockPos anchor = blocks.stream().findFirst().get();
        final SubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks((ServerLevel) level, anchor, blocks, bounds);

        if (subLevel != null) {
            final BlockPos offsetBlocks = subLevel.getPlot().getCenterBlock().subtract(anchor);

            fluxnautics$moveGlues(level, offsetBlocks, collectedContraptionGlues, superGlues, honeyGlues);
            return new AssemblyResult(subLevel, offsetBlocks);
        }

        return null;
    }

    /**
     * CALLSITE: Create mixin injects here to disassemble existing Create contraptions
     * that intersect with the assembly bounds and add their blocks.
     */
    private static void fluxnautics$disassembleIntersectingContraptions(final Level level, final BoundingBox3ic assemblyBounds, final Collection<BlockPos> blocks, final boolean passGluesBack, final Collection<Object> collectedGlues) {
        // Base implementation does nothing.
    }

    /**
     * CALLSITE: Create & Simulated mixins inject here to search for blocks and glue entities.
     * Base implementation just adds the single block being assembled.
     */
    private static void fluxnautics$searchStructure(final Level level, final BlockPos selfPos, final BlockPos toAssemble, final boolean includeStart, final boolean includeEncasingGlue, final Collection<BlockPos> blocks, final Collection<Object> superGlues, final Collection<Object> honeyGlues) {
        if (!includeStart && selfPos != null && toAssemble.equals(selfPos)) {
            return;
        }
        blocks.add(toAssemble);
    }

    /**
     * CALLSITE: Create & Simulated mixins inject here to move glues into the sub-level.
     */
    private static void fluxnautics$moveGlues(final Level level, final BlockPos offset, final Collection<Object> contraptionGlues, final Collection<Object> superGlues, final Collection<Object> honeyGlues) {
        // Base implementation does nothing.
    }

    /**
     * CALLSITE: Create & Simulated mixins inject here to handle glue entities during disassembly.
     * @return true if the entity was a glue entity and was handled.
     */
    private static boolean fluxnautics$handleGlueEntityDisassemble(final Level level, final Entity entity, final SubLevelAssemblyHelper.AssemblyTransform transform) {
        return false;
    }

    public static Rotation rotationFrom90DegRots(final int rots) {
        return switch (Math.floorMod(rots, 4)) {
            case 0 -> Rotation.NONE;
            case 1 -> Rotation.COUNTERCLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.CLOCKWISE_90;
            default -> throw new AssertionError();
        };
    }

    public record AssemblyResult(SubLevel subLevel, BlockPos offset) { }
}