package dev.alyti.fluxnautics.content.blocks;

import dev.alyti.fluxnautics.capabilities.MassBasedEnergyContainer;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.math.MathUtils;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractMassMachineBlockEntity extends TileEntityConfigurableMachine implements BlockEntitySubLevelActor {
    protected MassBasedEnergyContainer<?> energyContainer;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getChargeItem", docPlaceholder = "charge slot")
    EnergyInventorySlot chargeSlot;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getDischargeItem", docPlaceholder = "discharge slot")
    EnergyInventorySlot dischargeSlot;

    public long energyCost;
    public long energyUsed;

    public AbstractMassMachineBlockEntity(Holder<Block> blockProvider, BlockPos pos, BlockState state) {
        super(blockProvider, pos, state);

        configComponent.setupIOConfig(TransmissionType.ITEM, chargeSlot, dischargeSlot, RelativeSide.FRONT, true).setCanEject(false);
        configComponent.setupIOConfig(TransmissionType.ENERGY, energyContainer, RelativeSide.FRONT);
        ejectorComponent = new TileComponentEjector(this, () -> Long.MAX_VALUE, false);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ENERGY).setCanEject(type -> canFunction());
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);
        builder.addSlot(dischargeSlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 8, 35));
        builder.addSlot(chargeSlot = EnergyInventorySlot.drain(energyContainer, listener, 8, 53));

        dischargeSlot.setSlotOverlay(SlotOverlay.MINUS);
        chargeSlot.setSlotOverlay(SlotOverlay.PLUS);
        return builder.build();
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energyContainer = new MassBasedEnergyContainer<>(this, listener, this::getEnergyPerMassPerTickMultiplier));
        return builder.build();
    }

    public MachineEnergyContainer<?> getEnergyContainer() {
        return energyContainer;
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableLong.create(this::getEnergyUsed, value -> energyUsed = value));
        container.track(SyncableLong.create(this::getEnergyCost, value -> energyCost = value));
    }

    public long getEnergyPerMassPerTickMultiplier() {
        return MathUtils.multiplyClamped(Math.round(Math.ceil(getEnergyPerMassMultiplier())), 2);
    }

    public abstract double getEnergyPerMassMultiplier();

    public abstract boolean countConnected();

    public abstract long calcEnergyCost();

    public abstract void onSublevelTick(ServerSubLevel subLevel, boolean active);

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        chargeSlot.drainContainer();
        dischargeSlot.fillContainerOrConvert();
        energyCost = calcEnergyCost();

        this.setActive(this.canFunction());

        long energyUsedNow = 0;
        if (this.getActive()) {
            energyUsedNow = this.energyContainer.extract(energyCost, Action.EXECUTE, AutomationType.INTERNAL);
        }

        if (energyUsedNow != energyUsed || (ticker % (2 * SharedConstants.TICKS_PER_SECOND) == 0)) {
            energyUsed = energyUsedNow;
            sendUpdatePacket = true;
        }
        return sendUpdatePacket;
    }

    @Override
    public void sable$tick(final ServerSubLevel subLevel) {
        if (countConnected()) {
            if (ticker % (2 * SharedConstants.TICKS_PER_SECOND) == 0) {
                double totalMass = 0;
                for (SubLevel subSubLevel : SubLevelHelper.getConnectedChain(subLevel)) {
                    ServerSubLevel subLevel2 = (ServerSubLevel) subSubLevel;
                    totalMass += subLevel2.getMassTracker().getMass();
                }
                this.energyContainer.updateMassConstant(totalMass);
            }
        } else {
            this.energyContainer.updateFromSublevel(subLevel);
        }
        this.onSublevelTick(subLevel, this.getActive());
    }

    @ComputerMethod
    public long getEnergyCost() {
        if (this.level == null || this.isRemote())
            return energyCost;
        return calcEnergyCost();
    }

    @ComputerMethod
    public long getEnergyUsed() {
        return energyUsed;
    }

    @Override
    @ComputerMethod
    public boolean canFunction() {
        return super.canFunction() && (this.energyContainer.getEnergy() >= energyCost);
    }
}
