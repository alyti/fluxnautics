package dev.alyti.fluxnautics.content.items.drone;

import com.mojang.serialization.Codec;
import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.entities.gear.EntityMekaRobit;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.IntFunction;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.annotations.ParametersAreNotNullByDefault;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleContainer;
import mekanism.api.math.MathUtils;
import mekanism.api.text.IHasTextComponent;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.MekanismLang;
import mekanism.common.config.MekanismConfig;
import mekanism.common.network.to_client.PacketLightningRender;
import mekanism.common.network.to_client.PacketLightningRender.LightningPreset;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

@ParametersAreNotNullByDefault
public record ModuleQuantumPullUnit(Range range) implements ICustomModule<ModuleQuantumPullUnit> {

    public static final ResourceLocation RANGE = Fluxnautics.id("range");

    public ModuleQuantumPullUnit(IModule<ModuleQuantumPullUnit> module) {
        this(module.<Range>getConfigOrThrow(RANGE).get());
    }

    public void tickRobit(EntityMekaRobit drone, IModule<ModuleQuantumPullUnit> module, ItemStack stack) {
        if (range == Range.OFF) return;

        Player owner = drone.getOwner();
        Vec3 targetPos = null;

        if (owner != null) {
            targetPos = owner.position().add(0, 0.5, 0);
        } else if (drone.isBoundToBlock()) {
            Optional<GlobalPos> home = drone.getHome();
            if (home.isPresent())
                targetPos = home.map(x->Vec3.atCenterOf(x.pos())).orElse(drone.position());
        }

        if (targetPos == null)
            targetPos = drone.position();

        float size = 4 + range.getRange();
        long usage = MathUtils.ceilToLong(MekanismConfig.gear.mekaSuitEnergyUsageItemAttraction.get() * range.getRange());
        boolean free = usage == 0L || (owner != null && owner.isCreative());
        IEnergyContainer energyContainer = free ? null : module.getEnergyContainer(stack);

        if (!free && (energyContainer == null || energyContainer.getEnergy() < usage)) {
            return;
        }

        List<ItemEntity> items = drone.level().getEntitiesOfClass(ItemEntity.class, drone.getBoundingBox().inflate(size), item -> !item.hasPickUpDelay());
        for (ItemEntity item : items) {
            if (item.distanceTo(drone) > 0.001) {
                if (!free) {
                    if (module.useEnergy(owner, energyContainer, usage, true) == 0L)
                        break;
                }

                ItemStack itemStack = item.getItem();
                boolean removedEntity = false;

                if (stack.getItem() instanceof MekaRobitItem mekaRobitItem) {
                    long inserted = mekaRobitItem.tryMassInsertIntoQIO(stack, itemStack, itemStack.getCount());
                    if (inserted > 0) {
                        itemStack.shrink((int) inserted);

                        PacketDistributor.sendToPlayersTrackingEntityAndSelf(drone, new PacketLightningRender(LightningPreset.MAGNETIC_ATTRACTION, 31 * drone.getUUID().hashCode() + item.hashCode(),
                                drone.position().add(0, 0.2, 0), item.position(), (int) (drone.distanceTo(item) * 4)));

                        if (itemStack.isEmpty()) {
                            item.discard();
                            removedEntity = true;
                        }
                    }
                }

                if (!removedEntity) {
                    Vec3 diff = targetPos.subtract(item.position());
                    Vec3 motionNeeded = new Vec3(Math.min(diff.x, 1), Math.min(diff.y, 1), Math.min(diff.z, 1));
                    Vec3 motionDiff = motionNeeded.subtract(item.getDeltaMovement());
                    item.setDeltaMovement(motionDiff.scale(0.2));
                    
                    // Lightning effect to the target
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(drone, new PacketLightningRender(LightningPreset.MAGNETIC_ATTRACTION, 31 * drone.getUUID().hashCode() + item.hashCode(),
                            targetPos.add(0, 0.2, 0), item.position(), (int) (diff.length() * 4)));
                }

                if (!free && energyContainer.getEnergy() < usage) {
                    break;
                }
            }
        }
    }

    @Override
    public boolean canChangeModeWhenDisabled(IModule<ModuleQuantumPullUnit> module) {
        return true;
    }

    @Override
    public void changeMode(IModule<ModuleQuantumPullUnit> module, Player player, IModuleContainer moduleContainer, ItemStack stack, int shift, boolean displayChangeMessage) {
        module.toggleEnabled(moduleContainer, stack, player, MekanismLang.MODULE_MAGNETIC_ATTRACTION.translate());
    }

    @NothingNullByDefault
    public enum Range implements IHasTextComponent, StringRepresentable {
        OFF(0),
        LOW(1F),
        MED(3F),
        HIGH(5),
        ULTRA(10);

        public static final Codec<Range> CODEC = StringRepresentable.fromEnum(Range::values);
        public static final IntFunction<Range> BY_ID = ByIdMap.continuous(Range::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        public static final StreamCodec<ByteBuf, Range> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Range::ordinal);

        private final String serializedName;
        private final float range;
        private final Component label;

        Range(float boost) {
            this.serializedName = name().toLowerCase(Locale.ROOT);
            this.range = boost;
            this.label = TextComponentUtil.getString(Float.toString(boost));
        }

        @Override
        public Component getTextComponent() {
            return label;
        }

        public float getRange() {
            return range;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}