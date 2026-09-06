package dev.alyti.fluxnautics.capabilities;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import mekanism.api.IContentsListener;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.math.MathUtils;
import mekanism.common.block.attribute.AttributeEnergy;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.SharedConstants;
import org.jetbrains.annotations.Nullable;

import java.util.function.LongSupplier;

public class MassBasedEnergyContainer<TILE extends TileEntityMekanism> extends MachineEnergyContainer<TILE> {

    public static final long USAGE_MULTIPLIER = 20 * SharedConstants.TICKS_PER_SECOND;

    private double lastMass = 0;
    private double lastPassiveMult = 1.0;
    private LongSupplier energyPerMassPerTickMultiplier;
    private LongSupplier energyPerTickPassiveMultiplier;

    public MassBasedEnergyContainer(TILE tile, @Nullable IContentsListener listener, LongSupplier energyPerMassPerTickMultiplier) {
        this(tile, listener, energyPerMassPerTickMultiplier, () -> 0L);
    }

    public MassBasedEnergyContainer(TILE tile, @Nullable IContentsListener listener, LongSupplier energyPerMassPerTickMultiplier, LongSupplier energyPerTickPassiveMultiplier) {
        super(Long.MAX_VALUE, 0L, notExternal, ConstantPredicates.alwaysTrue(), tile, listener);
        AttributeEnergy electricBlock = validateBlock(tile);
        this.energyPerMassPerTickMultiplier = energyPerMassPerTickMultiplier;
        this.energyPerTickPassiveMultiplier = energyPerTickPassiveMultiplier;
    }

        @Override
    public boolean adjustableRates() {
        return true;
    }

    public double getLastMass() {
        return lastMass;
    }

    public void updateMassConstant(double mass) {
        if (lastMass == mass) {
            return;
        }
        lastMass = mass;
        updateMaxEnergy();
    }

    public void updatePassiveMultConstant(double passiveMult) {
        if (lastPassiveMult == passiveMult) {
            return;
        }
        lastPassiveMult = passiveMult;
        updateMaxEnergy();
    }

    @Override
    public long getBaseMaxEnergy() {
        if (energyPerMassPerTickMultiplier == null || lastMass == 0) {
            return 320_000_000L;
        }
        return MathUtils.multiplyClamped(energyPerMassPerTickMultiplier.getAsLong() * USAGE_MULTIPLIER, Math.round(lastMass)) + MathUtils.multiplyClamped((long) (energyPerTickPassiveMultiplier.getAsLong() * lastPassiveMult), USAGE_MULTIPLIER);
    }

    public void updateFromSublevel(ServerSubLevel subLevel) {
        updateMassConstant(subLevel.getMassTracker().getMass());
    }
}
