package dev.alyti.fluxnautics.content.blocks.forcefield;

import dev.alyti.fluxnautics.registry.FNContainerTypes;
import dev.alyti.fluxnautics.util.FNDistUtil;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.container.type.MekanismContainerType;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;

public class ForcefieldEmitterContainer extends MekanismTileContainer<ForcefieldEmitterBlockEntity> {

    public final ForcefieldBubbleEmitterConfig config;

    private ForcefieldEmitterContainer(int id, Inventory inv, ForcefieldEmitterBlockEntity tile, boolean remote, ForcefieldBubbleEmitterConfig config) {
        super(FNContainerTypes.FORCEFIELD_EMITTER, id, inv, tile);
        this.config = config;
        this.
            addSlotsAndOpen();
    }

    public static  MekanismContainerType<ForcefieldEmitterBlockEntity, ForcefieldEmitterContainer> configContainer() {
        return new MekanismContainerType<>(ForcefieldEmitterBlockEntity.class,
            (id, inv, tile) -> new ForcefieldEmitterContainer(id, inv, tile, false, ForcefieldBubbleEmitterConfig.INITIAL_SERVER),
            (id, inv, buf) -> new ForcefieldEmitterContainer(id, inv, getTileFromBuf(buf, ForcefieldEmitterBlockEntity.class), true, ForcefieldBubbleEmitterConfig.STREAM_CODEC.decode(buf))
        ) {};
    }

    @Override
    protected int getInventoryXOffset() {
        return super.getInventoryXOffset();
    }

    @Override
    protected int getInventoryYOffset() {
        return super.getInventoryYOffset() + 100;
    }

//    @Override
//    public boolean canPlayerAccess(@NotNull Player player) {
//        return true;
//    }
//
//    @Override
//    public boolean stillValid(Player player) {
//        return true;
//    }

    @NotNull
    private static <TILE extends BlockEntity> TILE getTileFromBuf(FriendlyByteBuf buf, Class<TILE> type) {
        if (buf == null) {
            throw new IllegalArgumentException("Null packet buffer");
        } else if (!FMLEnvironment.dist.isClient()) {
            throw new UnsupportedOperationException("This method is only supported on the client.");
        }
        BlockPos pos = buf.readBlockPos();
        TILE tile = WorldUtils.getTileEntity(type, FNDistUtil.getClientLevel(), pos);
        if (tile == null) {
            throw new IllegalStateException("Client could not locate tile at " + pos + " for tile container. "
                + "This is likely caused by a mod breaking client side tile lookup");
        }
        return tile;
    }
}
