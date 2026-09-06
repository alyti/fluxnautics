package dev.alyti.fluxnautics.content.items.drone;

import com.mojang.serialization.Codec;
import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.entities.gear.EntityMekaRobit;
import dev.alyti.fluxnautics.lang.FNLang;
import dev.alyti.fluxnautics.registry.FNEntityTypes;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import foundry.veil.api.network.VeilPacketManager;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import mekanism.api.IIncrementalEnum;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.annotations.ParametersAreNotNullByDefault;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleContainer;
import mekanism.api.radial.IRadialDataHelper;
import mekanism.api.radial.RadialData;
import mekanism.api.radial.mode.IRadialMode;
import mekanism.api.radial.mode.NestedRadialMode;
import mekanism.api.text.EnumColor;
import mekanism.api.text.IHasTextComponent;
import mekanism.api.text.ILangEntry;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.MekanismLang;
import mekanism.common.network.to_client.PacketLightningRender;
import mekanism.common.util.MekanismUtils;
import net.minecraft.Util;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.TranslatableEnum;
import net.neoforged.neoforge.common.util.Lazy;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

@ParametersAreNotNullByDefault
public record ModuleSwarmUnit(SwarmMode swarmMode, SwarmFormation swarmFormation) implements ICustomModule<ModuleSwarmUnit> {

    public static final ResourceLocation SWARM_MODE = Fluxnautics.id("swarm_mode");
    public static final ResourceLocation SWARM_FORMATION = Fluxnautics.id("swarm_formation");
    private static final ResourceLocation RADIAL_ID = Fluxnautics.id("swarm");

    private static final WeakHashMap<Player, SwarmState> playerStates = new WeakHashMap<>();

    private static final Int2ObjectMap<Lazy<NestedRadialMode>> RADIAL_DATAS_MODE = Util.make(() -> {
        int types = ModuleSwarmUnit.SwarmMode.values().length - 1;
        Int2ObjectMap<Lazy<NestedRadialMode>> map = new Int2ObjectArrayMap<>(types);
        for (int type = 1; type <= types; type++) {
            int accessibleValues = type + 1;
            map.put(type, Lazy.of(() -> new NestedRadialMode(IRadialDataHelper.INSTANCE.dataForTruncated(RADIAL_ID, accessibleValues, SwarmMode.values()[accessibleValues-1]),
                FNLang.RADIAL_SWARM, SwarmMode.SIX.icon(), EnumColor.YELLOW)));
        }
        return map;
    });

    private static final NestedRadialMode RADIAL_DATA_FORMATION = new NestedRadialMode(IRadialDataHelper.INSTANCE.dataForTruncated(SWARM_FORMATION, SwarmFormation.values().length, SwarmFormation.ROUND),
                FNLang.RADIAL_SWARM_FORMATION, SwarmFormation.RING.icon(), EnumColor.AQUA);

    public ModuleSwarmUnit(IModule<ModuleSwarmUnit> module) {
        this(module.<ModuleSwarmUnit.SwarmMode>getConfigOrThrow(SWARM_MODE).get(), module.<ModuleSwarmUnit.SwarmFormation>getConfigOrThrow(SWARM_FORMATION).get());
    }

    private NestedRadialMode getNestedData(IModule<ModuleSwarmUnit> module, ResourceLocation id) {
        if (id == SWARM_FORMATION)
            return RADIAL_DATA_FORMATION;
        return RADIAL_DATAS_MODE.get(module.getInstalledCount()).get();
    }

    private @NonNull RadialData<?> getRadialData(IModule<ModuleSwarmUnit> module, ResourceLocation id) {
        return getNestedData(module, id).nestedData();
    }

    @Override
    public void addRadialModes(IModule<ModuleSwarmUnit> module, ItemStack stack, Consumer<NestedRadialMode> adder) {
        adder.accept(getNestedData(module, SWARM_MODE));
        adder.accept(getNestedData(module, SWARM_FORMATION));
    }

