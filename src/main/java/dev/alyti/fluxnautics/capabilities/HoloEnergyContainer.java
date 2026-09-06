package dev.alyti.fluxnautics.capabilities;

import dev.alyti.fluxnautics.content.blocks.holomap.HoloBlockEntity;
import mekanism.api.IContentsListener;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.math.MathUtils;
import mekanism.common.block.attribute.AttributeEnergy;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.config.MekanismConfig;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class HoloEnergyContainer extends MachineEnergyContainer<HoloBlockEntity> {

    public static HoloEnergyContainer input(HoloBlockEntity tile, @Nullable IContentsListener listener) {
        AttributeEnergy electricBlock = validateBlock(tile);
        return new HoloEnergyContainer(electricBlock.getStorage(), electricBlock.getUsage(), tile, listener);
    }

    private long minerEnergyPerTick;

    private HoloEnergyContainer(long maxEnergy, long energyPerTick, HoloBlockEntity tile, @Nullable IContentsListener listener) {
        super(maxEnergy, energyPerTick, notExternal, ConstantPredicates.alwaysTrue(), tile, listener);
        this.minerEnergyPerTick = getBaseEnergyPerTick();
    }

    @Override
    public void setEnergyPerTick(long energyPerTick) {
        super.setEnergyPerTick(energyPerTick);
        this.minerEnergyPerTick = energyPerTick;
    }

    @Override
    public long getEnergyPerTick() {
        return minerEnergyPerTick;
    }

    @Override
    public void updateEnergyPerTick() {
        super.updateEnergyPerTick();
        updateMinerEnergyPerTick();
    }

    public void updateMinerEnergyPerTick() {
        minerEnergyPerTick = super.getEnergyPerTick();
//        if (tile.getSilkTouch()) {
//            minerEnergyPerTick = MathUtils.multiplyClamped(minerEnergyPerTick, MekanismConfig.general.minerSilkMultiplier.get());
//        }
        //Ranges are difference between max and default
        double radiusRange = MekanismConfig.general.minerMaxRadius.get() - TileEntityDigitalMiner.DEFAULT_RADIUS;
        double heightRange;
        Level level = tile.getLevel();
        if (level == null) {
            //Default to a world height of 255 (pre 1.18 height)
            heightRange = 255 - TileEntityDigitalMiner.DEFAULT_HEIGHT_RANGE;
        } else {
            //Note: We adjust the height by one as the height range is "zero indexed"
            heightRange = level.getHeight() - 1 - TileEntityDigitalMiner.DEFAULT_HEIGHT_RANGE;
        }
        //If the range for a specific thing is zero, ignore it from the cost calculations
        double radiusCost;
        double heightCost;
        if (radiusRange == 0) {
            radiusCost = 0;
        } else {
            radiusCost = Math.max((tile.getRadius() - TileEntityDigitalMiner.DEFAULT_RADIUS) / radiusRange, 0);
        }
        if (heightRange == 0) {
            heightCost = 0;
        } else {
            heightCost = 0;//  Math.max((tile.getMaxY() - tile.getMinY() - TileEntityDigitalMiner.DEFAULT_HEIGHT_RANGE) / heightRange, 0);
        }
        minerEnergyPerTick = MathUtils.ceilToLong(minerEnergyPerTick * (1 + radiusCost) * (1 + heightCost));
    }
}