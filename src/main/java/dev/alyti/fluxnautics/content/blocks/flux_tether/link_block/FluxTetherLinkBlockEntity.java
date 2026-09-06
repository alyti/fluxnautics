package dev.alyti.fluxnautics.content.blocks.flux_tether.link_block;

import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherBlock;
import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherBlockEntity;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.List;
import java.util.UUID;

public class FluxTetherLinkBlockEntity extends TileEntityMekanism implements BlockEntitySubLevelActor {

    @Nullable
    public BlockPos parent;
    @Nullable
    public UUID parentSubLevelId;
    public boolean assembling;

    public FluxTetherLinkBlockEntity(BlockPos pos, BlockState state) {
        super(FNBlocks.FLUX_TETHER_LINK, pos, state);
    }

    public Vector3d getCenter() {
        final BlockState state = this.getBlockState();

        final Direction facing = state.getValue(BlockStateProperties.FACING);
        final Vec3i facingVec = facing.getNormal();

        double scale = 0.75;
        return JOMLConversion.atCenterOf(this.worldPosition)
            .sub(facingVec.getX() * scale,
                facingVec.getY() * scale,
                facingVec.getZ() * scale);
    }

    public void beforeAssembly() { this.assembling = true; }

    public void notifyRemovedOutsideAssembly() {
        if (!this.level.isClientSide && !this.assembling)
            this.destroyBearing();
    }

    @Override
    public void setRemoved() {
//        if (!this.level.isClientSide && !this.assembling)
//            this.destroyBearing();
        super.setRemoved();
    }

    private void destroyBearing() {
        if (this.parent != null
                && this.getLevel().getBlockState(this.parent).getBlock() instanceof FluxTetherBlock) {
            this.getLevel().destroyBlock(this.parent, false);
        }
    }

    public void setParent(FluxTetherBlockEntity be) {
        final SubLevel subLevel = Sable.HELPER.getContaining(be);
        this.parent           = be.getBlockPos();
        this.parentSubLevelId = subLevel != null ? subLevel.getUniqueId() : null;
        setChanged();
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        if (this.parent != null) {
            final BlockEntity parentBE = this.level.getBlockEntity(this.parent);

            if (parentBE instanceof final FluxTetherBlockEntity fluxTetherBlockEntity && !parentBE.isRemoved()) {
                if (Sable.HELPER.getContaining(parentBE) == null) {
                    fluxTetherBlockEntity.updateServoCoefficients(timeStep);
                }
            }
        }
    }

    @Override
    public @Nullable Iterable<@NotNull SubLevel> sable$getConnectionDependencies() {
        if (this.parent == null)
            return null;
        final SubLevelContainer container = SubLevelContainer.getContainer(this.level);
        if (this.parentSubLevelId != null) {
            final SubLevel subLevel = container.getSubLevel(this.parentSubLevelId);
            if (subLevel != null)
                return List.of(subLevel);
        }
        return null;
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (this.parent != null)
            tag.put("ParentPos", NbtUtils.writeBlockPos(this.parent));
        if (this.parentSubLevelId != null)
            tag.putUUID("ParentSubLevelId", this.parentSubLevelId);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("ParentPos"))
            this.parent = NbtUtils.readBlockPos(tag, "ParentPos").orElse(null);
        if (tag.hasUUID("ParentSubLevelId"))
            this.parentSubLevelId = tag.getUUID("ParentSubLevelId");
    }



    public void fixParentLinkingWhenMoved() {
        if (this.level.isClientSide() || this.parent == null) {
            return;
        }

        final BlockEntity be = this.level.getBlockEntity(this.parent);

        if (be instanceof final FluxTetherBlockEntity sbe) {
            sbe.localPlatePos = this.getBlockPos();

            final ServerSubLevel newSublevel = (ServerSubLevel)Sable.HELPER.getContaining(this);
            if (newSublevel != null) {
                final UUID subLevelID = sbe.getSubLevelID();
                final UUID newID = newSublevel.getUniqueId();

                if (!newID.equals(subLevelID)) {
                    sbe.setSubLevelID(newSublevel.getUniqueId());
                    sbe.reattachConstraint(newSublevel, true);
                }
            } else {
                sbe.setSubLevelID(null);
                sbe.reattachConstraint(null, true);
            }
        }
    }
}