    @Nullable
    @Override
    public <MODE extends IRadialMode> MODE getMode(IModule<ModuleSwarmUnit> module, ItemStack stack, RadialData<MODE> radialData) {
        if (radialData == getRadialData(module, SWARM_MODE)) {
            return (MODE) swarmMode;
        } else if (radialData == getRadialData(module, SWARM_FORMATION)) {
            return (MODE) swarmFormation;
        }
        return null;
    }

    @Override
    public <MODE extends IRadialMode> boolean setMode(IModule<ModuleSwarmUnit> module, Player player, IModuleContainer moduleContainer, ItemStack stack, RadialData<MODE> radialData, MODE mode) {
        if (radialData == getRadialData(module, SWARM_MODE)) {
            ModuleSwarmUnit.SwarmMode newMode = (ModuleSwarmUnit.SwarmMode) mode;
            if (swarmMode != newMode) {
                moduleContainer.replaceModuleConfig(player.registryAccess(), stack, module.getDataHolder(), module.<ModuleSwarmUnit.SwarmMode>getConfigOrThrow(SWARM_MODE).with(newMode));
            }
        } else if (radialData == getRadialData(module, SWARM_FORMATION)) {
            ModuleSwarmUnit.SwarmFormation newMode = (ModuleSwarmUnit.SwarmFormation) mode;
            if (swarmFormation != newMode) {
                moduleContainer.replaceModuleConfig(player.registryAccess(), stack, module.getDataHolder(), module.<ModuleSwarmUnit.SwarmFormation>getConfigOrThrow(SWARM_FORMATION).with(newMode));
            }
        }
        return false;
    }

    @Override
    public Component getModeScrollComponent(IModule<ModuleSwarmUnit> module, ItemStack stack) {
        return MekanismLang.GENERIC_WITH_PARENTHESIS.translateColored(EnumColor.INDIGO, swarmMode.sliceName(), EnumColor.AQUA, swarmMode.getDrones());
    }

    @Override
    public void changeMode(IModule<ModuleSwarmUnit> module, Player player, IModuleContainer moduleContainer, ItemStack stack, int shift, boolean displayChangeMessage) {
        ModuleSwarmUnit.SwarmMode newMode = swarmMode.adjust(shift, v -> v.ordinal() < module.getInstalledCount() + 1);
        if (swarmMode != newMode) {
            if (displayChangeMessage) {
                module.displayModeChange(player, FNLang.RADIAL_SWARM_COUNT.translate(), newMode);
            }
            moduleContainer.replaceModuleConfig(player.registryAccess(), stack, module.getDataHolder(), module.<ModuleSwarmUnit.SwarmMode>getConfigOrThrow(SWARM_MODE).with(newMode));
        }
    }

    @Override
    public void addHUDStrings(IModule<ModuleSwarmUnit> module, IModuleContainer moduleContainer, ItemStack stack, Player player, Consumer<Component> hudStringAdder) {
        if (module.isEnabled()) {
            hudStringAdder.accept(FNLang.RADIAL_SWARM_COUNT.translateColored(EnumColor.DARK_GRAY, EnumColor.INDIGO, getDrones()));
        }
    }

    @Override
    public void tickServer(IModule<ModuleSwarmUnit> module, IModuleContainer moduleContainer, ItemStack stack, Player player) {
        if (stack.getItem() instanceof MekaRobitItem)
            return;
        if (!module.hasEnoughEnergy(stack, 1000L))
            return;

        SwarmState state = playerStates.computeIfAbsent(player, p -> new SwarmState());
        int targetMinions = module.isEnabled() ? getDrones() : 0;
        manageSwarm(player, targetMinions, state);
    }

