package dev.alyti.fluxnautics.api.content.redstone;

import net.minecraft.core.Direction;

/**
 * Implement on tiles whose redstone side configuration should be editable from the GUI
 * window/tab and from Mekanism's configurator (via our ItemConfigurator mixin). It is to
 * redstone control what {@code ISideConfiguration} is to Mekanism's DataType config.
 */
public interface IRedstoneSideConfigurable {

    /**
     * Machine orientation; maps clicked world faces to {@link mekanism.api.RelativeSide}s.
     * Same contract as {@code ISideConfiguration#getDirection()}, TileEntityMekanism already
     * provides this, so subclasses satisfy it for free.
     */
    Direction getDirection();

    TileComponentRedstoneControl<?, ?> getRedstoneConfig();
}