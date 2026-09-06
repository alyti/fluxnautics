package dev.alyti.fluxnautics.content.blocks.holomap;

import com.mojang.logging.LogUtils;
import dev.alyti.fluxnautics.index.BlockShapes;
import dev.alyti.fluxnautics.registry.FNBlockTypes;
import dev.ryanhcode.sable.Sable;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeGui;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.Machine;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.WorldUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public class HoloBlock extends BlockTile.BlockTileModel<HoloBlockEntity, Machine<HoloBlockEntity>> {

    public HoloBlock(UnaryOperator<BlockBehaviour.Properties> propertiesModifier) {
        super(FNBlockTypes.HOLO, propertiesModifier);
    }

    @NotNull
    @Override
    protected ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player,
                                              @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        Minecraft mc = Minecraft.getInstance();

        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        TileEntityMekanism tile = WorldUtils.getTileEntity(TileEntityMekanism.class, world, pos);
        if (tile == null) {
            //No tile, we can just skip trying to use without an item
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        } else if (world.isClientSide) {
            return genericClientActivated(stack, tile);
        }
        return tile.tryWrench(state, player, stack).getInteractionResult();
    }

    @NotNull
    @Override
    protected InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        Minecraft mc = Minecraft.getInstance();

//        if (!world.isClientSide) {
            Vec3 start = player.position()
                    .add(0, player.getEyeHeight(), 0);
            Vec3 range = player.getLookAngle()
                    .scale(10);
            BlockHitResult raytrace =
                    world.clip(new ClipContext(start, start.add(range), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            BlockHitResult raytraceSable = new BlockHitResult(
                    Sable.HELPER.projectOutOfSubLevel(world, raytrace.getLocation()),
                    raytrace.getDirection(),
                    raytrace.getBlockPos(),
                    raytrace.isInside()
            );

            LogUtils.getLogger().info("Location: {}", raytraceSable.getLocation().subtract(pos.getCenter()), raytrace.getLocation().subtract(pos.getCenter()));
//        }

        TileEntityMekanism tile = WorldUtils.getTileEntity(TileEntityMekanism.class, world, pos);
        if (tile == null) {
            return InteractionResult.PASS;
        } else if (world.isClientSide) {
            return Attribute.has(this, AttributeGui.class) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return tile.openGui(player);
    }


    @Override
    protected VoxelShape getInteractionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return BlockShapes.HOLO_GRAM[0];
    }


//    @NotNull
//    @Override
//    protected VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
//        if (!(context instanceof EntityCollisionContext entityCollisionContext) || !(entityCollisionContext.getEntity() instanceof Projectile)) {
//            return BASE;
//        }
//        return super.getCollisionShape(state, level, pos, context);
//    }
}