    public static void manageSwarm(LivingEntity leader, int targetMinions, SwarmState state) {
        if (state.cacheUpdateTimer <= 0) {
            state.cacheUpdateTimer = 20;
            state.cachedSwarm.clear();
            leader.level().getEntitiesOfClass(EntityMekaRobit.class, leader.getBoundingBox().inflate(64)).forEach(e -> {
                if (e.isSwarmMinion() && e.mainDroneUuid != null && e.mainDroneUuid.equals(leader.getUUID())) {
                    state.cachedSwarm.add(e);
                }
            });
        } else {
            state.cacheUpdateTimer--;
        }

        state.cachedSwarm.removeIf(e -> !e.isAlive() || e.isRemoved());

        List<EntityMekaRobit> minions = state.cachedSwarm;

        if (targetMinions == 0) {
            minions.forEach(Entity::discard);
            return;
        }

        int leaderId = leader instanceof EntityMekaRobit robit ? robit.getId() : -1;
        Optional<GlobalPos> block = leader instanceof EntityMekaRobit robit ? robit.getHome() : Optional.empty();

        for (EntityMekaRobit minion : minions) {
            minion.setMainDroneId(leaderId);
            minion.setFormationSize(minions.size());
        }

        if (minions.size() < targetMinions) {
            Set<Integer> activeIndices = minions.stream()
                .map(EntityMekaRobit::getFormationIndex)
                .collect(Collectors.toSet());

            int nextIndex = 0;
            while (activeIndices.contains(nextIndex)) {
                nextIndex++;
            }
            EntityMekaRobit newMinion = spawnSwarmMinion(leader, nextIndex, leader.getUUID(), leaderId);
            block.ifPresent(newMinion::setHome);
            state.cachedSwarm.add(newMinion);
            return;
        }

        if (minions.size() > targetMinions) {
            minions.stream()
                .max(Comparator.comparingInt(EntityMekaRobit::getFormationIndex))
                .ifPresent(Entity::discard);
            return;
        }

        minions.sort(Comparator.comparingInt(EntityMekaRobit::getFormationIndex));
        for (int i = 0; i < minions.size(); i++) {
            if (minions.get(i).getFormationIndex() != i) {
                minions.get(i).setFormationIndex(i);
            }
        }

        boolean isIdle = leader.getDeltaMovement().lengthSqr() < 0.0025D;

        if (isIdle) {
            if (state.swapTimer > 0) {
                state.swapTimer--;
            } else {
                RandomSource random = leader.level().getRandom();
                state.swapTimer = 100 + random.nextInt(100);

                if (minions.size() >= 3) {
                    int idx1 = random.nextInt(minions.size());
                    int idx2 = random.nextInt(minions.size());
                    if (idx1 != idx2) {
                        EntityMekaRobit m1 = minions.get(idx1);
                        EntityMekaRobit m2 = minions.get(idx2);

                        int temp = m1.getFormationIndex();
                        m1.setFormationIndex(m2.getFormationIndex());
                        m2.setFormationIndex(temp);
                    }
                }
            }
        } else {
            state.swapTimer = 0;
        }
    }

    private static EntityMekaRobit spawnSwarmMinion(LivingEntity leader, int assignedIndex, UUID mainUuid, int leaderId) {
        EntityMekaRobit minion = FNEntityTypes.MEKA_ROBIT.get().create(leader.level());
        if (minion != null) {
            Vec3 basePos = leader.getEyePosition();
            Vec3 dest = minion.getRandomPositionAround(basePos, 3.0f, 6.0f, 10);
            if (dest == null) dest = leader.position();

            minion.setSwarmMinion(true);
            minion.mainDroneUuid = mainUuid;
            minion.setMainDroneId(leaderId);

            if (leader instanceof Player player) {
                minion.setOwner(player);
            } else if (leader instanceof EntityMekaRobit robit) {
                minion.setOwner(robit.getOwner());
            }

            minion.setFormationIndex(assignedIndex);
            minion.setPos(dest.x, dest.y, dest.z);
            leader.level().addFreshEntity(minion);

            VeilPacketManager.tracking(minion).sendPacket(new PacketLightningRender(
                PacketLightningRender.LightningPreset.TOOL_AOE, 31 * minion.hashCode(), basePos, dest, 10)
            );
        }
        return minion;
    }

    public static void forEachSwarmMember(LivingEntity leader, SwarmState state, Consumer<EntityMekaRobit> action) {
        for (EntityMekaRobit minion : state.cachedSwarm) {
            if (minion.isAlive() && !minion.isRemoved()) {
                action.accept(minion);
            }
        }
    }

