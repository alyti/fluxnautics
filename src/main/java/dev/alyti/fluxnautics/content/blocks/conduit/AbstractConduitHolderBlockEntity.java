package dev.alyti.fluxnautics.content.blocks.conduit;

import dev.alyti.fluxnautics.api.content.tooltip.IHoverTooltip;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.client.ClientConduitStrand;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.server.ConduitAttachment;
import dev.alyti.fluxnautics.content.blocks.conduit.strand.server.ServerConduitStrand;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public abstract class AbstractConduitHolderBlockEntity extends TileEntityMekanism implements BlockEntitySubLevelActor, IHoverTooltip {
    private final TileComponentConduitStrandHolder<?> componentStrandHolder;

    abstract Vec3 getAttachmentPoint(final BlockPos pos, final BlockState state);

    Vec3 getVisualAttachmentPoint(final BlockPos pos, final BlockState state) {
        return this.getAttachmentPoint(pos, state);
    }

    public AbstractConduitHolderBlockEntity(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);
        this.componentStrandHolder = new TileComponentConduitStrandHolder<>(this);
    }

    @Override
    public boolean onUpdateServer() {
        this.componentStrandHolder.tick();

        return super.onUpdateServer();
    }

    public TileComponentConduitStrandHolder<?> getStrandHolder() {
        return this.componentStrandHolder;
    }

    @Override
    @Nullable
    public Iterable<@NotNull SubLevel> sable$getConnectionDependencies() {
        final TileComponentConduitStrandHolder<?> behavior = this.getStrandHolder();

        final Level level = this.getLevel();
        final SubLevelContainer container = SubLevelContainer.getContainer(level);
        assert container != null;

        final ServerConduitStrand serverStrand = behavior.getAttachedStrand();

        if (serverStrand != null) {
            final ObjectList<SubLevel> dependencies = new ObjectArrayList<>();
            final Iterable<ConduitAttachment> attachments = serverStrand.getAttachments();

            for (final ConduitAttachment attachment : attachments) {
                final UUID id = attachment.subLevelID();
                if (id == null) continue;

                final SubLevel subLevel = container.getSubLevel(id);

                if (subLevel != null) {
                    dependencies.add(subLevel);
                }
            }

            return dependencies;
        }

        final ClientConduitStrand clientStrand = behavior.getClientStrand();

        if (clientStrand != null) {
            final ObjectList<SubLevel> dependencies = new ObjectArrayList<>();

            final SubLevel subLevelA = Sable.HELPER.getContaining(level, clientStrand.startAttachment);
            final SubLevel subLevelB = Sable.HELPER.getContaining(level, clientStrand.endAttachment);

            if (subLevelA != null) dependencies.add(subLevelA);
            if (subLevelB != null) dependencies.add(subLevelB);

            return dependencies;
        }

        return null;
    }

    @Override
    public void addLines(List<Component> list) {
        TileComponentConduitStrandHolder<?> strand =  this.getStrandHolder();
        if (strand != null) {
            if (strand.isAttached()) {
                list.add(Component.literal("Conduit connected and %s".formatted(strand.ownsConduit() ? "owned by this point" : "owned by the other point")));
                ClientConduitStrand clientStrand = strand.getClientStrand();
                if (clientStrand != null) {
                    list.add(Component.literal("Current extension: %.2f (%d points)".formatted(clientStrand.getCurrentExtension(), clientStrand.getPoints().size())));
                }
            } else {
                list.add(Component.literal("Conduit not connected"));
            }
        }
    }
}
