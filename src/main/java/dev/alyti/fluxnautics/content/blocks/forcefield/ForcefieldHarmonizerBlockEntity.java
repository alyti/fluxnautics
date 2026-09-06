package dev.alyti.fluxnautics.content.blocks.forcefield;

import dev.alyti.fluxnautics.index.HarmonicResonanceFilter;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.functions.LongObjectToLongFunction;
import mekanism.api.math.MathUtils;
import mekanism.common.capabilities.energy.FixedUsageEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.content.filter.SortableFilterManager;
import mekanism.common.integration.computer.ComputerException;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.ComputerMethod;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.interfaces.ITileFilterHolder;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;

public class ForcefieldHarmonizerBlockEntity extends TileEntityConfigurableMachine implements ITileFilterHolder<HarmonicResonanceFilter<?>>, BlockEntitySubLevelActor {
    private static final Map<ResourceKey<Level>, Map<ServerSubLevel, HarmonizerRecordingTicket>> mapOfHarmonizers = new WeakHashMap<>();
    private static final Map<ResourceKey<Level>, Map<ServerSubLevel, Set<HarmonizerRecordingTicket>>> mapOfChildSublevelsToRootSublevel = new WeakHashMap<>();
    private static final LongObjectToLongFunction<ForcefieldHarmonizerBlockEntity> BASE_ENERGY_CALCULATOR = (base, tile) -> MathUtils.multiplyClamped(base, tile.filterManager.getEnabledFilters().size());

    public final Predicate<ItemStack> CHECK_HARMONY = stack -> getFilterManager().anyEnabledMatch(stack, (filter, s) -> filter.getFinder().test(s));

    @SuppressWarnings({"unchecked", "rawtypes"})
    private final SortableFilterManager<HarmonicResonanceFilter<?>> filterManager = new SortableFilterManager<HarmonicResonanceFilter<?>>((Class) HarmonicResonanceFilter.class, this::updateEnergyAndMarkForSave);

    private FixedUsageEnergyContainer<ForcefieldHarmonizerBlockEntity> energyContainer;
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getEnergyItem", docPlaceholder = "energy slot")
    EnergyInventorySlot energySlot;