    public int getDrones() {
        return swarmMode.getDrones();
    }

    public static class SwarmState {
        public int swapTimer = 0;

        public int cacheUpdateTimer = 0;
        public final List<EntityMekaRobit> cachedSwarm = new ArrayList<>();
    }

    @NothingNullByDefault
    public enum SwarmMode implements IIncrementalEnum<ModuleSwarmUnit.SwarmMode>, IHasTextComponent, TranslatableEnum, IRadialMode, StringRepresentable {
        OFF(FNLang.RADIAL_SWARM_OFF, 0, EnumColor.WHITE, "swarm_off"),
        TWO(FNLang.RADIAL_SWARM_TWO, 2, EnumColor.PINK, "swarm_two"),
        FOUR(FNLang.RADIAL_SWARM_FOUR, 4, EnumColor.BRIGHT_GREEN, "swarm_four"),
        SIX(FNLang.RADIAL_SWARM_SIX, 6, EnumColor.YELLOW, "swarm_six"),
        EIGHT(FNLang.RADIAL_SWARM_EIGHT, 8, EnumColor.YELLOW, "swarm_eight"),
        TEN(FNLang.RADIAL_SWARM_TEN, 10, EnumColor.RED, "swarm_ten"),
        TWELVE(FNLang.RADIAL_SWARM_TWELVE, 12, EnumColor.RED, "swarm_twelve"),
        FOURTEEN(FNLang.RADIAL_SWARM_FOURTEEN, 14, EnumColor.AQUA, "swarm_twelve"),
        SIXTEEN(FNLang.RADIAL_SWARM_SIXTEEN, 16, EnumColor.AQUA, "swarm_twelve"),
        ;

        public static final Codec<ModuleSwarmUnit.SwarmMode> CODEC = StringRepresentable.fromEnum(ModuleSwarmUnit.SwarmMode::values);
        public static final IntFunction<ModuleSwarmUnit.SwarmMode> BY_ID = ByIdMap.continuous(ModuleSwarmUnit.SwarmMode::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        public static final StreamCodec<ByteBuf, ModuleSwarmUnit.SwarmMode> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ModuleSwarmUnit.SwarmMode::ordinal);

        private final String serializedName;
        private final ResourceLocation icon;
        private final ILangEntry langEntry;
        private final Component label;
        private final EnumColor color;
        private final int drones;

        SwarmMode(ILangEntry langEntry, int drones, EnumColor color, String texture) {
            this.serializedName = name().toLowerCase(Locale.ROOT);
            this.langEntry = langEntry;
            this.drones = drones;
            this.color = color;
            this.icon = Fluxnautics.id(MekanismUtils.ResourceType.GUI_RADIAL.getPrefix() + texture + ".png");
            this.label = TextComponentUtil.getString(Integer.toString(drones));
        }

        @Override
        public ModuleSwarmUnit.SwarmMode byIndex(int index) {
            return BY_ID.apply(index);
        }

        @Override
        public Component getTextComponent() {
            return label;
        }

        @Override
        public Component getTranslatedName() {
            return sliceName();
        }

        public int getDrones() {
            return drones;
        }

        @Override
        public Component sliceName() {
            return langEntry.translateColored(color);
        }

        @Override
        public ResourceLocation icon() {
            return icon;
        }

        @Override
        public EnumColor color() {
            return color;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }


