package dev.alyti.fluxnautics.content.blocks.field;

import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.capabilities.MassBasedEnergyContainer;
import dev.alyti.fluxnautics.client.render.field.FieldShadowRenderer;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.alyti.fluxnautics.registry.FNDataComponents;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import mekanism.api.*;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.common.capabilities.chemical.VariableCapacityChemicalTank;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import org.joml.Vector3d;
import mekanism.common.inventory.container.sync.SyncableDouble;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.chemical.ChemicalInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registries.MekanismChemicals;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.interfaces.IBoundingBlock;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class FieldDriveBlockEntity extends TileEntityConfigurableMachine implements IBoundingBlock, BlockEntitySubLevelActor {
    private static final List<RecipeError> TRACKED_ERROR_TYPES = List.of(
            RecipeError.NOT_ENOUGH_ENERGY,
            RecipeError.NOT_ENOUGH_INPUT,
            RecipeError.NOT_ENOUGH_SECONDARY_INPUT,
            RecipeError.NOT_ENOUGH_OUTPUT_SPACE,
            RecipeError.INPUT_DOESNT_PRODUCE_OUTPUT
    );

    public static final Predicate<ChemicalStack> HAS_WARPFUEL = chemical -> chemical.is(MekanismChemicals.ANTIMATTER);

    public static final long MAX_WARPFUEL = 100L * FluidType.BUCKET_VOLUME;
    public static final int BASE_TICKS_REQUIRED = 20 * SharedConstants.TICKS_PER_SECOND;

    private MassBasedEnergyContainer<FieldDriveBlockEntity> energyContainer;

    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerChemicalTankWrapper.class, methodNames = {"getInputChemical", "getInputChemicalCapacity", "getInputChemicalNeeded",
            "getInputChemicalFilledPercentage"}, docPlaceholder = "input gas tank")
    public IChemicalTank gasTank;

    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getInputChemicalItem", docPlaceholder = "input chemical item slot")
    ChemicalInventorySlot inputChemicalSlot;

    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getEnergyItem", docPlaceholder = "energy slot")
    EnergyInventorySlot energySlot;

    private int operatingTicks;
    protected int baseTicksRequired;
    public int ticksRequired;
    private int operationsPerTick = 1;//will increase for modified upgrade multipliers
    private boolean usedEnergy = false;

    private double impulseVectorX;
    private double impulseVectorY;
    private double impulseVectorZ;
    private double impulseMultiplier;
    private boolean isGlobal;

    private Vec3 setImpulse;
    private boolean setGlobal;
    private boolean clientSetImpulse;

    private long clientEnergyCost;
    private long clientChemicalCost;

    public FieldDriveBlockEntity(BlockPos pos, BlockState state) {
        super(FNBlocks.FIELD_DRIVE, pos, state);

        setControlType(RedstoneControl.PULSE);

        this.setImpulse = Vec3.ZERO;
        this.baseTicksRequired = BASE_TICKS_REQUIRED;
        ticksRequired = this.baseTicksRequired;

        ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
        if (itemConfig != null) {
            itemConfig.addSlotInfo(DataType.INPUT, new InventorySlotInfo(true, true, inputChemicalSlot));
            itemConfig.addSlotInfo(DataType.ENERGY, new InventorySlotInfo(true, true, energySlot));
        }

//        configComponent.setupInputConfig(TransmissionType.CHEMICAL, gasTank);
//        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);
        configComponent.setupIOConfig(TransmissionType.CHEMICAL, gasTank, RelativeSide.BOTTOM, true);
        configComponent.setupIOConfig(TransmissionType.ENERGY, energyContainer, RelativeSide.FRONT, true);


        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ENERGY);
    }

    @Override
    public @Nullable IChemicalTankHolder getInitialChemicalTanks(IContentsListener listener) {
        ChemicalTankHelper builder = ChemicalTankHelper.forSideWithConfig(this);
        builder.addTank(gasTank = new WarpFuelTank(listener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energyContainer = new MassBasedEnergyContainer<>(this, listener, this::getEnergyPerMassPerTickMultiplier));
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this);
        builder.addSlot(inputChemicalSlot = ChemicalInventorySlot.fill(gasTank, listener, 101, 52));
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 143, 35));
        inputChemicalSlot.setSlotType(ContainerSlotType.INPUT);
        return builder.build();
    }

    public long getEnergyPerMassPerTickMultiplier() {
        return Math.round(Long.valueOf(FluxnauticsConfig.common.fieldDriveJoulesPerImpulseMult.getOrDefault()).doubleValue() * FluxnauticsConfig.common.fieldDriveImpulseMultCap.getOrDefault());
    }

    @Override
    public void sable$tick(final ServerSubLevel subLevel) {
        this.energyContainer.updateFromSublevel(subLevel);
    }

    public MachineEnergyContainer<?> getEnergyContainer() {
        return energyContainer;
    }

    @Override
    protected void onUpdateClient() {
        super.onUpdateClient();

        if (!FieldShadowRenderer.renderingShadowMap() && this.getActive()) {
            FieldShadowRenderer.addVoidAnchor(this);
        }
    }

    public double getScaledProgress() {
        return getOperatingTicks() / (double) ticksRequired;
    }

    protected void setOperatingTicks(int ticks) {
        this.operatingTicks = ticks;
    }

    @ComputerMethod(nameOverride = "getRecipeProgress")
    public int getOperatingTicks() {
        return operatingTicks;
    }

    @ComputerMethod
    public int getTicksRequired() {
        return ticksRequired;
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        operatingTicks = nbt.getInt(SerializationConstants.PROGRESS);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbtTags, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(nbtTags, provider);
        nbtTags.putInt(SerializationConstants.PROGRESS, getOperatingTicks());
    }


    @Override
    public void readSustainedData(HolderLookup.Provider provider, @NotNull CompoundTag data) {
        super.readSustainedData(provider, data);

        isGlobal = data.getBoolean("isGlobal");
        impulseMultiplier = data.getDouble("impulseMultiplier");
        impulseVectorX = data.getDouble("impulseVectorX");
        impulseVectorY = data.getDouble("impulseVectorY");
        impulseVectorZ = data.getDouble("impulseVectorZ");
    }

    @Override
    public void writeSustainedData(HolderLookup.Provider provider, CompoundTag data) {
        super.writeSustainedData(provider, data);
        data.putBoolean("isGlobal", isGlobal);
        data.putDouble("impulseMultiplier", impulseMultiplier);
        data.putDouble("impulseVectorX", impulseVectorX);
        data.putDouble("impulseVectorY", impulseVectorY);
        data.putDouble("impulseVectorZ", impulseVectorZ);
    }

    @Override
    protected void applyImplicitComponents(@NotNull BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        setImpulseMultiplier(input.getOrDefault(FNDataComponents.IMPULSE_MULT, impulseMultiplier));
        setImpulseVectorX(input.getOrDefault(FNDataComponents.IMPULSE_VEC_X, impulseVectorX));
        setImpulseVectorY(input.getOrDefault(FNDataComponents.IMPULSE_VEC_Y, impulseVectorY));
        setImpulseVectorZ(input.getOrDefault(FNDataComponents.IMPULSE_VEC_Z, impulseVectorZ));
        setGlobal(input.getOrDefault(FNDataComponents.IS_GLOBAL, isGlobal));
    }

    @Override
    protected void collectImplicitComponents(@NotNull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(FNDataComponents.IMPULSE_MULT, impulseMultiplier);
        builder.set(FNDataComponents.IMPULSE_VEC_X, impulseVectorX);
        builder.set(FNDataComponents.IMPULSE_VEC_Y, impulseVectorY);
        builder.set(FNDataComponents.IMPULSE_VEC_Z, impulseVectorZ);
        builder.set(FNDataComponents.IS_GLOBAL, isGlobal);
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == Upgrade.SPEED) {
            ticksRequired = MekanismUtils.getTicks(this, baseTicksRequired);
            operationsPerTick = MekanismUtils.getOperationsPerTick(this, baseTicksRequired, 1);
        }
    }

    public int getOperationsPerTick() {
        return this.operationsPerTick;
    }

    @NotNull
    @Override
    public List<Component> getInfo(@NotNull Upgrade upgrade) {
        return UpgradeUtils.getMultScaledInfo(this, upgrade);
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(this::getOperatingTicks, this::setOperatingTicks));
        container.track(SyncableInt.create(this::getTicksRequired, value -> ticksRequired = value));
        container.track(SyncableBoolean.create(this::usedEnergy, value -> usedEnergy = value));
        container.track(SyncableBoolean.create(this::isGlobal, value -> isGlobal = value));
        container.track(SyncableDouble.create(this::getImpulseMultiplier,  value -> impulseMultiplier = value));
        container.track(SyncableDouble.create(this::getImpulseVectorX, value -> impulseVectorX = value));
        container.track(SyncableDouble.create(this::getImpulseVectorY, value -> impulseVectorY = value));
        container.track(SyncableDouble.create(this::getImpulseVectorZ, value -> impulseVectorZ = value));
        container.track(SyncableLong.create(this::getEnergyCost, value -> clientEnergyCost = value));
        container.track(SyncableLong.create(this::getWarpfuelCost, value -> clientChemicalCost = value));
        container.track(SyncableBoolean.create(this::canActivate, value -> clientSetImpulse = value));

    }

    public boolean usedEnergy() {
        return usedEnergy;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    @ComputerMethod
    public void setGlobal(boolean isGlobal) {
        if (isGlobal != this.isGlobal) {
            this.isGlobal = isGlobal;
            markForSave();
        }
    }

    @ComputerMethod
    public double getImpulseMultiplier() {
        return impulseMultiplier;
    }

    @ComputerMethod
    public void setImpulseMultiplier(double target) {
        target = Math.clamp(target, 1, FluxnauticsConfig.common.fieldDriveImpulseMultCap.getAsDouble());
        if (this.impulseMultiplier != target) {
            this.impulseMultiplier = target;
            markForSave();
        }
    }

    @ComputerMethod
    public double getImpulseVectorX() {
        return impulseVectorX;
    }

    @ComputerMethod
    public void setImpulseVectorX(double target) {
        target = Math.clamp(target, -1, 1);
        if (this.impulseVectorX != target) {
            this.impulseVectorX = target;
            markForSave();
        }
    }

    @ComputerMethod
    public double getImpulseVectorY() {
        return impulseVectorY;
    }

    @ComputerMethod
    public void setImpulseVectorY(double target) {
        target = Math.clamp(target, -1, 1);
        if (this.impulseVectorY != target) {
            this.impulseVectorY = target;
            markForSave();
        }
    }

    @ComputerMethod
    public double getImpulseVectorZ() {
        return impulseVectorZ;
    }

    @ComputerMethod
    public void setImpulseVectorZ(double target) {
        target = Math.clamp(target, -1, 1);
        if (this.impulseVectorZ != target) {
            this.impulseVectorZ = target;
            markForSave();
        }
    }

    public Vec3 getImpulseVector() {
        return new Vec3(impulseVectorX, impulseVectorY, impulseVectorZ);
    }

    public Vec3 getImpulse() {
        return getImpulseVector().scale(impulseMultiplier);
    }

    @ComputerMethod
    public long getWarpfuelCost() {
        if (this.isRemote()) {
            return clientChemicalCost;
        }

        return Math.round(Math.ceil(getImpulse().length())) * FluxnauticsConfig.common.fieldDriveChemicalsPerImpulseMult.get();
    }

    @ComputerMethod
    public double getSublevelMass() {
        double mass = Double.MAX_VALUE;

        SubLevelAccess sla = Sable.HELPER.getContaining(this);
        if (sla instanceof final ServerSubLevel serverSubLevel) {
            mass = serverSubLevel.getMassTracker().getMass();
        }

        return mass;
    }

    private ServerSubLevel getServerSubLevel() {
        SubLevelAccess sla = Sable.HELPER.getContaining(this);
        if (sla instanceof final ServerSubLevel serverSubLevel) {
            return serverSubLevel;
        }
        return null;
    }

    @ComputerMethod
    public long getEnergyCost() {
        if (this.isRemote()) {
            return clientEnergyCost;
        }
        return Math.round((getSublevelMass() * FluxnauticsConfig.common.fieldDriveJoulesPerMassMult.getAsDouble())
                * getImpulse().length()
                * (isGlobal ? 4 : 1))
                * FluxnauticsConfig.common.fieldDriveJoulesPerImpulseMult.getOrDefault();
    }

    public boolean canActivate() {
        if (this.isRemote()) {
            return clientSetImpulse;
        }

        // impulse target is already set
        if (!this.setImpulse.equals(Vec3.ZERO)) {
            return false;
        }
        return energyContainer.getEnergy() >= getEnergyCost() && gasTank.getStored() >= getWarpfuelCost();
    }

    @Override
    public boolean supportsMode(RedstoneControl mode) {
        return mode == RedstoneControl.PULSE;
    }

    @Override
    public boolean canFunction() {
        return !this.setImpulse.equals(Vec3.ZERO);
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();

        energySlot.fillContainerOrConvert();
        inputChemicalSlot.fillTank();

        if (canActivate() && isRedstoneActivated()) {
            double mass = getSublevelMass();
            if (energyContainer.extract(getEnergyCost(), Action.EXECUTE, AutomationType.INTERNAL) == getEnergyCost()
                && gasTank.extract(getWarpfuelCost(), Action.EXECUTE, AutomationType.INTERNAL).getAmount() == getWarpfuelCost()) {
                this.setImpulse = getImpulse().scale(mass);
                if (isGlobal) {
                    assert getServerSubLevel() != null;
                    this.setImpulse = getServerSubLevel().logicalPose().transformNormalInverse(this.setImpulse);
                }
                setActive(true);
                return true;
            }
        } else if (canFunction()) {
            operatingTicks++;
            if (operatingTicks >= ticksRequired) {
                operatingTicks = 0;
                setActive(false);
                assert getServerSubLevel() != null;
                RigidBodyHandle handle = RigidBodyHandle.of(getServerSubLevel());
                handle.applyLinearImpulse(JOMLConversion.toJOML(setImpulse));
                setImpulse = Vec3.ZERO;
            }
            return true;
        } else {
            setActive(false);
        }
        return sendUpdatePacket;
    }

    @Override
    public boolean isOffsetCapabilityDisabled(@NotNull BlockCapability<?, @Nullable Direction> capability, Direction side, @NotNull Vec3i offset) {
        return false;
    }

    //Implementation of gas tank that on no longer being empty updates the output rate of this generator
    public class WarpFuelTank extends VariableCapacityChemicalTank {

        protected WarpFuelTank(@Nullable IContentsListener listener) {
            super(() -> MAX_WARPFUEL, ConstantPredicates.notExternal(), ConstantPredicates.alwaysTrueBi(), HAS_WARPFUEL, null, listener);
        }

        @Override
        public void setStack(@NotNull ChemicalStack stack) {
            boolean wasEmpty = isEmpty();
            super.setStack(stack);
            recheckOutput(stack, wasEmpty);
        }

        @Override
        public void setStackUnchecked(@NotNull ChemicalStack stack) {
            boolean wasEmpty = isEmpty();
            super.setStackUnchecked(stack);
            recheckOutput(stack, wasEmpty);
        }

        private void recheckOutput(@NotNull ChemicalStack stack, boolean wasEmpty) {
            if (wasEmpty && !stack.isEmpty()) {
//                ChemicalFuel fuel = getFuel();
//                if (fuel != null) {
//                    //updateMaxOutputRaw(fuel.energyMult());
//                }
            }
        }
    }
}