    public ForcefieldHarmonizerBlockEntity(BlockPos pos, BlockState state) {
        super(FNBlocks.FORCEFIELD_HARMONIZER, pos, state);

        configComponent.setupInputConfig(TransmissionType.ITEM, energySlot);
        configComponent.setupIOConfig(TransmissionType.ENERGY, energyContainer, RelativeSide.FRONT);
        ejectorComponent = new TileComponentEjector(this, () -> Long.MAX_VALUE, false);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ENERGY).setCanEject(type -> canFunction());
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSide(facingSupplier);
        builder.addContainer(energyContainer = FixedUsageEnergyContainer.input(this, BASE_ENERGY_CALCULATOR, listener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSide(facingSupplier);
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 143, 35), RelativeSide.BACK);
        return builder.build();
    }

    public FixedUsageEnergyContainer<?> getEnergyContainer() {
        return energyContainer;
    }

    public void updateEnergyAndMarkForSave() {
        energyContainer.updateEnergyPerTick();
        setChanged(false);
    }


    @Override
    public void sable$tick(final ServerSubLevel serverSubLevel) {
        updateHarmonizerFor(serverSubLevel, this, this.getActive());
    }

    public static void updateHarmonizerFor(final ServerSubLevel serverSubLevel, final ForcefieldHarmonizerBlockEntity be, boolean keep) {
        final Map<ServerSubLevel, HarmonizerRecordingTicket> map = mapOfHarmonizers.get(serverSubLevel.getLevel().dimension());
        HarmonizerRecordingTicket ticket = map != null ? map.get(serverSubLevel) : null;

        if (ticket != null && !ticket.isValid()) {
            mapOfHarmonizers.remove(serverSubLevel);
            ticket = null;
            if (!keep) return;
        }


        if (ticket == null) {
            Set<ForcefieldHarmonizerBlockEntity> blockEntities = new ObjectOpenHashSet<>();
            ticket = new HarmonizerRecordingTicket(serverSubLevel, blockEntities);
            mapOfHarmonizers.computeIfAbsent(serverSubLevel.getLevel().dimension(), x -> new Object2ObjectOpenHashMap<>())
                    .put(serverSubLevel, ticket);
        }
        if (keep)
            ticket.blockEntities.add(be);
        else
            ticket.blockEntities.remove(be);

        final Map<ServerSubLevel, Set<HarmonizerRecordingTicket>> mapSubs = mapOfChildSublevelsToRootSublevel.get(serverSubLevel.getLevel().dimension());
        for (final SubLevel subLevel : SubLevelHelper.getConnectedChain(serverSubLevel)) {
            ServerSubLevel ssb = (ServerSubLevel) subLevel;
            Set<HarmonizerRecordingTicket> tickets = mapSubs != null ? mapSubs.get(ssb) : null;
            if (tickets == null) {
                if (!keep) continue;
                tickets = new  ObjectOpenHashSet<>();
                mapOfChildSublevelsToRootSublevel.computeIfAbsent(serverSubLevel.getLevel().dimension(), x -> new Object2ObjectOpenHashMap<>()).put(ssb, tickets);
            }
            if (!tickets.contains(ticket) && keep) {
                tickets.add(ticket);
            } else if  (tickets.contains(ticket) && !keep) {
                tickets.remove(ticket);
            }
        }
    }

    public static InteractionResult checkHarmony(final ServerSubLevel serverSubLevel, final ItemStack harmonizerStack) {
        final Map<ServerSubLevel, Set<HarmonizerRecordingTicket>> mapSubs = mapOfChildSublevelsToRootSublevel.get(serverSubLevel.getLevel().dimension());
        Set<HarmonizerRecordingTicket> tickets = mapSubs != null ? mapSubs.get(serverSubLevel) : null;
        if (tickets == null) return InteractionResult.FAIL;

        for (final HarmonizerRecordingTicket ticket : tickets) {
            if (!ticket.isValid()) continue;
            for (final ForcefieldHarmonizerBlockEntity blockEntity : ticket.blockEntities) {
                if (blockEntity.CHECK_HARMONY.test(harmonizerStack)) return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }


    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        energySlot.fillContainerOrConvert();
        if (canFunction()) {
            long energyPerTick = energyContainer.getEnergyPerTick();
            if (energyContainer.extract(energyPerTick, Action.SIMULATE, AutomationType.INTERNAL) == energyPerTick) {
                energyContainer.extract(energyPerTick, Action.EXECUTE, AutomationType.INTERNAL);
                setActive(true);
            } else {
                setActive(false);
            }
        } else {
            setActive(false);
        }
        return sendUpdatePacket;
    }

    @Override
    public void writeSustainedData(HolderLookup.Provider provider, CompoundTag dataMap) {
        super.writeSustainedData(provider, dataMap);
        filterManager.writeToNBT(provider, dataMap);
    }

    @Override
    public void readSustainedData(HolderLookup.Provider provider, @NotNull CompoundTag dataMap) {
        super.readSustainedData(provider, dataMap);
        filterManager.readFromNBT(provider, dataMap);
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        filterManager.addContainerTrackers(container);
    }

    @Override
    public SortableFilterManager<HarmonicResonanceFilter<?>> getFilterManager() {
        return filterManager;
    }

    @ComputerMethod
    Collection<HarmonicResonanceFilter<?>> getFilters() {
        return filterManager.getFilters();
    }

    @ComputerMethod(requiresPublicSecurity = true)
    boolean addFilter(HarmonicResonanceFilter<?> filter) throws ComputerException {
        validateSecurityIsPublic();
        return filterManager.addFilter(filter);
    }

    @ComputerMethod(requiresPublicSecurity = true)
    boolean removeFilter(HarmonicResonanceFilter<?> filter) throws ComputerException {
        validateSecurityIsPublic();
        return filterManager.removeFilter(filter);
    }

    record HarmonizerRecordingTicket(ServerSubLevel rootLevel, Set<ForcefieldHarmonizerBlockEntity> blockEntities) {

        public boolean isValid() {
            for (final ForcefieldHarmonizerBlockEntity be : this.blockEntities) {
                if (be.isRemoved() || be.getLevel() != this.rootLevel.getLevel()) {
                    return false;
                }
            }

            return this.rootLevel != null && !this.rootLevel.isRemoved();
        }
    }

}