    @NothingNullByDefault
    public enum SwarmFormation implements IIncrementalEnum<ModuleSwarmUnit.SwarmFormation>, IHasTextComponent, TranslatableEnum, IRadialMode, StringRepresentable {
        ROUND(FNLang.RADIAL_SWARM_FORMATION_CIRCLE_V, EnumColor.WHITE, "swarm_off") {
            @Override
            public Pose3dc getFormationPose(LivingEntity leader, EntityMekaRobit self, int total, int index, boolean isIdle, boolean isAssist, int tickCount, Pose3d dest) {
                if (total <= 0) total = 1;

                Vec3 forward = leader.getLookAngle();
                Vec3 worldUp = new Vec3(0, 1, 0);
                if (Math.abs(forward.y) > 0.99) {
                    worldUp = new Vec3(0, 0, 1);
                }

                Vec3 right = forward.cross(worldUp).normalize();
                Vec3 up = right.cross(forward).normalize();

                double radius = 0.5;
                double perRadius = 0.15;
                if (isAssist) {
                    radius *= 2D;
                    perRadius *= 1.25D;
                }

                double baseRadius = radius + (total * perRadius);
                double angle = (Math.PI * 2 / total) * index;
                double forwardOffset = 0.5D;
                double upOffset = 0.0D;

                if (isAssist) {
                    if (leader.onGround()) {
                        if (total > 1) {
                            angle = (Math.PI * index) / (total - 1);
                        } else {
                            angle = Math.PI / 2;
                        }
                        forwardOffset = -baseRadius * 0.5D;
                        upOffset = baseRadius * 0.5D;
                    } else {
                        upOffset = 0.5D;
                    }
                }

                double currentRadius = baseRadius;
                if (isIdle) {
                    double orbitRad = (tickCount * 0.05D);
                    currentRadius = baseRadius + Math.sin(orbitRad * 1.5D) * 0.25D;
                }

                Vec3 targetPos = leader.position()
                    .add(0, upOffset, 0)
                    .add(forward.scale(forwardOffset))
                    .add(right.scale(Math.cos(angle) * currentRadius))
                    .add(up.scale(Math.sin(angle) * currentRadius));

                Vec3 leaderEyePos = leader.getEyePosition();
                Vec3 lookTarget = leaderEyePos.add(forward.scale(20));
                JOMLConversion.toJOML(targetPos, dest.position());
                JOMLConversion.toJOML(lookTarget, dest.rotationPoint());

                return dest;
            }
        },
        RING(FNLang.RADIAL_SWARM_FORMATION_CIRCLE_H, EnumColor.AQUA, "swarm_eight") {
            @Override
            public Pose3dc getFormationPose(LivingEntity leader, EntityMekaRobit self, int total, int index, boolean isIdle, boolean isAssist, int tickCount, Pose3d dest) {
                if (total <= 0) total = 1;

                Vec3 forward = leader.getLookAngle();
                Vec3 worldUp = new Vec3(0, 1, 0);
                if (Math.abs(forward.y) > 0.99) {
                    worldUp = new Vec3(0, 0, 1);
                }
                Vec3 right = forward.cross(worldUp).normalize();
                Vec3 up = right.cross(forward).normalize();

                double baseRadius = 2.0D + (total * 0.2D);
                double forwardOffset = 0.0D;
                double upOffset = -0.5D;

                if (isAssist) {
                    baseRadius *= 2D;

                    if (leader.onGround()) {
                        double angle = (total > 1) ? (Math.PI * index) / (total - 1) : Math.PI / 2;
                        forwardOffset = -baseRadius * 0.5D;
                        upOffset = baseRadius * 0.5D;

                        Vec3 targetPos = leader.position()
                            .add(forward.scale(forwardOffset))
                            .add(right.scale(Math.cos(angle) * baseRadius))
                            .add(up.scale(Math.sin(angle) * baseRadius + upOffset));

                        Vec3 leaderEyePos = leader.getEyePosition();
                        Vec3 lookTarget = leaderEyePos.add(forward.scale(20));
                        JOMLConversion.toJOML(targetPos, dest.position());
                        JOMLConversion.toJOML(lookTarget, dest.rotationPoint());

                        return dest;
                    } else {
                        upOffset = 0.5D;
                    }
                }

                double angle = ((Math.PI * 2 / total) * index) + (tickCount * 0.03D);

                double rightOffset = Math.cos(angle) * baseRadius;
                double forwardOffsetFlat = Math.sin(angle) * baseRadius;

                Vec3 targetPos = leader.position()
                    .add(forward.scale(forwardOffsetFlat))
                    .add(right.scale(rightOffset))
                    .add(up.scale(upOffset));

                Vec3 leaderEyePos = leader.getEyePosition();
                Vec3 lookTarget = leaderEyePos.add(forward.scale(20));
                JOMLConversion.toJOML(targetPos, dest.position());
                JOMLConversion.toJOML(lookTarget, dest.rotationPoint());

                return dest;
            }
        },
        SPHERE(FNLang.RADIAL_SWARM_FORMATION_SPHERE, EnumColor.PINK, "swarm_two") {
            @Override
            public Pose3dc getFormationPose(LivingEntity leader, EntityMekaRobit self, int total, int index, boolean isIdle, boolean isAssist, int tickCount, Pose3d dest) {
                if (total <= 0) total = 1;

                Vec3 forward = leader.getLookAngle();
                Vec3 worldUp = new Vec3(0, 1, 0);
                if (Math.abs(forward.y) > 0.99) {
                    worldUp = new Vec3(0, 0, 1);
                }
                Vec3 right = forward.cross(worldUp).normalize();
                Vec3 up = right.cross(forward).normalize();

                double baseRadius = 2.0D + (total * 0.1D);
                double upShift = 0.0D;

                if (isAssist) {
                    baseRadius *= 2D;

                    if (leader.onGround()) {
                        upShift = baseRadius * 0.6D;
                    }
                }

                if (isIdle) {
                    baseRadius += Math.sin(tickCount * 0.05D) * 0.2D;
                }

                double phi = Math.PI * (3.0 - Math.sqrt(5.0)); // Golden angle

                double yScale = (isAssist && leader.onGround()) ? 1.0 : 2.0;
                double y = 1.0 - ((double) index / Math.max(1, total - 1)) * yScale;

                double radiusAtY = Math.sqrt(Math.max(0, 1 - y * y));
                double theta = phi * index;

                double x = Math.cos(theta) * radiusAtY;
                double z = Math.sin(theta) * radiusAtY;

                Vec3 targetPos = leader.position()
                    .add(forward.scale(z * baseRadius))
                    .add(right.scale(x * baseRadius))
                    .add(up.scale((y * baseRadius) + upShift));

                Vec3 leaderEyePos = leader.getEyePosition();
                Vec3 lookTarget = leaderEyePos.add(forward.scale(20));
                JOMLConversion.toJOML(targetPos, dest.position());
                JOMLConversion.toJOML(lookTarget, dest.rotationPoint());

                return dest;
            }
        },
        ;

