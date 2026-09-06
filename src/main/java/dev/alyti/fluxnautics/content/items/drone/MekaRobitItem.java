package dev.alyti.fluxnautics.content.items.drone;

import com.mojang.serialization.Codec;
import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.entities.gear.EntityMekaRobit;
import dev.alyti.fluxnautics.lang.FNLang;
import dev.alyti.fluxnautics.registry.FNDataComponents;
import dev.alyti.fluxnautics.registry.FNEntityTypes;
import dev.alyti.fluxnautics.util.FNModuleUtils;
import foundry.veil.mixin.registry.accessor.RegistryDataAccessor;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IIncrementalEnum;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleContainer;
import mekanism.api.gear.IModuleHelper;
import mekanism.api.radial.IRadialDataHelper;
import mekanism.api.radial.RadialData;
import mekanism.api.radial.mode.IRadialMode;
import mekanism.api.radial.mode.NestedRadialMode;
import mekanism.api.security.IItemSecurityUtils;
import mekanism.api.text.EnumColor;
import mekanism.api.text.IHasTextComponent;
import mekanism.api.text.ILangEntry;
import mekanism.client.key.MekKeyHandler;
import mekanism.client.key.MekanismKeyHandler;
import mekanism.common.MekanismLang;
import mekanism.common.attachments.FrequencyAware;
import mekanism.common.attachments.qio.PortableDashboardContents;
import mekanism.common.capabilities.ICapabilityAware;
import mekanism.common.capabilities.security.OwnerObject;
import mekanism.common.config.MekanismConfig;
import mekanism.common.content.gear.IRadialModuleContainerItem;
import mekanism.common.content.gear.ModuleHelper;
import mekanism.common.content.gear.mekatool.ModuleExcavationEscalationUnit;
import mekanism.common.content.qio.QIOFrequency;
import mekanism.common.inventory.container.item.PortableQIODashboardContainer;
import mekanism.common.item.ItemEnergized;
import mekanism.common.item.ItemPortableQIODashboard;
import mekanism.common.item.gear.ItemAtomicDisassembler;
import mekanism.common.item.interfaces.IColoredItem;
import mekanism.common.item.interfaces.IGuiItem;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.lib.frequency.IFrequencyItem;
import mekanism.common.lib.radial.data.NestingRadialData;
import mekanism.common.lib.security.ItemSecurityUtils;
import mekanism.common.network.to_client.qio.BulkQIOData;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.registration.impl.CreativeTabDeferredRegister;
import mekanism.common.registries.MekanismContainerTypes;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.registries.MekanismModules;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.StorageUtils;
import mekanism.common.util.WorldUtils;
import mekanism.common.util.text.BooleanStateDisplay;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class MekaRobitItem extends ItemPortableQIODashboard implements IRadialModuleContainerItem, IFrequencyItem, IGuiItem, IColoredItem, ICapabilityAware {

    private static final ResourceLocation RADIAL_ID = Fluxnautics.id("meka_robit");

    public MekaRobitItem(Properties properties) {
        super(properties);
    }

    public static Pair<Pair<Optional<DroneMode>, Optional<EntityMekaRobit>>, Pair<Optional<DroneMode>, Optional<EntityMekaRobit>>> getBothHandDroneModes(final Player player) {
        return Pair.of(
            getDroneWithMode(player.level(), player.getMainHandItem()),
            getDroneWithMode(player.level(), player.getOffhandItem())
        );
    }

    public static Pair<Optional<DroneMode>, Optional<EntityMekaRobit>> getDroneWithMode(Level level, ItemStack stack) {
        if (stack.getItem() instanceof MekaRobitItem item) {
            return Pair.of(
                getDroneMode(stack),
                getDrone(level, stack)
            );
        }
        return Pair.of(Optional.empty(), Optional.empty());
    }

    public static Optional<DroneMode> getDroneMode(ItemStack stack) {
        if (stack.getItem() instanceof MekaRobitItem item) {
            return Optional.ofNullable(item.getMode(stack, RADIAL_DRONE_MODE_DATA));
        }
        return Optional.empty();
    }

    public static Optional<EntityMekaRobit> getDrone(Level level, ItemStack stack) {
        if (stack.getItem() instanceof MekaRobitItem item) {
            UUID id = stack.get(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY.get());
            if (id == null)
                return Optional.empty();
            if (level.isClientSide) {
                for (final Entity entity : level.getEntitiesOfClass(EntityMekaRobit.class, Minecraft.getInstance().player.getBoundingBox().inflate(128))) {
                    if (entity instanceof final EntityMekaRobit robit && entity.getUUID().equals(id)) {
                        return Optional.of(robit);
                    }
                }
            }

            return Optional.ofNullable(EntityMekaRobit.findByUuid(level, id));
        }
        return Optional.empty();
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        //If we are currently stacked, don't display the bar as it will overlap the stack count
        return stack.getCount() == 1;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return StorageUtils.getEnergyBarWidth(stack);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return MekanismConfig.client.energyColor.get();
    }

    public void addItems(Holder<Item> item, Consumer<ItemStack> tabOutput, HolderLookup.Provider provider) {
        tabOutput.accept(StorageUtils.getFilledEnergyVariant(item));
        tabOutput.accept(FNModuleUtils.maxModules(item, provider));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        //Ignore NBT for energized items causing re-equip animations
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        //Ignore NBT for energized items causing block break reset
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void onDestroyed(@NotNull ItemEntity item, @NotNull DamageSource damageSource) {
        ModuleHelper.INSTANCE.dropModuleContainerContents(item, damageSource);
        InventoryUtils.dropItemContents(item, damageSource);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (MekKeyHandler.isKeyPressed(MekanismKeyHandler.detailsKey)) {
            addModuleDetails(stack, tooltip);
        } else {
            IItemSecurityUtils.INSTANCE.addSecurityTooltip(stack, tooltip);
            MekanismUtils.addFrequencyItemTooltip(stack, tooltip);
            tooltip.add(MekanismLang.HAS_INVENTORY.translateColored(EnumColor.AQUA, EnumColor.GRAY, BooleanStateDisplay.YesNo.hasInventory(stack)));
            StorageUtils.addStoredEnergy(stack, tooltip, true);
            tooltip.add(MekanismLang.HOLD_FOR_MODULES.translateColored(EnumColor.GRAY, EnumColor.INDIGO, MekanismKeyHandler.detailsKey.getTranslatedKeyMessage()));
        }

    }


    @Override
    public void encodeContainerData(RegistryFriendlyByteBuf buf, ItemStack stack) {
        FrequencyAware<QIOFrequency> frequencyAware = stack.get(getFrequencyComponent());
        BulkQIOData.encodeToPacket(buf, frequencyAware == null ? null : frequencyAware.getFrequency(stack, getFrequencyComponent()));
    }

    @Override
    public ContainerTypeRegistryObject<PortableQIODashboardContainer> getContainerType() {
        return MekanismContainerTypes.PORTABLE_QIO_DASHBOARD;
    }

    @Override
    public FrequencyType<?> getFrequencyType() {
        return FrequencyType.QIO;
    }

    @NotNull
    @Override
    public DataComponentType<FrequencyAware<QIOFrequency>> getFrequencyComponent() {
        return MekanismDataComponents.QIO_FREQUENCY.get();
    }

    public long tryMassInsertIntoQIO(ItemStack stack, ItemStack toInsert, long count) {
        FrequencyAware<QIOFrequency> frequencyAware = stack.get(getFrequencyComponent());
        if (frequencyAware == null)
            return 0;
        QIOFrequency freq = frequencyAware.getFrequency(stack, getFrequencyComponent());
        if (freq == null)
            return 0;
        return freq.massInsert(toInsert, count, Action.EXECUTE);
    }

    @Override
    public boolean canPerformAction(@NonNull ItemStack stack, @NonNull ItemAbility action) {
        IModuleContainer moduleContainer = moduleContainer(stack);
        if (moduleContainer != null) {
            for (IModule<?> module : moduleContainer.modules()) {
                if (module.isEnabled() && canPerformAction(module, moduleContainer, stack, action)) {
                    return true;
                }
            }
        }
        return false;
    }

    private <MODULE extends ICustomModule<MODULE>> boolean canPerformAction(IModule<MODULE> module, IModuleContainer moduleContainer, ItemStack stack, ItemAbility action) {
        return module.getCustomInstance().canPerformAction(module, moduleContainer, stack, action);
    }


    @Override
    public int getEnchantmentLevel(@NonNull ItemStack stack, @NonNull Holder<Enchantment> enchantment) {
        //Enchantments in our data
        IModuleContainer container = IModuleHelper.INSTANCE.getModuleContainer(stack);
        int moduleLevel = container == null ? 0 : container.getModuleEnchantmentLevel(enchantment);
        return Math.max(moduleLevel, super.getEnchantmentLevel(stack, enchantment));
    }

    @NotNull
    @Override
    public ItemEnchantments getAllEnchantments(@NotNull ItemStack stack, HolderLookup.@NonNull RegistryLookup<Enchantment> lookup) {
        ItemEnchantments enchantments = super.getAllEnchantments(stack, lookup);
        IModuleContainer container = IModuleHelper.INSTANCE.getModuleContainer(stack);
        if (container != null) {
            ItemEnchantments moduleEnchantments = container.moduleBasedEnchantments();
            if (enchantments.isEmpty()) {
                //Skip copying if there are no builtin enchantments
                return moduleEnchantments;
            } else if (!moduleEnchantments.isEmpty()) {
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchantments);
                for (Object2IntMap.Entry<Holder<Enchantment>> entry : moduleEnchantments.entrySet()) {
                    mutable.upgrade(entry.getKey(), entry.getIntValue());
                }
                return mutable.toImmutable();
            }
        }
        return enchantments;
    }

    @NotNull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        for (IModule<?> module : getModules(context.getItemInHand())) {
            if (module.isEnabled()) {
                InteractionResult result = onModuleUse(module, context);
                if (result != InteractionResult.PASS) return result;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public @NonNull InteractionResultHolder<ItemStack> use(@NonNull Level level, Player player, @NonNull InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return ItemSecurityUtils.get().claimOrOpenGui(level, player, hand, getContainerType()::tryOpenGui);
        }
        ItemStack stack = player.getItemInHand(hand);
        UUID boundId = stack.get(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY.get());

        if (boundId != null) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            Vec3 spawnPos = player.getEyePosition().add(player.getLookAngle().scale(1.0));
            InteractionResult result = spawnMekaRobit(level, player, stack, spawnPos, BlockPos.containing(spawnPos));
            return new InteractionResultHolder<>(result, stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private InteractionResult spawnMekaRobit(Level level, Player player, ItemStack stack,
                                             Vec3 clickLoc, BlockPos clickedPos) {
        EntityMekaRobit robit = FNEntityTypes.MEKA_ROBIT.get().create(level);
        if (robit == null) return InteractionResult.FAIL;

        double x = clickLoc == null ? clickedPos.getX() + 0.5 : clickLoc.x;
        double y = clickLoc == null ? clickedPos.getY() + 1.0 : clickLoc.y;
        double z = clickLoc == null ? clickedPos.getZ() + 0.5 : clickLoc.z;
        robit.moveTo(x, y, z, player.getYRot(), 0.0F);
        robit.setOwner(player);

        // Bind: marker on the stack = entity UUID. No shrink(1).
        stack.set(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY.get(), robit.getUUID());

        level.addFreshEntity(robit);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, robit.blockPosition());
        return InteractionResult.CONSUME;
    }

    private <MODULE extends ICustomModule<MODULE>> InteractionResult onModuleUse(IModule<MODULE> module, UseOnContext context) {
        return module.getCustomInstance().onItemUse(module, context);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public boolean isBookEnchantable(@NotNull ItemStack stack, @NotNull ItemStack book) {
        return isEnchantable(stack) && super.isBookEnchantable(stack, book);
    }

    @Override
    public boolean isPrimaryItemFor(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return isEnchantable(stack) && super.isPrimaryItemFor(stack, enchantment);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return isEnchantable(stack) && super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public ResourceLocation getRadialIdentifier() {
        return RADIAL_ID;
    }

    private static final RadialData<DroneMode> RADIAL_DRONE_MODE_DATA = IRadialDataHelper.INSTANCE.dataForEnum(Fluxnautics.id("drone_mode"), DroneMode.PATROL);
    private static final NestedRadialMode NESTED_RADIAL_DRONE_MODE = new NestedRadialMode(RADIAL_DRONE_MODE_DATA, FNLang.MEKA_ROBIT_MODE, ItemAtomicDisassembler.DisassemblerMode.VEIN.icon(), EnumColor.AQUA);

    public DataComponentType<DroneMode> getDroneModeComponent() {
        return FNDataComponents.DRONE_MODE.get();
    }

    @Nullable
    @Override
    public RadialData<?> getRadialData(ItemStack stack) {
        List<NestedRadialMode> nestedModes = new ArrayList<>();
        Consumer<NestedRadialMode> adder = nestedModes::add;
        for (IModule<?> module : getModules(stack)) {
            if (module.handlesRadialModeChange()) {
               addRadialModes(module, stack, adder);
            }
        }
        if (nestedModes.isEmpty()) {
            return NESTED_RADIAL_DRONE_MODE.nestedData();
        }
        nestedModes.add(NESTED_RADIAL_DRONE_MODE);
        return new NestingRadialData(getRadialIdentifier(), nestedModes);
    }

    private static <MODULE extends ICustomModule<MODULE>> void addRadialModes(IModule<MODULE> module, ItemStack stack, Consumer<NestedRadialMode> adder) {
        module.getCustomInstance().addRadialModes(module, stack, adder);
    }

    @Override
    public <M extends IRadialMode> void setMode(ItemStack stack, Player player, RadialData<M> radialData, M mode) {
        if (radialData == RADIAL_DRONE_MODE_DATA && mode instanceof DroneMode dm) {
            stack.set(getDroneModeComponent(), dm);
            return;
        }
        IRadialModuleContainerItem.super.setMode(stack, player, radialData, mode);
    }

    @Nullable
    @Override
    public <M extends IRadialMode> M getMode(ItemStack stack, RadialData<M> radialData) {
        if (radialData == RADIAL_DRONE_MODE_DATA) {
            return (M) stack.getOrDefault(getDroneModeComponent(), DroneMode.PATROL);
        }

        return IRadialModuleContainerItem.super.getMode(stack, radialData);
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
        IModuleContainer container = IModuleHelper.INSTANCE.getModuleContainer(stack);
        if  (container == null) {
            return super.getDestroySpeed(stack, state);
        }
        IEnergyContainer energyContainer = StorageUtils.getEnergyContainer(stack, 0);
        if (energyContainer == null) {
            return 0;
        }
        //Use raw hardness to get the best guess of if it is zero or not
        long energyRequired = EntityMekaRobit.getDestroyEnergy(container, state.destroySpeed, container.hasEnabled(MekanismModules.SILK_TOUCH_UNIT));
        long energyAvailable = energyContainer.extract(energyRequired, Action.SIMULATE, AutomationType.MANUAL);
        if (energyAvailable < energyRequired) {
            //If we can't extract all the energy we need to break it go at base speed reduced by how much we actually have available
            return (float) (MekanismConfig.gear.mekaToolBaseEfficiency.get() * ((double) energyAvailable / energyRequired));
        }
        IModule<ModuleExcavationEscalationUnit> module = container.getIfEnabled(MekanismModules.EXCAVATION_ESCALATION_UNIT);
        return module == null ? MekanismConfig.gear.mekaToolBaseEfficiency.get() : module.getCustomInstance().getEfficiency();
    }


    @Override
    public boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int inventorySlot) {
        //Try to avoid replacing this item if there are any modules currently installed
        return super.isNotReplaceableByPickAction(stack, player, inventorySlot) || hasInstalledModules(stack);
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return true;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide)
            return;

        if (!isSelected)
            return;

        if (level.getGameTime() % (5 * SharedConstants.TICKS_PER_SECOND) == 0)
            syncColorWithFrequency(stack);

        UUID boundId = stack.get(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY.get());
        if (boundId == null)
            return;

        EntityMekaRobit robit = EntityMekaRobit.findByUuid(level, boundId);
        if (robit == null)
            stack.remove(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY.get());

    }

    @Override
    public void attachCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(IItemSecurityUtils.INSTANCE.ownerCapability(), (stack, ctx) -> new OwnerObject(stack), this);
    }

    @NothingNullByDefault
    public enum DroneMode implements IIncrementalEnum<DroneMode>, IHasTextComponent.IHasEnumNameTextComponent, IRadialMode, StringRepresentable {
        PATROL(FNLang.MEKA_ROBIT_MODE_PATROL, EnumColor.DARK_RED, MekanismUtils.getResource(MekanismUtils.ResourceType.GUI_RADIAL, "empty.png")),
        ASSIST(FNLang.MEKA_ROBIT_MODE_ASSIST, EnumColor.AQUA, MekanismUtils.getResource(MekanismUtils.ResourceType.GUI_RADIAL, "empty.png")),
        DELETE(FNLang.MEKA_ROBIT_MODE_DELETE, EnumColor.RED, MekanismUtils.getResource(MekanismUtils.ResourceType.GUI_RADIAL, "empty.png")),
        MANUAL(FNLang.MEKA_ROBIT_MODE_MANUAL, EnumColor.BRIGHT_GREEN, MekanismUtils.getResource(MekanismUtils.ResourceType.GUI_RADIAL, "empty.png"));

        public static final Codec<DroneMode> CODEC = StringRepresentable.fromEnum(DroneMode::values);
        public static final IntFunction<DroneMode> BY_ID = ByIdMap.continuous(DroneMode::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        public static final StreamCodec<ByteBuf, DroneMode> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, DroneMode::ordinal);

        private final String serializedName;
        private final ILangEntry langEntry;
        private final EnumColor color;
        private final ResourceLocation icon;

        DroneMode(ILangEntry langEntry, EnumColor color, ResourceLocation icon) {
            this.serializedName = name().toLowerCase(Locale.ROOT);
            this.langEntry = langEntry;
            this.color = color;
            this.icon = icon;
        }

        @Override
        public Component getTextComponent() {
            return langEntry.translateColored(color);
        }

        @Override
        public EnumColor color() {
            return color;
        }

        @Override
        public DroneMode byIndex(int index) {
            return BY_ID.apply(index);
        }

        @Override
        public Component sliceName() {
            return getTextComponent();
        }

        @Override
        public ResourceLocation icon() {
            return icon;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
