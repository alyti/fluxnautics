package dev.alyti.fluxnautics.content.blocks.conduit;

import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.server.ConduitAttachment;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.server.ConduitAttachmentPoint;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.server.ServerConduitStrand;
import dev.alyti.fluxnautics.math.LerpedFloat;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.alyti.fluxnautics.registry.FNItems;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class ConduitConnectorBlockEntity extends AbstractConduitHolderBlockEntity {

    private double motorSpeed = 0;
    private int stretchTimer = 0;
    private boolean stretched;
    private boolean stretchedLastTick;

    protected LerpedFloat clientAngle = LerpedFloat.linear();
    
    @Override
    public Vec3 getAttachmentPoint(final BlockPos pos, final BlockState state) {
        return pos.getCenter();
    }

    public ConduitConnectorBlockEntity(BlockPos pos, BlockState state) {
        super(FNBlocks.CONDUIT_CONNECTOR, pos, state);
    }

    @Nullable
    public ConduitConnectorBlockEntity getRemoteConnector() {
        TileComponentConduitStrandHolder<?> holder = getStrandHolder();
        ServerConduitStrand strand = holder.ownsConduit() ? holder.getOwnedStrand() : holder.getAttachedStrand();
        if (strand == null) return null;

        ConduitAttachmentPoint remotePoint = holder.ownsConduit() ? ConduitAttachmentPoint.END : ConduitAttachmentPoint.START;
        ConduitAttachment remoteAttachment = strand.getAttachment(remotePoint);
        if (remoteAttachment == null) return null;

        BlockPos remotePos = remoteAttachment.blockAttachment();
        Level level = getLevel();

        if (level == null || !level.isLoaded(remotePos)) {
            return null;
        }

        BlockEntity be = level.getBlockEntity(remotePos);
        if (be instanceof ConduitConnectorBlockEntity remoteBe) {
            return remoteBe;
        }
        return null;
    }

    public void onUpdateClient() {
//        this.invalidateRenderBoundingBox();
        this.clientAngle.setValue(this.clientAngle.getValue() + this.getMotorSpeed());
    }

    @Override
    public boolean onUpdateServer() {
        boolean update = super.onUpdateServer();

        final ServerConduitStrand strand = this.getStrandHolder().getOwnedStrand();
        final boolean hasConduit = strand != null;

        if (hasConduit && this.getStrandHolder().ownsConduit()) {
            this.updateConduitStrandExtension(strand);
        }

        if (this.stretchTimer > 0) {
            this.stretchTimer--;
        }

        return update;
    }

    private void updateConduitStrandExtension(final ServerConduitStrand strand) {
        double movementSpeed = this.getMotorSpeed();

        final double desiredExtension = strand.getExtension() + (strand.getPoints().size() - 2) * ServerConduitStrand.SEGMENT_LENGTH;
        final double currentExtension = strand.getCurrentExtension();

        this.stretched = currentExtension > desiredExtension * (1.0 + (FluxnauticsConfig.common.conduitsMaxStretch.get() / 100.0));
        if (this.stretched) {
            if (!this.stretchedLastTick) {
//                this.effects.triggerOverStressedEffect();
            }

            if (this.stretchTimer == 0) {
                this.stretchTimer = this.level.random.nextIntBetweenInclusive(5 * 20, 15 * 20);
//                this.level.playSound(null, this.getBlockPos(), SoundEvent., SoundSource.BLOCKS, 0.1f, 0.8f + this.level.random.nextFloat() * 0.2f);
            }

            movementSpeed = Math.max(0.0f, movementSpeed);
        }
        this.stretchedLastTick = this.stretched;

        if (currentExtension > FluxnauticsConfig.common.conduitsMaxLength.get()) {
            movementSpeed = Math.min(0.0f, movementSpeed);
        }

        // extension of the source-most segment
        double extension = strand.getExtension();
        extension += movementSpeed;

        final int minPointCount = 2;

        if (extension < 1.0 && strand.getPoints().size() == minPointCount) {
            extension = 1.0;
        } else {
            while (extension < 0.0) {
                strand.removeFirstPoint();
                extension += ServerConduitStrand.SEGMENT_LENGTH;

                if (extension < 1.0 && strand.getPoints().size() == minPointCount) {
                    extension = 1.0;
                    break;
                }
            }

            while (extension > ServerConduitStrand.SEGMENT_LENGTH) {
                final Vector3d point = JOMLConversion.toJOML(Sable.HELPER.projectOutOfSubLevel(this.level, this.getStrandHolder().getAttachmentPoint()));
                strand.addPoint(point);
                extension -= 1.0;
            }

            if (extension < 1.0 && strand.getPoints().size() <= minPointCount) {
                extension = 1.0;
            }
        }

        strand.updateFirstSegmentExtension(extension);
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableDouble.create(this::getMotorSpeed, value -> motorSpeed = value));
    }

    public double getMotorSpeed() {
        return motorSpeed;
    }

    public void setMotorSpeed(double speed) {
        this.motorSpeed = Mth.clamp(speed, -1.0, 1.0);
    }

    @Override
    public InteractionResult openGui(Player player) {
        if (hasGui() && !isRemote() && !player.isShiftKeyDown()) {
            if (hasSecurity() && !IBlockSecurityUtils.INSTANCE.canAccessOrDisplayError(player, player.level(), worldPosition, this)) {
                return InteractionResult.FAIL;
            }
            ItemStack stack = player.getMainHandItem();
            if (isDirectional() && !stack.isEmpty() && (stack.is(FNItems.CONDUIT_CORE) || stack.is(FNBlocks.CONDUIT_PORT.asItem()))) {
                return InteractionResult.PASS;
            }
            return super.openGui(player);
        }
        return InteractionResult.PASS;
    }
}