        public static final Codec<ModuleSwarmUnit.SwarmFormation> CODEC = StringRepresentable.fromEnum(ModuleSwarmUnit.SwarmFormation::values);
        public static final IntFunction<ModuleSwarmUnit.SwarmFormation> BY_ID = ByIdMap.continuous(ModuleSwarmUnit.SwarmFormation::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        public static final StreamCodec<ByteBuf, ModuleSwarmUnit.SwarmFormation> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ModuleSwarmUnit.SwarmFormation::ordinal);

        private final String serializedName;
        private final ResourceLocation icon;
        private final ILangEntry langEntry;
        private final Component label;
        private final EnumColor color;

        SwarmFormation(ILangEntry langEntry,EnumColor color, String texture) {
            this.serializedName = name().toLowerCase(Locale.ROOT);
            this.langEntry = langEntry;
            this.color = color;
            this.icon = Fluxnautics.id(MekanismUtils.ResourceType.GUI_RADIAL.getPrefix() + texture + ".png");
            this.label = TextComponentUtil.getString(langEntry.toString());
        }

        @Override
        public ModuleSwarmUnit.SwarmFormation byIndex(int index) {
            return BY_ID.apply(index);
        }

        @Override
        public Component getTextComponent() {
            return label;
        }

        @Override
        public Component getTranslatedName() {
            return sliceName();
        }

        @Override
        public Component sliceName() {
            return langEntry.translateColored(color);
        }

        @Override
        public ResourceLocation icon() {
            return icon;
        }

        @Override
        public EnumColor color() {
            return color;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public abstract Pose3dc getFormationPose(LivingEntity leader, EntityMekaRobit self, int total, int index, boolean isIdle, boolean isAssist, int tickCount, Pose3d dest);
    }
}