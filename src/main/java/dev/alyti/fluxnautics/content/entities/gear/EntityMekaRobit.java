package dev.alyti.fluxnautics.content.entities.gear;

import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.content.items.drone.MekaRobitItem;
import dev.alyti.fluxnautics.content.items.drone.ModuleQuantumPullUnit;
import dev.alyti.fluxnautics.content.items.drone.ModuleSwarmUnit;
import dev.alyti.fluxnautics.math.LerpedFloat;
import dev.alyti.fluxnautics.registry.FNDamageTypes;
import dev.alyti.fluxnautics.registry.FNDataComponents;
import dev.alyti.fluxnautics.registry.FNModules;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;
import mekanism.api.*;
import mekanism.api.datamaps.IMekanismDataMapTypes;
import mekanism.api.datamaps.MekaSuitAbsorption;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleContainer;
import mekanism.api.gear.IModuleHelper;
import mekanism.api.lasers.ILaserDissipation;
import mekanism.api.math.MathUtils;
import mekanism.common.Mekanism;
import mekanism.common.capabilities.laser.item.LaserDissipationHandler;
import mekanism.common.config.MekanismConfig;
import mekanism.common.content.gear.IBlastingItem;
import mekanism.common.content.gear.IRadialModuleContainerItem;
import mekanism.common.content.gear.mekatool.ModuleAttackAmplificationUnit;
import mekanism.common.content.gear.mekatool.ModuleBlastingUnit;
import mekanism.common.content.gear.mekatool.ModuleExcavationEscalationUnit;
import mekanism.common.content.gear.mekatool.ModuleVeinMiningUnit;
import mekanism.common.registries.MekanismDataSerializers;
import mekanism.common.registries.MekanismModules;
import mekanism.common.tags.MekanismTags;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.NBTUtils;
import mekanism.common.util.StorageUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

public class EntityMekaRobit extends Mob implements OwnableEntity, IMekanismStrictEnergyHandler {
    public static final ModelProperty<ResourceLocation> SKIN_TEXTURE_PROPERTY = new ModelProperty<>();

    public interface IMekaRobitDock {
        ItemStack getDroneStack();
        PatrolConfig getPatrolConfig();
        GlobalPos getWanderCenter();
        boolean isEnemy(LivingEntity entity);
    }

    public record PatrolConfig(boolean isPoints, List<BlockPos> points, int range, int waitTicks) {}

    private static final EntityDataAccessor<UUID> OWNER_UUID = define(MekanismDataSerializers.UUID.value());
    private static final EntityDataAccessor<String> OWNER_NAME = define(EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> DATA_MODE = define(EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> HOVER_LEFT = define(EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ALERTED = define(EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_SWARM_MINION = define(EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MAIN_DRONE_ID = define(EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FORMATION_INDEX = define(EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FORMATION_SIZE = define(EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_FOCUS_FIRING = define(EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> IDLE_COLOR = define(EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACK_COLOR = define(EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RAINBOW_MODE = define(EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> TARGET_ID = define(EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> COMBAT_STATE = define(EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> LASER_TARGET_X = define(EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LASER_TARGET_Y = define(EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LASER_TARGET_Z = define(EntityDataSerializers.FLOAT);


    public static final byte STATE_IDLE = 0;
    public static final byte STATE_ATTACK_START = 1;
    public static final byte STATE_ATTACK_ONGOING = 2;
    public static final byte STATE_ATTACK_END = 3;

    public static final double DISTANCE_MULTIPLIER = 50D;
    public static final double DEFAULT_MAX_ASSIST_RANGE = 32.0;

    public boolean isWithinAssistRange(BlockPos pos) {
        double range = FluxnauticsConfig.common.mekaRobitAssistRange.getOrDefault();
        return pos != null && pos.distToCenterSqr(this.getX(), this.getY(), this.getZ()) <= range * range;
    }

    public boolean isWithinAssistRange(Entity target) {
        double range = FluxnauticsConfig.common.mekaRobitAssistRange.getOrDefault();
        return target != null && target.distanceToSqr(this) <= range * range;
    }

    private final ItemStackEnergyProxy energyProxy =
        new ItemStackEnergyProxy(this::resolveBoundStack, 100_000L, (IContentsListener) null);
    private final List<IEnergyContainer> energyContainers = Collections.singletonList(energyProxy);


    private GlobalPos homeLocation;
    private ItemStack cachedBoundStack = ItemStack.EMPTY;
    private int cachedBoundSlot = -1;
    private int moduleCacheTimer = 0;
    private float cachedPotentialDamage = -1;
    private int cachedSwarmCount = -1;
    private ModuleSwarmUnit.SwarmFormation cachedFormation = null;


    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState attackStartState = new AnimationState();
    public final AnimationState attackOngoingState = new AnimationState();
    public final AnimationState attackEndState = new AnimationState();
    public final AnimationState patrolAnimationState = new AnimationState();
    public final AnimationState fullControlAnimationState = new AnimationState();

    public final LerpedFloat smoothYaw;
    public final LerpedFloat smoothPitch;
    public final LerpedFloat smoothRoll;

    public static final byte MODE_FOLLOW = 0;
    public static final byte MODE_ASSIST = 1;
    public static final byte MODE_FULL_CONTROL = 2;

    private byte previousMode = MODE_FOLLOW;

    public float clientXRot = 0;
    public float clientYRot = 0;
    public float clientForward = 0;
    public float clientStrafe = 0;
    public float clientUp = 0;
    public final LerpedFloat smoothBeamThickness = LerpedFloat.linear().startWithValue(0);
    public Vec3 clientTargetPos = Vec3.ZERO;
    public boolean clientTargetValid = false;
    public float clientColorTransition = 0f;

    private int alertTicks = 0;
    public int attackTimer = 0;
    private int endAnimTicks = 0;
    public UUID mainDroneUuid = null;
    private final ModuleSwarmUnit.SwarmState swarmState = new ModuleSwarmUnit.SwarmState();

    public boolean isAssisting = false;
    public boolean isAssistMining = false;
    public boolean isAssistDelete = false;
    public BlockPos assistBlockPos = BlockPos.ZERO;
    public int assistEntityId = -1;
    public Vec3 assistVisualPos = Vec3.ZERO;
    public float assistProgress = 0;
    public int assistTimer = 0;

    public EntityMekaRobit(EntityType<? extends EntityMekaRobit> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.moveControl = new MoveControlHover(this);

        this.smoothYaw = LerpedFloat.angular().startWithValue(this.getYRot());
        this.smoothPitch = LerpedFloat.angular().startWithValue(this.getXRot());
        this.smoothRoll = LerpedFloat.angular().startWithValue(0.0F);
    }

    public static AttributeSupplier.Builder getDefaultAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 16.0D)
            .add(Attributes.FOLLOW_RANGE, 64.0D)
            .add(Attributes.MOVEMENT_SPEED, 2.4D)
            .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.0D)
            .add(Attributes.FLYING_SPEED, 2.9D)
            .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Mekanism.gameProfile.getId());
        builder.define(OWNER_NAME, "");
        builder.define(DATA_MODE, MODE_FOLLOW);
        builder.define(HOVER_LEFT, true);
        builder.define(ALERTED, false);
        builder.define(TARGET_ID, -1);
        builder.define(COMBAT_STATE, STATE_IDLE);
        builder.define(IS_SWARM_MINION, false);
        builder.define(MAIN_DRONE_ID, -1);
        builder.define(FORMATION_INDEX, 0);
        builder.define(FORMATION_SIZE, 1);
        builder.define(IS_FOCUS_FIRING, false);
        builder.define(IDLE_COLOR, 0x00FFFF);
        builder.define(ATTACK_COLOR, 0xFF0000);
        builder.define(RAINBOW_MODE, false);
        builder.define(LASER_TARGET_X, 0.0F);
        builder.define(LASER_TARGET_Y, 0.0F);
        builder.define(LASER_TARGET_Z, 0.0F);
    }

    private static <T> EntityDataAccessor<T> define(EntityDataSerializer<T> dataSerializer) {
        return SynchedEntityData.defineId(EntityMekaRobit.class, dataSerializer);
    }

    public void setHome(GlobalPos globalPos) {
        this.homeLocation = globalPos;
    }
    public Optional<GlobalPos> getHome() {
        return Optional.ofNullable(this.homeLocation);
    }
    public boolean isBoundToBlock() { return this.homeLocation != null; }

    public boolean isSwarmMinion() { return this.entityData.get(IS_SWARM_MINION); }
    public void setSwarmMinion(boolean minion) { this.entityData.set(IS_SWARM_MINION, minion); }

    public int getMainDroneId() { return this.entityData.get(MAIN_DRONE_ID); }
    public void setMainDroneId(int id) { this.entityData.set(MAIN_DRONE_ID, id); }

    public int getFormationIndex() { return this.entityData.get(FORMATION_INDEX); }
    public void setFormationIndex(int index) { this.entityData.set(FORMATION_INDEX, index); }

    public int getFormationSize() { return this.entityData.get(FORMATION_SIZE); }
    public void setFormationSize(int size) { this.entityData.set(FORMATION_SIZE, size); }

    public boolean isAlerted() { return this.entityData.get(ALERTED); }
    public void setAlerted(boolean alerted) {
        if (alerted) this.alertTicks = 100;
        this.entityData.set(ALERTED, alerted);
    }

    public int getTargetId() { return this.entityData.get(TARGET_ID); }
    public void setTargetId(int id) { this.entityData.set(TARGET_ID, id); }

    public byte getCombatState() { return this.entityData.get(COMBAT_STATE); }
    public void setCombatState(byte state) {
        if (state == STATE_ATTACK_END) this.endAnimTicks = 5;
        this.entityData.set(COMBAT_STATE, state);
    }

    public byte getMode() { return this.entityData.get(DATA_MODE); }
    public void setMode(byte mode) { this.entityData.set(DATA_MODE, mode); }

    public boolean isHoverLeft() { return this.entityData.get(HOVER_LEFT); }
    public void setHoverLeft(boolean left) { this.entityData.set(HOVER_LEFT, left); }

    public boolean isFocusFiring() { return this.entityData.get(IS_FOCUS_FIRING); }
    public void setFocusFiring(boolean focus) { this.entityData.set(IS_FOCUS_FIRING, focus); }

    public int getIdleColor() { return this.entityData.get(IDLE_COLOR); }
    public void setIdleColor(int color) { this.entityData.set(IDLE_COLOR, color); }

    public int getAttackColor() { return this.entityData.get(ATTACK_COLOR); }
    public void setAttackColor(int color) { this.entityData.set(ATTACK_COLOR, color); }

    public boolean isRainbowMode() { return this.entityData.get(RAINBOW_MODE); }
    public void setRainbowMode(boolean rainbow) { this.entityData.set(RAINBOW_MODE, rainbow); }

    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID);
    }

    public void setOwnerUUID(UUID uuid) { this.entityData.set(OWNER_UUID, uuid); }

    public void setOwner(@Nullable Player player) {
        if (player == null) return;
        this.setOwnerUUID(player.getUUID());
        this.entityData.set(OWNER_NAME, player.getScoreboardName());
    }

    public Player getOwner() {
        LivingEntity owner = OwnableEntity.super.getOwner();
        if (owner == null)
            return null;
        if (owner instanceof Player player)
            return player;
        return null;
    }

    public void setLaserTarget(Vector3dc pos) {
        this.assistVisualPos = JOMLConversion.toMojang(pos);
        this.entityData.set(LASER_TARGET_X, (float)pos.x());
        this.entityData.set(LASER_TARGET_Y, (float)pos.y());
        this.entityData.set(LASER_TARGET_Z, (float)pos.z());
    }

    public void setLaserTarget(Vector3fc pos) {
        this.assistVisualPos = new Vec3(pos.x(), pos.y(), pos.z());
        this.entityData.set(LASER_TARGET_X, pos.x());
        this.entityData.set(LASER_TARGET_Y, pos.y());
        this.entityData.set(LASER_TARGET_Z, pos.z());
    }

    public Vec3 getLaserTarget() {
        return new Vec3(this.entityData.get(LASER_TARGET_X), this.entityData.get(LASER_TARGET_Y), this.entityData.get(LASER_TARGET_Z));
    }

    @Override
    public float getScale() {
        return isSwarmMinion() ? 0.6F : 1.0F;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean isAlliedTo(Entity entity) {
        if (this == entity)
            return true;
        UUID owner = this.getOwnerUUID();
        if (owner != null && entity.getUUID().compareTo(owner) == 0)
            return true;
        if (entity instanceof EntityMekaRobit other && Objects.equals(other.getOwnerUUID(), owner))
            return true;
        return super.isAlliedTo(entity);
    }

    private Optional<IMekaRobitDock> getHomeBlock() {
        Level level = this.level();
        return getHome().map((homeLocation) -> {
            Level serverWorld;
            if (level.dimension() == homeLocation.dimension()) {
                serverWorld = level;
            } else {
                MinecraftServer server = getServer();
                serverWorld = server == null ? null : server.getLevel(homeLocation.dimension());
            }
            BlockPos homePos = homeLocation.pos();
            if (!WorldUtils.isBlockLoaded(serverWorld, homePos))
                return null;

            BlockEntity be =WorldUtils.getTileEntity(serverWorld, homePos);
            if (be instanceof IMekaRobitDock droneDock)
                return droneDock;
            return null;
        });
    }

    private ItemStack getHomeBlockStack() {
        return getHomeBlock().map(IMekaRobitDock::getDroneStack).orElse(ItemStack.EMPTY);
    }

    public ItemStack resolveBoundStack() {
        if (isBoundToBlock()) {
            return getHomeBlockStack();
        }

        if (isSwarmMinion()) {
            if (getMainDroneId() != -1) {
                EntityMekaRobit main = getMainDrone();
                if (main != null) return main.resolveBoundStack();
            } else {
                Player owner = getOwner();
                if (owner != null) {
                    return owner.getItemBySlot(EquipmentSlot.CHEST);
                }
            }
            return ItemStack.EMPTY;
        }

        Player owner = getOwner();
        if (owner == null || getOwnerUUID() == null) {
            invalidateBoundStackCache();
            return ItemStack.EMPTY;
        }

        if (cachedBoundSlot >= 0 && cachedBoundSlot < owner.getInventory().getContainerSize()) {
            ItemStack stackAtSlot = owner.getInventory().getItem(cachedBoundSlot);
            if (isBoundStack(stackAtSlot)) {
                cachedBoundStack = stackAtSlot;
                return cachedBoundStack;
            }
        }

        for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
            ItemStack stack = owner.getInventory().getItem(i);
            if (isBoundStack(stack)) {
                cachedBoundStack = stack;
                cachedBoundSlot = i;
                return cachedBoundStack;
            }
        }

        invalidateBoundStackCache();
        return ItemStack.EMPTY;
    }

    private boolean isBoundStack(ItemStack stack) {
        return !stack.isEmpty()
            && stack.getItem() instanceof MekaRobitItem
            && this.getUUID().equals(stack.get(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY.get()));
    }

    private void invalidateBoundStackCache() {
        cachedBoundStack = ItemStack.EMPTY;
        cachedBoundSlot = -1;
    }

    @Override
    public @NonNull List<IEnergyContainer> getEnergyContainers(@Nullable Direction side) {
        return canHandleEnergy() ? energyContainers : Collections.emptyList();
    }

    public IEnergyContainer getEnergyContainer() { return energyProxy; }

    public float getPotentialDamage() {
        if (this.moduleCacheTimer > 0 && this.cachedPotentialDamage >= 0) return this.cachedPotentialDamage;

        float damage = 8.0f;
        ItemStack stack = this.resolveBoundStack();
        if (!stack.isEmpty()) {
            IModuleContainer container = IModuleHelper.INSTANCE.getModuleContainer(stack);
            if (container != null) {
                IModule<ModuleAttackAmplificationUnit> module = container.get(MekanismModules.ATTACK_AMPLIFICATION_UNIT);
                if (module != null && module.isEnabled())
                    damage += module.getCustomInstance().getDamage();
            }
        }
        if (this.isSwarmMinion()) damage /= 2.0f;

        this.cachedPotentialDamage = damage;
        return damage;
    }

    float extractEnergyForDamage(float damage) {
        IEnergyContainer energyContainer = this.getEnergyContainer();
        if (energyContainer == null || energyContainer.isEmpty()) return 0;

        long energyCost = MathUtils.clampToLong(MekanismConfig.gear.mekaToolEnergyUsageWeapon.get() * (damage / 2D));
        long extracted = energyContainer.extract(energyCost, Action.EXECUTE, AutomationType.INTERNAL);

        if (extracted < energyCost) return 0;
        return damage;
    }

    public boolean dealDamageTo(LivingEntity target, float damage) {
        LivingEntity livingentity = this.getOwner();
        if (target.isAlive() && !target.isInvulnerable() && target != livingentity) {
            if (livingentity == null) {
                target.hurt(this.damageSources().source(FNDamageTypes.TEMPORAL.key(), this), damage);
            } else {
                if (livingentity.isAlliedTo(target)) return false;

                DamageSource damagesource = FNDamageTypes.TEMPORAL.source(this.registryAccess(), this, livingentity);
                if (target.hurt(damagesource, damage) && this.level() instanceof ServerLevel serverlevel) {
                    EnchantmentHelper.doPostAttackEffects(serverlevel, target, damagesource);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        final SubLevel subLevel = Sable.HELPER.getContaining(this);
        if (subLevel != null && this.getMode() == MODE_FULL_CONTROL) {
            EntitySubLevelUtil.kickEntity(subLevel, this);
        }

        if (!this.level().isClientSide) {
            if (this.moduleCacheTimer > 0) {
                this.moduleCacheTimer--;
            } else {
                this.cachedPotentialDamage = -1;
                this.cachedSwarmCount = -1;
                this.cachedFormation = null;
                this.moduleCacheTimer = 20; // Cache for 1 second
            }

            if (this.attackTimer > 0) {
                this.attackTimer--;
                if (this.attackTimer == 0 && this.getCombatState() == STATE_ATTACK_ONGOING) {
                    this.setCombatState(STATE_ATTACK_END);
                    this.setTargetId(-1);
                }
            }

            if (this.isSwarmMinion()) {
                if (getMainDroneId() != -1) {
                    EntityMekaRobit main = getMainDrone();
                    if (main == null || !main.isAlive() || main.isRemoved()) {
                        this.discard();
                        return;
                    }
                } else {
                    Player owner = this.getOwner();
                    if (owner == null || !owner.isAlive()) {
                        this.discard();
                        return;
                    }

                    ItemStack chestplate = owner.getItemBySlot(EquipmentSlot.CHEST);
                    boolean shouldDespawn = true;

                    if (!chestplate.isEmpty()) {
                        IModuleContainer container = IModuleHelper.INSTANCE.getModuleContainer(chestplate);
                        if (container != null) {
                            IModule<?> module = container.get(FNModules.SWARM_UNIT);
                            if (module != null && module.isEnabled()) {
                                int drones = (int) ((ModuleSwarmUnit) module.getCustomInstance()).getDrones();
                                if (drones > 0) {
                                    IEnergyContainer energy = StorageUtils.getEnergyContainer(chestplate, 0);
                                    if (energy != null && !energy.isEmpty()) {
                                        shouldDespawn = false;
                                    }
                                }
                            }
                        }
                    }

                    if (shouldDespawn) {
                        this.discard();
                        return;
                    }
                }
            } else {
                if (this.isBoundToBlock()) {
                    ItemStack stack = this.resolveBoundStack();
                    if (stack.isEmpty()) {
                        this.discard();
                        return;
                    }
                } else {
                    Player owner = this.getOwner();
                    if (owner == null || this.resolveBoundStack().isEmpty()) {
                        this.discard();
                        return;
                    }
                }

                ItemStack boundStack = this.resolveBoundStack();
                if (!boundStack.isEmpty() && boundStack.getItem() instanceof MekaRobitItem) {
                    IModuleContainer container = IModuleHelper.INSTANCE.getModuleContainer(boundStack);
                    if (container != null) {
                        IModule<ModuleQuantumPullUnit> magModule = container.getIfEnabled(FNModules.QUANTUM_PULL_UNIT);
                        if (magModule != null) {
                            magModule.getCustomInstance().tickRobit(this, magModule, boundStack);
                        }
                    }
                }

                int targetMinions = getSwarmModuleCount();
                ModuleSwarmUnit.manageSwarm(this, targetMinions, this.swarmState);

                if (this.isAssisting) {
                    if (handleAssisting()) return;
                } else {
                    if (this.getCombatState() == STATE_ATTACK_ONGOING && this.getTarget() == null) {
                        this.setCombatState(STATE_ATTACK_END);
                    }
                    if (this.tickCount % 20 == 0) {
                        ModuleSwarmUnit.forEachSwarmMember(this, this.swarmState, r -> {
                            r.isAssisting = false;
                            if (r.getCombatState() == STATE_ATTACK_ONGOING && r.getTarget() == null) {
                                r.setCombatState(STATE_ATTACK_END);
                                r.setFocusFiring(false);
                            }
                        });
                    }
                }
            }
        }

        if (!this.level().isClientSide && this.alertTicks > 0) {
            this.alertTicks--;
            if (this.alertTicks == 0) this.setAlerted(false);
        }

        if (!this.level().isClientSide && this.getCombatState() == STATE_ATTACK_END && --this.endAnimTicks <= 0)
            this.setCombatState(STATE_IDLE);

        if (this.level().isClientSide) {
            this.setupAnimationStates();
        }

        if (isLocallyPiloted()) {
            this.smoothYaw.setValue(this.yHeadRot);
            this.smoothPitch.setValue(this.getXRot());
            this.smoothRoll.setValue(0.0F);
        } else {
            float targetRoll = Mth.wrapDegrees(this.yHeadRot - this.yHeadRotO) * 1.5F;
            this.smoothYaw.chase(this.yHeadRot, 0.25F, LerpedFloat.Chaser.EXP);
            this.smoothPitch.chase(this.getXRot(), 0.25F, LerpedFloat.Chaser.EXP);
            this.smoothRoll.chase(targetRoll, 0.25F, LerpedFloat.Chaser.EXP);

            this.smoothYaw.tickChaser();
            this.smoothPitch.tickChaser();
            this.smoothRoll.tickChaser();
        }
    }

    private boolean handleAssisting() {
        this.setCombatState(STATE_ATTACK_ONGOING);
        this.setFocusFiring(true);

        if (this.isAssistMining) {
            this.setTargetId(-1);
            this.setLaserTarget(JOMLConversion.toJOML(this.assistVisualPos));
            this.getLookControl().setLookAt(this.assistVisualPos);

            final float[] totalPowerArr = {this.getPotentialDamage() * 0.5f};
            ModuleSwarmUnit.forEachSwarmMember(this, this.swarmState, r -> {
                r.isAssisting = true;
                r.setCombatState(STATE_ATTACK_ONGOING);
                r.setFocusFiring(true);
                r.setTargetId(-1);
                r.setLaserTarget(JOMLConversion.toJOML(this.assistVisualPos));
                r.getLookControl().setLookAt(this.assistVisualPos);
                totalPowerArr[0] += r.getPotentialDamage() * 0.5f;
            });
            float totalPower = totalPowerArr[0];

            BlockState state = this.level().getBlockState(this.assistBlockPos);
            float hardness = state.getDestroySpeed(this.level(), this.assistBlockPos);
            if (hardness < 0) {
                this.isAssisting = false;
                return true;
            }

            if (!isWithinAssistRange(this.assistBlockPos)) {
                this.isAssisting = false;
                return true;
            }

            this.assistProgress += totalPower;
            this.level().levelEvent(2001, this.assistBlockPos, Block.getId(state));

            if (this.assistProgress >= hardness * 10) {
                this.assistProgress = 0;
                if (this.getOwner() instanceof final ServerPlayer serverOwner) {
                    breakBlockWithModules(serverOwner, this.assistBlockPos, this.isAssistDelete);
                    if (this.isAssistDelete) {
                        this.level().explode(this, this.assistBlockPos.getX(), this.assistBlockPos.getY(), this.assistBlockPos.getZ(), 2.0f, Level.ExplosionInteraction.BLOCK);
                    }
                } else {
                    this.isAssisting = false;
                    return true;
                }
            }
        } else {
            Entity target = this.level().getEntity(this.assistEntityId);
            if (target instanceof LivingEntity living) {
                if (!isWithinAssistRange(living)) {
                    this.isAssisting = false;
                    return true;
                }
                this.setTargetId(this.assistEntityId);
                this.setLaserTarget(living.getEyePosition().toVector3f());
                this.getLookControl().setLookAt(living);

                final float[] totalDmgArr = {this.getPotentialDamage()};
                ModuleSwarmUnit.forEachSwarmMember(this, this.swarmState, r -> {
                    r.isAssisting = true;
                    r.setCombatState(STATE_ATTACK_ONGOING);
                    r.setFocusFiring(true);
                    r.setTargetId(this.assistEntityId);
                    r.setLaserTarget(living.getEyePosition().toVector3f());
                    r.getLookControl().setLookAt(living);
                    totalDmgArr[0] += r.getPotentialDamage();
                });
                float totalDmg = totalDmgArr[0];

                if (this.assistTimer <= 0) {
                    this.assistTimer = 5;
                    if (this.isAssistDelete) totalDmg *= 2.0f;

                    this.dealDamageTo(living, totalDmg);
                    this.extractEnergyForDamage(totalDmg);

                    if (this.isAssistDelete) {
                        this.level().explode(this, living.getX(), living.getY(), living.getZ(), 2.0f, Level.ExplosionInteraction.MOB);
                    }
                } else {
                    this.assistTimer--;
                }
            } else {
                this.isAssisting = false;
            }
        }
        return false;
    }

    @Override
    public void baseTick() {
        if (!level().isClientSide) {
            Player owner = getOwner();
            if (owner != null && !energyProxy.isEmpty() && getRoundedTravelEnergy() > 0) {
                energyProxy.extract(getRoundedTravelEnergy(), Action.EXECUTE, AutomationType.MANUAL);
            }
        }

        super.baseTick();

        if (!level().isClientSide) {
            if (energyProxy.isEmpty()) {
                remove(RemovalReason.DISCARDED);
            }
        }
    }

    public int getSwarmModuleCount() {
        if (this.moduleCacheTimer > 0 && this.cachedSwarmCount >= 0) return this.cachedSwarmCount;

        int count = 0;
        ItemStack stack = resolveBoundStack();
        if (!stack.isEmpty()) {
            IModuleContainer container = IModuleHelper.INSTANCE.getModuleContainer(stack);
            if (container != null) {
                IModule<?> module = container.get(FNModules.SWARM_UNIT);
                if (module != null && module.isEnabled()) {
                    count = (int) ((ModuleSwarmUnit) module.getCustomInstance()).getDrones();
                }
            }
        }

        this.cachedSwarmCount = count;
        return count;
    }

    public ModuleSwarmUnit.SwarmFormation getSwarmFormation() {
        if (this.moduleCacheTimer > 0 && this.cachedFormation != null) return this.cachedFormation;

        ItemStack stack = resolveBoundStack();
        if (stack.isEmpty()) {
            this.cachedFormation = ModuleSwarmUnit.SwarmFormation.ROUND;
            return this.cachedFormation;
        }
        IModuleContainer container = IModuleHelper.INSTANCE.getModuleContainer(stack);
        if (container == null) {
            this.cachedFormation = ModuleSwarmUnit.SwarmFormation.ROUND;
            return this.cachedFormation;
        }

        IModule<?> module = container.get(FNModules.SWARM_UNIT);
        if (module != null && module.isEnabled()) {
            this.cachedFormation = ((ModuleSwarmUnit) module.getCustomInstance()).swarmFormation();
        } else {
            this.cachedFormation = ModuleSwarmUnit.SwarmFormation.ROUND;
        }
        return this.cachedFormation;
    }

    public EntityMekaRobit getMainDrone() {
        if (!isSwarmMinion() || getMainDroneId() == -1) return null;
        Entity entity = this.level().getEntity(getMainDroneId());
        return entity instanceof EntityMekaRobit main ? main : null;
    }

    private long getRoundedTravelEnergy() {
        return MathUtils.ceilToLong(FluxnauticsConfig.common.mekaRobitTravelEnergyMultiplier.getOrDefault() * (isSwarmMinion() ? 64 : 256) * (getDeltaMovement().lengthSqr())) + 1_000L;
    }

    @Override
    public @NonNull AABB getBoundingBoxForCulling() {
        AABB box = super.getBoundingBoxForCulling();
        int targetId = this.getTargetId();
        if (targetId != -1 && targetId != this.getId()) {
            Entity target = this.level().getEntity(targetId);
            if (target != null) {
                box = box.minmax(target.getBoundingBox());
            }
        }
        Vec3 targetPos = Sable.HELPER.projectOutOfSubLevel(this.level(), this.getLaserTarget());
        if (!targetPos.equals(Vec3.ZERO)) {
            Vec3 delta = targetPos.subtract(this.position());
            box = box.expandTowards(delta).inflate(1.0);
        }
        return box;
    }

    private void breakBlockWithModules(ServerPlayer player, BlockPos pos, boolean isDelete) {
        Level level = player.level();
        BlockState state = level.getBlockState(pos);
        if (state.getDestroySpeed(level, pos) < 0 && !player.gameMode.isCreative())
            return;

        ItemStack stack = this.resolveBoundStack();
        IModuleContainer container = IModuleHelper.INSTANCE.getModuleContainer(stack);

        if (container == null)
            return;

        BlockEvent.BreakEvent event = CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state);
        if (event.isCanceled())
            return;

        FluidState fluidState = state.getFluidState();
        BlockEntity tileEntity = WorldUtils.getTileEntity(level, pos);
        state = state.getBlock().playerWillDestroy(level, pos, state, player);
        Block block = state.getBlock();
        if (state.onDestroyedByPlayer(level, pos, player, true, fluidState)) {
            block.destroy(level, pos, state);
            block.playerDestroy(level, player, pos, state, tileEntity, stack);
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            handleAOEMine(player.level(), state, pos, player, container, stack);
        }
    }

    public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull IModuleContainer container) {
        IEnergyContainer energyContainer = StorageUtils.getEnergyContainer(stack, 0);
        if (energyContainer == null) {
            return 0;
        }
        long energyRequired = getDestroyEnergy(container, state.destroySpeed, container.hasEnabled(MekanismModules.SILK_TOUCH_UNIT));
        long energyAvailable = energyContainer.extract(energyRequired, Action.SIMULATE, AutomationType.MANUAL);
        if (energyAvailable < energyRequired) {
            return (float) (MekanismConfig.gear.mekaToolBaseEfficiency.get() * ((double) energyAvailable / energyRequired));
        }
        IModule<ModuleExcavationEscalationUnit> module = container.getIfEnabled(MekanismModules.EXCAVATION_ESCALATION_UNIT);
        return module == null ? MekanismConfig.gear.mekaToolBaseEfficiency.get() : module.getCustomInstance().getEfficiency();
    }

    public void handleAOEMine(@NotNull Level world, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull LivingEntity entity, @NotNull IModuleContainer container, @NotNull ItemStack stack) {
        IEnergyContainer energyContainer = StorageUtils.getEnergyContainer(stack, 0);
        if (energyContainer == null)
            return;

        boolean silk = container.hasEnabled(MekanismModules.SILK_TOUCH_UNIT);
        long modDestroyEnergy = getDestroyEnergy(container, silk);
        long energyRequired = getDestroyEnergy(modDestroyEnergy, state.getDestroySpeed(world, pos));
        energyContainer.extract(energyRequired, Action.EXECUTE, AutomationType.MANUAL);
        //AOE/vein mining handling
        if (!world.isClientSide && entity instanceof ServerPlayer player && !player.isCreative() &&
            energyContainer.extract(energyRequired, Action.SIMULATE, AutomationType.MANUAL) >= energyRequired) {
            Map<BlockPos, BlockState> blocks = getBlastedBlocks(world, player, container, pos, state);
            blocks = blocks.isEmpty() && ModuleVeinMiningUnit.canVeinBlock(state) ? Map.of(pos, state) : blocks;

            Reference2BooleanMap<Block> oreTracker = blocks.values().stream().collect(Collectors.toMap(BlockBehaviour.BlockStateBase::getBlock,
                bs -> bs.is(MekanismTags.Blocks.ATOMIC_DISASSEMBLER_ORE), (l, r) -> l, Reference2BooleanArrayMap::new));

            Object2IntMap<BlockPos> veinedBlocks = getVeinedBlocks(world, container, blocks, oreTracker);
            if (!veinedBlocks.isEmpty()) {
                long baseDestroyEnergy = getDestroyEnergy(silk);
                MekanismUtils.veinMineArea(energyContainer, energyRequired, modDestroyEnergy, baseDestroyEnergy, world, pos, player, stack, stack.getItem(), veinedBlocks,
                    EntityMekaRobit::getDestroyEnergy, (base, hardness, distance, bs) -> {
                        double multiplier = 0.5 * Math.pow(distance, bs.is(MekanismTags.Blocks.ATOMIC_DISASSEMBLER_ORE) ? 1.5 : 2);
                        return MathUtils.ceilToLong(getDestroyEnergy(base, hardness) * multiplier);
                    });
            }
        }
    }

    public Map<BlockPos, BlockState> getBlastedBlocks(Level world, Player player, @NotNull IModuleContainer container, BlockPos pos, BlockState state) {
        if (!player.isShiftKeyDown()) {
            IModule<ModuleBlastingUnit> blastingUnit = container.getIfEnabled(MekanismModules.BLASTING_UNIT);
            if (blastingUnit != null) {
                int radius = blastingUnit.getCustomInstance().getBlastRadius();
                if (radius > 0 && IBlastingItem.canBlastBlock(world, pos, state)) {
                    return IBlastingItem.findPositions(world, pos, player, radius);
                }
            }
        }
        return Collections.emptyMap();
    }

    private Object2IntMap<BlockPos> getVeinedBlocks(Level world, @NotNull IModuleContainer container, Map<BlockPos, BlockState> blocks, Reference2BooleanMap<Block> oreTracker) {
        IModule<ModuleVeinMiningUnit> veinMiningUnit = container.getIfEnabled(MekanismModules.VEIN_MINING_UNIT);
        if (veinMiningUnit != null) {
            ModuleVeinMiningUnit customInstance = veinMiningUnit.getCustomInstance();
            return ModuleVeinMiningUnit.findPositions(world, blocks, customInstance.extended() ? customInstance.getExcavationRange() : 0, oreTracker);
        }
        return blocks.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, be -> 0, (l, r) -> l, Object2IntArrayMap::new));
    }

    private static long getDestroyEnergy(boolean silk) {
        return silk ? MekanismConfig.gear.mekaToolEnergyUsageSilk.get() : MekanismConfig.gear.mekaToolEnergyUsage.get();
    }

    public static long getDestroyEnergy(@NotNull IModuleContainer container, float hardness, boolean silk) {
        return getDestroyEnergy(getDestroyEnergy(container, silk), hardness);
    }

    private static long getDestroyEnergy(long baseDestroyEnergy, float hardness) {
        return hardness == 0 ? Math.max(baseDestroyEnergy / 2, 1) : baseDestroyEnergy;
    }

    private static long getDestroyEnergy(@NotNull IModuleContainer container, boolean silk) {
        long destroyEnergy = getDestroyEnergy(silk);
        IModule<ModuleExcavationEscalationUnit> module = container.getIfEnabled(MekanismModules.EXCAVATION_ESCALATION_UNIT);
        float efficiency = module == null ? MekanismConfig.gear.mekaToolBaseEfficiency.get() : module.getCustomInstance().getEfficiency();
        return MathUtils.clampToLong(destroyEnergy * efficiency);
    }

    private void setupAnimationStates() {
        byte state = this.getCombatState();

        if (this.level().isClientSide) {
            float target = (state == STATE_ATTACK_ONGOING || state == STATE_ATTACK_START) ? 1.0f : 0.0f;
            this.clientColorTransition = Mth.lerp(0.1f, this.clientColorTransition, target);
            float targetThickness = (state == STATE_ATTACK_ONGOING || state == STATE_ATTACK_START) ? 1.0f : 0.0f;
            this.smoothBeamThickness.chase(targetThickness, 0.25F, LerpedFloat.Chaser.EXP);
            this.smoothBeamThickness.tickChaser();

            if (state != STATE_ATTACK_ONGOING && state != STATE_ATTACK_START) {
                this.clientTargetValid = false;
            }
        }

        if (state == STATE_ATTACK_START) {
            this.attackStartState.startIfStopped(this.tickCount);
        } else {
            this.attackStartState.stop();
        }

        if (state == STATE_ATTACK_ONGOING) {
            this.attackOngoingState.startIfStopped(this.tickCount);
        } else {
            this.attackOngoingState.stop();
        }

        if (state == STATE_ATTACK_END) {
            this.attackEndState.startIfStopped(this.tickCount);
        } else {
            this.attackEndState.stop();
        }

        if (state == STATE_IDLE) {
            if (getMode() == MODE_FULL_CONTROL) {
                this.fullControlAnimationState.startIfStopped(this.tickCount);
                this.patrolAnimationState.stop();
            } else if (this.getDeltaMovement().lengthSqr() > 0.01D) {
                this.patrolAnimationState.startIfStopped(this.tickCount);
                this.fullControlAnimationState.stop();
            } else {
                this.idleAnimationState.startIfStopped(this.tickCount);
                this.patrolAnimationState.stop();
                this.fullControlAnimationState.stop();
            }
        } else {
            this.patrolAnimationState.stop();
            this.fullControlAnimationState.stop();
        }
    }

    public void enterFullControl() {
        if (this.getMode() != MODE_FULL_CONTROL) {
            this.previousMode = this.getMode();
            this.setMode(MODE_FULL_CONTROL);
        }
        this.clientForward = 0f;
        this.clientStrafe  = 0f;
        this.clientUp      = 0f;
    }

    public void exitFullControl() {
        this.setMode(this.previousMode);
        this.clientForward = 0f;
        this.clientStrafe  = 0f;
        this.clientUp      = 0f;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new GoalTeleportToOwner(this, 24.0D));
        this.goalSelector.addGoal(1, new GoalLaserAttack(this, 64f));
        this.goalSelector.addGoal(2, new GoalFollowOwner(this, 1.5D, 16.0F, 2.0F));
        this.goalSelector.addGoal(2, new GoalFollowMainDrone(this, 1.5D));
        this.goalSelector.addGoal(3, new GoalIdlePatrol(this, 0.8D));

        this.targetSelector.addGoal(1, new GoalOwnerHurtBy(this));
        this.targetSelector.addGoal(2, new GoalOwnerHurtTarget(this));
        this.targetSelector.addGoal(3, new GoalHurtBy(this));
        this.targetSelector.addGoal(4, new GoalSmartAttackTarget(this, 32f));
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;

        float ratioAbsorbed = getDamageAbsorbed(this, source, amount);
        if (ratioAbsorbed > 0.0F) {
            amount = amount * Math.max(0, 1 - ratioAbsorbed);
        }

        return super.hurt(source, amount);
    }


    public static float getDamageAbsorbed(EntityMekaRobit entity, DamageSource source, float amount) {
        if (amount <= 0) {
            return 0;
        }
        float ratioAbsorbed = 0;
        ItemStack stack = entity.resolveBoundStack();
        if (stack.isEmpty()) {
            return 0;
        }
        if (!(stack.getItem() instanceof IRadialModuleContainerItem container))
            return 0;

        IEnergyContainer energyContainer = StorageUtils.getEnergyContainer(stack, 0);

        if (energyContainer == null || energyContainer.isEmpty())
            return 0;

        EnergyUsageInfo usageInfo = new EnergyUsageInfo(energyContainer.getEnergy());

        for (IModule<?> module : container.getModules(stack)) {
            if (module.isEnabled()) {
                ICustomModule.ModuleDamageAbsorbInfo damageAbsorbInfo = getModuleDamageAbsorbInfo(module, source);
                if (damageAbsorbInfo != null) {
                    float absorption = damageAbsorbInfo.absorptionRatio().getAsFloat();
                    ratioAbsorbed += absorbDamage(usageInfo, amount, absorption, ratioAbsorbed, damageAbsorbInfo.energyCost());
                    if (ratioAbsorbed >= 1) {
                        //If we have fully absorbed the damage, stop checking/trying to absorb more
                        break;
                    }
                }
            }
        }


        if (ratioAbsorbed < 1) {
            //If we haven't fully absorbed it check the individual pieces of armor for if they can absorb any
            Float absorbRatio = null;

            //If we haven't looked up yet if we can absorb the damage type and if we can't
            // stop checking if the armor is able to
            if (!source.is(Tags.DamageTypes.IS_TECHNICAL) && (source.is(MekanismAPITags.DamageTypes.MEKASUIT_ALWAYS_SUPPORTED) || !source.is(DamageTypeTags.BYPASSES_ARMOR))) {
                // Next lookup the ratio at which we can absorb the given damage type from the data map
                MekaSuitAbsorption absorptionData = IMekanismDataMapTypes.INSTANCE.getMekaSuitAbsorption(entity.registryAccess(), source.typeHolder());
                if (absorptionData == null) {
                    absorbRatio = MekanismConfig.gear.mekaSuitUnspecifiedDamageRatio.get();
                } else {
                    absorbRatio = absorptionData.absorption();
                }
                if (absorbRatio != 0) {
                    ratioAbsorbed += absorbDamage(usageInfo, amount, absorbRatio, ratioAbsorbed, MekanismConfig.gear.mekaSuitEnergyUsageDamage);
                }
            }
        }

        energyContainer.extract(usageInfo.energyUsed, Action.EXECUTE, AutomationType.MANUAL);
        return Math.min(ratioAbsorbed, 1);
    }

    private static float absorbDamage(EnergyUsageInfo usageInfo, float amount, float absorption, float currentAbsorbed, LongSupplier energyCost) {
        //Cap the amount that we can absorb to how much we have left to absorb
        absorption = Math.min(1 - currentAbsorbed, absorption);
        float toAbsorb = amount * absorption;
        if (toAbsorb > 0) {
            long usage = MathUtils.ceilToLong(energyCost.getAsLong() * toAbsorb);
            if (usage == 0L) {
                //No energy is actually needed to absorb the damage, either because of the config
                // or how small the amount to absorb is
                return absorption;
            } else if (usageInfo.energyAvailable >= usage) {
                //If we have more energy available than we need, increase how much energy we "used"
                // and decrease how much we have available.
                usageInfo.energyUsed += usage;
                usageInfo.energyAvailable -= usage;
                return absorption;
            } else if (usageInfo.energyAvailable > 0L) {
                //Otherwise, if we have energy available but not as much as needed to fully absorb it
                // then we calculate what ratio we are able to block
                float absorbedPercent = (float) (usageInfo.energyAvailable / (double) usage);
                usageInfo.energyUsed += usageInfo.energyAvailable;
                usageInfo.energyAvailable = 0L;
                return absorption * absorbedPercent;
            }
        }
        return 0;
    }

    @Nullable
    private static <MODULE extends ICustomModule<MODULE>> ICustomModule.ModuleDamageAbsorbInfo getModuleDamageAbsorbInfo(IModule<MODULE> module, DamageSource damageSource) {
        return module.getCustomInstance().getDamageAbsorbInfo(module, damageSource);
    }

    private static class EnergyUsageInfo {
        private long energyAvailable;
        private long energyUsed = 0;

        public EnergyUsageInfo(long energyAvailable) {
            this.energyAvailable = energyAvailable;
        }
    }


    @Nullable
    public final ILaserDissipation getLaserCapability() {
        if (IModuleHelper.INSTANCE.getIfEnabled(this.resolveBoundStack(), MekanismModules.LASER_DISSIPATION_UNIT) != null) {
            return LaserDissipationHandler.create(0.75, 1.00);
        }
        return null;
    }

    @Override
    public @NonNull InteractionResult mobInteract(@NonNull Player player, @NonNull InteractionHand hand) {
        if (!this.level().isClientSide && player == getOwner() && !isSwarmMinion()) {
            if (player.isShiftKeyDown()) {
                this.discard();
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    @Nullable
    public static EntityMekaRobit findByUuid(Level level, UUID uuid) {
        if (level instanceof ServerLevel serverLevel) {
            net.minecraft.world.entity.Entity e = serverLevel.getEntity(uuid);
            return e instanceof EntityMekaRobit r && !r.isRemoved() ? r : null;
        }
        return null;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (getOwnerUUID() != null) tag.putUUID("Owner", getOwnerUUID());
        tag.putByte("Mode", getMode());
        tag.putBoolean("HoverLeft", isHoverLeft());

        tag.putBoolean("IsSwarmMinion", isSwarmMinion());
        if (mainDroneUuid != null) tag.putUUID("MainDroneUuid", mainDroneUuid);
        tag.putInt("FormationIndex", getFormationIndex());
        tag.putInt("FormationSize", getFormationSize());

        if (homeLocation != null) {
            Optional<Tag> result = GlobalPos.CODEC.encodeStart(registryAccess().createSerializationContext(NbtOps.INSTANCE), homeLocation).result();
            //noinspection OptionalIsPresent - Capturing lambda
            if (result.isPresent()) {
                tag.put(SerializationConstants.HOME_LOCATION, result.get());
            }
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NBTUtils.setUUIDIfPresent(tag, "Owner", uuid -> {
            Player player = this.level().getPlayerByUUID(uuid);
            if (player != null) {
                this.setOwner(player);
            } else {
                this.setOwnerUUID(uuid);
            }
        });
        NBTUtils.setByteIfPresent(tag, "Mode", this::setMode);
        NBTUtils.setBooleanIfPresent(tag, "HoverLeft", this::setHoverLeft);

        NBTUtils.setBooleanIfPresent(tag, "IsSwarmMinion", this::setSwarmMinion);
        NBTUtils.setUUIDIfPresent(tag, "MainDroneUuid", uuid -> this.mainDroneUuid = uuid);
        NBTUtils.setIntIfPresent(tag, "FormationIndex", this::setFormationIndex);
        NBTUtils.setIntIfPresent(tag, "FormationSize", this::setFormationSize);

        NBTUtils.setCompoundIfPresent(tag, SerializationConstants.HOME_LOCATION, home -> homeLocation = GlobalPos.CODEC.parse(registryAccess().createSerializationContext(NbtOps.INSTANCE), home).result().orElse(null));
    }

    @Override
    public void onContentsChanged() {}

    public void clearBoundStackMarker() {
        ItemStack stack = resolveBoundStack();
        if (!stack.isEmpty()) {
            stack.remove(FNDataComponents.MEKA_ROBIT_BOUND_ENTITY.get());
            invalidateBoundStackCache();
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && !isSwarmMinion()) {
            clearBoundStackMarker();
            this.swarmState.cachedSwarm.forEach(Entity::discard);
        }
        super.remove(reason);
    }

    @Override
    public void aiStep() {
        if (this.level().isClientSide && isLocallyPiloted() && getMode() == MODE_FULL_CONTROL) {
            this.lerpSteps = 0;
            this.travel(Vec3.ZERO);
            return;
        }
        super.aiStep();
    }

    @Override
    @ParametersAreNonnullByDefault
    public void travel(Vec3 travelVector) {
        if (getMode() == MODE_FULL_CONTROL) {
            float yaw   = Mth.wrapDegrees(this.clientYRot);
            float pitch = Mth.clamp(this.clientXRot, -90F, 90F);

            Vec3 forward = Vec3.directionFromRotation(pitch, yaw);
            Vec3 right   = forward.cross(new Vec3(0, 1, 0));
            if (right.lengthSqr() < 1.0E-6D) {
                right = new Vec3(Mth.cos(-yaw * ((float)Math.PI / 180F) - (float)Math.PI), 0,
                    Mth.sin(-yaw * ((float)Math.PI / 180F) - (float)Math.PI));
            }
            right = right.normalize();

            Vec3 motion = forward.scale(this.clientForward)
                .add(right.scale(this.clientStrafe))
                .add(0, this.clientUp, 0)
                .scale(0.5D);

            this.setDeltaMovement(motion);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.8D));
        } else if (this.isEffectiveAi()) {
            this.moveRelative(this.getSpeed(), new Vec3(0, this.yya, this.zza));
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.4D));
        } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.8D));
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (isLocallyPiloted()) {
            double dx = x - getX();
            double dy = y - getY();
            double dz = z - getZ();
            double errorSq = dx * dx + dy * dy + dz * dz;

            if (errorSq > 1.0) {
                this.setPos(x, y, z);
                this.setDeltaMovement(Vec3.ZERO);
            }
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, steps);
    }

    @Override
    public void lerpHeadTo(float yHeadRot, int steps) {
        if (isLocallyPiloted()) return;
        super.lerpHeadTo(yHeadRot, steps);
    }

    @Override
    public boolean isPushable() {
        if (this.isSwarmMinion()) return false;
        return super.isPushable();
    }

    @Override
    public boolean canCollideWith(@NonNull Entity entity) {
        return entity != this.getOwner() && (entity instanceof EntityMekaRobit robit && !robit.isSwarmMinion());
    }

    @Override
    public boolean canBeCollidedWith() {
        if (this.isSwarmMinion()) return false;
        return super.canBeCollidedWith();
    }

    public boolean isLocallyPiloted() {
        return level().isClientSide && ClientPilotCheck.isCameraEntity(this);
    }

    public Vec3 getRandomPositionAround(Vec3 pos, float minRange, float maxRange, int steps) {
        for (int i = 0; i < steps; i++) {
            double angle = Math.random() * Math.PI * 2;
            double dist = minRange + Math.random() * (maxRange - minRange);
            double x = pos.x + Math.cos(angle) * dist;
            double z = pos.z + Math.sin(angle) * dist;
            double y = pos.y + (Math.random() * (maxRange - minRange) - (maxRange - minRange) / 2.0);

            if (y < this.level().getMinBuildHeight() + 1 || y > this.level().getMaxBuildHeight() - 1)
                continue;

            if (this.level().noCollision(this.getType().getDimensions().makeBoundingBox(x, y, z)))
                return new Vec3(x, y, z);
        }

        for (int i = 1; i <= 5; i++) {
            if (this.level().noCollision(this.getType().getDimensions().makeBoundingBox(pos.x, pos.y + i, pos.z)))
                return pos.add(0, i, 0);
        }

        return pos;
    }

    private static final class ClientPilotCheck {
        static boolean isCameraEntity(Entity entity) {
            return FMLEnvironment.dist == Dist.CLIENT && ClientPilotCheckClient.isCameraEntity(entity);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientPilotCheckClient {
        static boolean isCameraEntity(Entity entity) {
            return net.minecraft.client.Minecraft.getInstance().getCameraEntity() == entity;
        }
    }

    public static class GoalLaserAttack extends Goal {
        private final EntityMekaRobit drone;
        private final float maxRange;
        private int attackTime = 0;
        private int teleportCooldown = 0;

        public GoalLaserAttack(EntityMekaRobit drone, float maxRange) {
            this.drone = drone;
            this.maxRange = maxRange;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            IEnergyContainer energyContainer = this.drone.getEnergyContainer();
            if (energyContainer == null || energyContainer.isEmpty())
                return false;
            LivingEntity target = drone.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            IEnergyContainer energyContainer = this.drone.getEnergyContainer();
            if (energyContainer == null || energyContainer.isEmpty())
                return false;
            return drone.getTarget() != null && drone.getTarget().isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = drone.getTarget();
            if (target == null) return;

            drone.setTargetId(target.getId());

            if (teleportCooldown > 0) teleportCooldown--;

            double distSqr = drone.distanceToSqr(target);
            boolean hasLOS = drone.getSensing().hasLineOfSight(target);

            double desiredDist = 14.0D;
            if (distSqr < desiredDist * desiredDist) {
                Vec3 away = drone.position().subtract(target.position()).normalize();
                Vec3 dest = drone.position().add(away.scale(2.0D));
                drone.getMoveControl().setWantedPosition(dest.x, dest.y, dest.z, 1.5D);
            } else if (!hasLOS || distSqr > maxRange * maxRange) {
                if (teleportCooldown == 0) {
                    tryTeleportToEntity(target);
                    teleportCooldown = 60;
                }
            }

            drone.getLookControl().setLookAt(target, 90.0F, 90.0F);

            if (target.invulnerableTime > 0) {
                List<Monster> otherTargets = drone.level().getEntitiesOfClass(Monster.class,
                    drone.getBoundingBox().inflate(maxRange),
                    m -> m != target && m.isAlive() && !drone.isAlliedTo(m) && m.invulnerableTime == 0 && drone.getSensing().hasLineOfSight(m));

                if (!otherTargets.isEmpty()) {
                    drone.setTarget(otherTargets.getFirst());
                    return;
                }
            }

            attackTime++;
            if (distSqr < maxRange * maxRange && hasLOS) {
                if (attackTime < 5) {
                    drone.setCombatState(STATE_ATTACK_START);
                } else {
                    drone.setCombatState(STATE_ATTACK_ONGOING);

                    if (attackTime % 4 == 0) {
                        boolean isLeader = (!drone.isSwarmMinion()) || (drone.getMainDroneId() == -1 && drone.getFormationIndex() == 0);
                        AtomicReference<Float> totalDmg = new AtomicReference<>((float) 0);

                        if (isLeader) {
                            totalDmg.set(drone.getPotentialDamage());
                            ModuleSwarmUnit.forEachSwarmMember(drone, drone.swarmState, (r) -> {
                                if (r.isSwarmMinion() && r.getMainDroneId() == drone.getId()) {
                                    totalDmg.updateAndGet(v -> v + r.getPotentialDamage());
                                } else if (drone.getMainDroneId() == -1 && r.isSwarmMinion() && r.getMainDroneId() == -1) {
                                    totalDmg.updateAndGet(v -> v + r.getPotentialDamage());
                                }
                            });

                            boolean shouldFocus = target.getHealth() / totalDmg.get() > 0.5f;
                            drone.setFocusFiring(shouldFocus);

                            ModuleSwarmUnit.forEachSwarmMember(drone, drone.swarmState, (r) -> r.setFocusFiring(shouldFocus));
                        }

                        if (drone.isFocusFiring() && !isLeader) {
                            return;
                        }

                        float damageToDeal = drone.isFocusFiring() ? totalDmg.get() : drone.getPotentialDamage();
                        float damage = drone.extractEnergyForDamage(damageToDeal);
                        if (damage == 0 || !drone.dealDamageTo(target, damage)) {
                            drone.setCombatState(STATE_ATTACK_END);
                        }
                    }
                }
            } else {
                if (drone.getCombatState() == STATE_ATTACK_START || drone.getCombatState() == STATE_ATTACK_ONGOING) {
                    drone.setCombatState(STATE_ATTACK_END);
                }
                attackTime = 0;
            }
        }

        @Override
        public void stop() {
            drone.setCombatState(STATE_ATTACK_END);
            drone.setTargetId(-1);
            drone.setFocusFiring(false);
            attackTime = 0;
        }

        private void tryTeleportToEntity(LivingEntity target) {
            Vec3 targetPos = target.position();
            Vec3 dest = drone.getRandomPositionAround(targetPos, 4f, 6f, 10);
            if (dest == null) return;
            spawnTeleportParticles(drone.position());
            drone.teleportTo(dest.x, dest.y, dest.z);
            spawnTeleportParticles(dest);
        }

        private void spawnTeleportParticles(Vec3 pos) {
            if (drone.level() instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 20; i++) {
                    double dx = (Math.random() - 0.5) * 0.5;
                    double dy = (Math.random() - 0.5) * 0.5;
                    double dz = (Math.random() - 0.5) * 0.5;
                    serverLevel.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y, pos.z, 1, dx, dy, dz, 0.1);
                }
            }
        }
    }

    public static class GoalIdlePatrol extends Goal {
        private final EntityMekaRobit drone;
        private final double speedModifier;

        private Vec3 patrolPos;
        private int timeout = 0;
        private int pauseTicks = 0;
        private int idleTicks = 0;

        private int currentPointIndex = 0;
        private int waitTicks = 0;

        public GoalIdlePatrol(EntityMekaRobit drone, double speedModifier) {
            this.drone = drone;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean isPlayerMoving(Player owner) {
            double dx = owner.getX() - owner.xOld;
            double dz = owner.getZ() - owner.zOld;
            return (dx * dx + dz * dz) > 0.0001D;
        }

        @Override
        public boolean canUse() {
            if (this.drone.getMode() != MODE_FOLLOW)
                return false;

            if (this.drone.isBoundToBlock())
                return !this.drone.isAssisting;

            Player owner = this.drone.getOwner();
            if (owner == null)
                return false;

            if (isPlayerMoving(owner)) {
                idleTicks = 0;
                return false;
            }
            if (owner.distanceToSqr(this.drone) > 22.0 * 22.0) {
                idleTicks = 0;
                return false;
            }

            idleTicks++;
            return idleTicks > 200;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.drone.getMode() != MODE_FOLLOW)
                return false;

            if (this.drone.isBoundToBlock())
                return true;

            Player owner = this.drone.getOwner();
            if (owner == null)
                return false;
            if (isPlayerMoving(owner))
                return false;
            if (owner.distanceToSqr(this.drone) > 20.0 * 20.0)
                return false;

            return this.timeout > 0;
        }

        @Override
        public void start() {
            this.timeout = 200;
            if (!this.drone.isBoundToBlock()) {
                this.patrolPos = this.getRandomPointAroundOwner();
                if (this.patrolPos == null) {
                    this.patrolPos = this.drone.getEyePosition().add(0, 10, 0);
                }
                this.drone.getMoveControl().setWantedPosition(this.patrolPos.x, this.patrolPos.y, this.patrolPos.z, this.speedModifier);
            }
        }

        @Override
        public void stop() {
            this.idleTicks = 0;
            this.patrolPos = null;
        }

        @Override
        public void tick() {
            this.timeout--;

            if (this.drone.isBoundToBlock()) {
                tickBlockPatrol();
                return;
            }

            if (this.pauseTicks > 0) {
                this.pauseTicks--;
                this.drone.getMoveControl().setWantedPosition(this.drone.getX(), this.drone.getY(), this.drone.getZ(), 0.0D);
                return;
            }

            if (this.patrolPos == null || this.drone.distanceToSqr(this.patrolPos) < 0.5D || this.timeout <= 0) {
                this.pauseTicks = 40 + this.drone.getRandom().nextInt(60);
                this.timeout = 100;
                this.patrolPos = this.getRandomPointAroundOwner();
            }

            if (this.patrolPos != null) {
                this.drone.getMoveControl().setWantedPosition(this.patrolPos.x, this.patrolPos.y, this.patrolPos.z, this.speedModifier);
                this.drone.getLookControl().setLookAt(this.patrolPos.x, this.patrolPos.y, this.patrolPos.z);
            }
        }

        private void tickBlockPatrol() {
            this.drone.getHomeBlock().ifPresent((dock) -> {
                PatrolConfig config;
                config = dock.getPatrolConfig();

                if (config == null) return;

                if (this.waitTicks > 0) {
                    this.waitTicks--;
                    this.drone.getMoveControl().setWantedPosition(this.drone.getX(), this.drone.getY(), this.drone.getZ(), 0.0D);
                    return;
                }

                if (config.isPoints() && !config.points().isEmpty()) {
                    if (this.patrolPos == null || this.drone.distanceToSqr(this.patrolPos) < 0.5D || this.timeout <= 0) {
                        this.currentPointIndex = (this.currentPointIndex + 1) % config.points().size();
                        BlockPos point = config.points().get(this.currentPointIndex);
                        this.patrolPos = new Vec3(point.getX() + 0.5, point.getY() + 0.5, point.getZ() + 0.5);

                        if (this.timeout <= 0) {
                            this.waitTicks = config.waitTicks();
                            this.timeout = 200;
                        } else {
                            this.timeout = 200;
                        }
                    }
                } else {
                    BlockPos pos = dock.getWanderCenter().pos();
                    if (this.patrolPos == null || this.drone.distanceToSqr(this.patrolPos) < 0.5D || this.timeout <= 0) {
                        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        this.patrolPos = this.drone.getRandomPositionAround(center, 2.0f, Math.max(2.0f, config.range()), 10);
                        if (this.patrolPos == null) {
                            this.patrolPos = center;
                        }
                        this.patrolPos = Sable.HELPER.projectOutOfSubLevel(this.drone.level(), this.patrolPos);
                        this.timeout = 100;
                        this.waitTicks = config.waitTicks();
                    }
                }

                if (this.patrolPos != null) {
                    this.drone.getMoveControl().setWantedPosition(this.patrolPos.x, this.patrolPos.y, this.patrolPos.z, this.speedModifier);
                    this.drone.getLookControl().setLookAt(this.patrolPos.x, this.patrolPos.y, this.patrolPos.z);
                }
            });
        }

        private Vec3 getRandomPointAroundOwner() {
            Player owner = this.drone.getOwner();
            assert owner != null;
            return drone.getRandomPositionAround(owner.getPosition(0f), 16f, 32f, 10);
        }
    }

    public static class GoalFollowMainDrone extends Goal {
        private final EntityMekaRobit drone;
        private final double speedModifier;
        private int idleTicks = 0;
        private final Pose3d pose =  new Pose3d();

        public GoalFollowMainDrone(EntityMekaRobit drone, double speedModifier) {
            this.drone = drone;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!drone.isSwarmMinion()) return false;

            if (drone.getMainDroneId() != -1) {
                EntityMekaRobit main = drone.getMainDrone();
                return main != null && main.isAlive();
            } else {
                Player owner = drone.getOwner();
                return owner != null && owner.isAlive();
            }
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity leader;
            EntityMekaRobit main = drone.getMainDrone();
            boolean usePlayerFormation = (drone.isAssisting && main != null && main.getMode() != MODE_FULL_CONTROL) || drone.getMainDroneId() == -1;

            if (usePlayerFormation) {
                Player owner = drone.getOwner();
                if (owner == null) return;
                leader = owner;
            } else {
                if (main == null) return;
                leader = main;
            }

            int total = drone.getFormationSize();
            int index = drone.getFormationIndex();
            if (total <= 0) total = 1;

            boolean isIdle = leader.getDeltaMovement().lengthSqr() < 0.0025D;
            if (isIdle) {
                idleTicks++;
            } else {
                idleTicks = 0;
            }

            ModuleSwarmUnit.SwarmFormation formation = drone.getSwarmFormation();
            formation.getFormationPose(leader, drone, total, index, isIdle, usePlayerFormation, idleTicks, pose);
            Vector3d dest = pose.position();

            drone.getMoveControl().setWantedPosition(dest.x, dest.y, dest.z, speedModifier);

            if (!drone.isAssisting) {
                Vector3d view = pose.rotationPoint();
                drone.getLookControl().setLookAt(view.x, view.y, view.z);
            }
        }
    }

    public static class GoalFollowOwner extends Goal {
        private final EntityMekaRobit drone;
        private final double speedModifier;
        private final float startDistance;
        private final float stopDistance;

        public GoalFollowOwner(EntityMekaRobit drone, double speedModifier, float startDistance, float stopDistance) {
            this.drone = drone;
            this.speedModifier = speedModifier;
            this.startDistance = startDistance;
            this.stopDistance = stopDistance;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean isPlayerMoving(Player owner) {
            double dx = owner.getX() - owner.xOld;
            double dz = owner.getZ() - owner.zOld;
            return (dx * dx + dz * dz) > 0.0001D;
        }

        @Override
        public boolean canUse() {
            if (this.drone.isBoundToBlock())
                return false;

            Player owner = this.drone.getOwner();
            if (owner == null || this.drone.getMode() != MODE_FOLLOW) return false;

            if (this.drone.isAssisting) return true;

            if (isPlayerMoving(owner)) return true;
            return owner.distanceToSqr(this.drone) > startDistance * startDistance;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.drone.isBoundToBlock())
                return false;

            Player owner = this.drone.getOwner();
            if (owner == null || this.drone.getMode() != MODE_FOLLOW) return false;

            if (this.drone.isAssisting) return true;

            return isPlayerMoving(owner) || owner.distanceToSqr(this.drone) > startDistance * startDistance;
        }

        @Override
        public void tick() {
            Player owner = this.drone.getOwner();
            if (owner != null) {
                Vec3 targetPos;

                if (this.drone.isAssisting) {
                    Vec3 lookVec = owner.getLookAngle();
                    targetPos = owner.getEyePosition().subtract(lookVec.scale(2.0)).add(0, 1.5, 0);
                } else {
                    Vec3 lookVec = owner.getLookAngle();
                    Vec3 flatLook = new Vec3(lookVec.x, 0, lookVec.z);
                    if (flatLook.lengthSqr() < 0.001D) {
                        flatLook = new Vec3(Math.sin(-owner.getYRot() * 0.017453292F), 0, Math.cos(owner.getYRot() * 0.017453292F));
                    }
                    flatLook = flatLook.normalize();

                    Vec3 rightVec = flatLook.cross(new Vec3(0, 1, 0)).normalize();
                    if (this.drone.isHoverLeft()) {
                        rightVec = rightVec.scale(-1);
                    }

                    targetPos = owner.position().add(flatLook.scale(-3).add(0, 3.0, 0).add(rightVec.scale(3.0)));
                }

                double distToTarget = this.drone.distanceToSqr(targetPos);

                if (distToTarget > stopDistance * stopDistance || !this.drone.getMoveControl().hasWanted())
                    this.drone.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, this.speedModifier);

                if (!this.drone.isAssisting && !this.drone.isAlerted())
                    this.drone.getLookControl().setLookAt(owner.getX(), owner.getY() + owner.getEyeHeight(), owner.getZ());
            }
        }
    }

    public static class GoalTeleportToOwner extends Goal {
        private final EntityMekaRobit drone;
        private final double maxDistance;

        private int checkCooldown = 0;
        private double lastX, lastY, lastZ;
        private int stuckTicks = 0;

        public GoalTeleportToOwner(EntityMekaRobit drone, double maxDistance) {
            this.drone = drone;
            this.maxDistance = maxDistance;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.drone.isBoundToBlock())
                return false;

            Player owner = this.drone.getOwner();
            EntityMekaRobit main = this.drone.getMainDrone();
            if (owner == null || this.drone.getMode() != MODE_FOLLOW || ( main != null && main.getMode() != MODE_FOLLOW))
                return false;

            double distSqr = this.drone.distanceToSqr(owner);

            if (distSqr > this.maxDistance * this.maxDistance) return true;

            if (distSqr > 10.0 * 10.0) {
                if (this.checkCooldown <= 0) {
                    this.checkCooldown = 20;
                    double dx = this.drone.getX() - this.lastX;
                    double dy = this.drone.getY() - this.lastY;
                    double dz = this.drone.getZ() - this.lastZ;
                    double movedDist = dx * dx + dy * dy + dz * dz;

                    if (movedDist < 0.5D * 0.5D) {
                        this.stuckTicks++;
                    } else {
                        this.stuckTicks = 0;
                    }

                    this.lastX = this.drone.getX();
                    this.lastY = this.drone.getY();
                    this.lastZ = this.drone.getZ();
                } else {
                    this.checkCooldown--;
                }

                return this.stuckTicks >= 3;
            } else {
                this.stuckTicks = 0;
            }

            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            Player owner = this.drone.getOwner();
            if (owner == null) return;

            Vec3 lookVec = owner.getLookAngle();
            Vec3 flatLook = new Vec3(lookVec.x, 0, lookVec.z);
            if (flatLook.lengthSqr() < 0.001D) {
                flatLook = new Vec3(Math.sin(-owner.getYRot() * 0.017453292F), 0, Math.cos(owner.getYRot() * 0.017453292F));
            }
            flatLook = flatLook.normalize();

            Vec3 rightVec = flatLook.cross(new Vec3(0, 1, 0)).normalize();
            if (this.drone.isHoverLeft()) {
                rightVec = rightVec.scale(-1);
            }

            Vec3 offset = flatLook.scale(-2.5).add(0, 2.0, 0).add(rightVec.scale(2.0));
            Vec3 targetPos = owner.position().add(offset);

            this.drone.setDeltaMovement(Vec3.ZERO);
            this.drone.getMoveControl().setWantedPosition(this.drone.getX(), this.drone.getY(), this.drone.getZ(), 0.0D);
            this.drone.teleportTo(targetPos.x, targetPos.y, targetPos.z);

            this.stuckTicks = 0;
            this.lastX = targetPos.x;
            this.lastY = targetPos.y;
            this.lastZ = targetPos.z;
        }
    }

    public static class GoalHurtBy extends TargetGoal {
        private final EntityMekaRobit drone;
        private LivingEntity hurtBy;
        private int timestamp;

        public GoalHurtBy(EntityMekaRobit drone) {
            super(drone, false);
            this.drone = drone;
        }

        @Override
        public boolean canUse() {
            int i = this.drone.getLastHurtByMobTimestamp();
            LivingEntity livingentity = this.drone.getLastHurtByMob();
            if (i != this.timestamp && livingentity != null && !this.drone.isAlliedTo(livingentity)) {
                this.hurtBy = livingentity;
                return true;
            }
            return false;
        }

        @Override
        public void start() {
            this.drone.setTarget(this.hurtBy);
            this.timestamp = this.drone.getLastHurtByMobTimestamp();
            super.start();
        }
    }

    public static class GoalOwnerHurtBy extends TargetGoal {
        private final EntityMekaRobit drone;
        private LivingEntity ownerLastHurtBy;
        private int timestamp;

        public GoalOwnerHurtBy(EntityMekaRobit drone) {
            super(drone, false);
            this.drone = drone;
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = drone.getOwner();
            if (owner == null) return false;

            this.ownerLastHurtBy = owner.getLastHurtByMob();
            int i = owner.getLastHurtByMobTimestamp();

            return i != timestamp && ownerLastHurtBy != null && !this.drone.isAlliedTo(ownerLastHurtBy);
        }

        @Override
        public void start() {
            drone.setTarget(ownerLastHurtBy);
            LivingEntity owner = drone.getOwner();
            if (owner != null) timestamp = owner.getLastHurtByMobTimestamp();
            super.start();
        }
    }

    public static class GoalOwnerHurtTarget extends TargetGoal {
        private final EntityMekaRobit drone;
        private LivingEntity ownerLastHurtTarget;
        private int timestamp;

        public GoalOwnerHurtTarget(EntityMekaRobit drone) {
            super(drone, false);
            this.drone = drone;
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = drone.getOwner();
            if (owner == null) return false;

            this.ownerLastHurtTarget = owner.getLastHurtMob();
            int i = owner.getLastHurtMobTimestamp();

            return i != timestamp && ownerLastHurtTarget != null && !this.drone.isAlliedTo(ownerLastHurtTarget);
        }

        @Override
        public void start() {
            drone.setTarget(ownerLastHurtTarget);
            LivingEntity owner = drone.getOwner();
            if (owner != null) timestamp = owner.getLastHurtMobTimestamp();
            super.start();
        }
    }

    public static class GoalSmartAttackTarget extends TargetGoal {
        private final EntityMekaRobit drone;
        private final float range;
        private LivingEntity target;
        private int cooldown = 0;

        public GoalSmartAttackTarget(EntityMekaRobit drone, float range) {
            super(drone, true, false);
            this.drone = drone;
            this.range = range;
        }

        @Override
        public boolean canUse() {
            if (cooldown++ < 2)
                return false;
            cooldown = 0;

            Optional<IMekaRobitDock> dock = this.drone.getHomeBlock();

            List<LivingEntity> list = this.drone.level().getEntitiesOfClass(LivingEntity.class,
                this.drone.getBoundingBox().inflate(range),
                m -> {
                    if (!m.isAlive())
                        return false;

                    if (m instanceof Player player && (player.isCreative() || player.isSpectator()))
                        return false;

                    if (m instanceof Monster)
                        return true;

                    if (this.drone.isAlliedTo(m))
                        return false;

                    return dock.map((d) -> d.isEnemy(m)).orElse(false);
                });


            if (list.isEmpty()) return false;

            Player owner = this.drone.getOwner();

            list.sort((e1, e2) -> {
                int invuln1 = e1.invulnerableTime > 0 ? 1 : 0;
                int invuln2 = e2.invulnerableTime > 0 ? 1 : 0;
                if (invuln1 != invuln2) {
                    return Integer.compare(invuln1, invuln2);
                }

                if (owner != null) {
                    double distOwner1 = e1.distanceToSqr(owner);
                    double distOwner2 = e2.distanceToSqr(owner);
                    int distCmp = Double.compare(distOwner1, distOwner2);
                    if (distCmp != 0) {
                        return distCmp;
                    }
                }

                int hpCmp = Float.compare(e1.getHealth(), e2.getHealth());
                if (hpCmp != 0) {
                    return hpCmp;
                }

                return Double.compare(e1.distanceToSqr(this.drone), e2.distanceToSqr(this.drone));
            });

            this.target = list.getFirst();
            return true;
        }

        @Override
        public void start() {
            this.drone.setTarget(target);
            super.start();
        }
    }

    public static class MoveControlHover extends MoveControl {
        public MoveControlHover(Mob pMob) {
            super(pMob);
        }

        @Override
        public void tick() {
            if (this.operation == Operation.MOVE_TO) {
                double dx = this.wantedX - this.mob.getX();
                double dy = this.wantedY - this.mob.getY();
                double dz = this.wantedZ - this.mob.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (dist < 0.5D) {
                    this.operation = Operation.WAIT;
                    this.mob.setZza(0.0F);
                    this.mob.setYya(0.0F);
                    this.mob.setDeltaMovement(this.mob.getDeltaMovement().scale(0.2D));
                    return;
                }

                double horizontalDist = Math.sqrt(dx * dx + dz * dz);

                float targetYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
                float targetXRot = (float)(-(Math.toDegrees(Math.atan2(dy, Math.max(horizontalDist, 0.3D)))));

                this.mob.setYRot(this.rotlerp(this.mob.getYRot(), targetYaw, 20.0F));
                this.mob.setXRot(this.rotlerp(this.mob.getXRot(), targetXRot, 20.0F));

                float yawDiff = Mth.wrapDegrees(targetYaw - this.mob.getYRot());
                double speed = this.speedModifier;

                if (Math.abs(yawDiff) > 45.0F) {
                    speed *= 0.2D;
                } else if (dist < 3.0D) {
                    speed *= (dist / 3.0D);
                }

                if (speed < 0.1D && dist > 0.5D)
                    speed = 0.1D;

                this.mob.setSpeed((float) speed);
                this.mob.setZza((float) speed);

                this.mob.setYya((float)(dy * 0.3D));
            } else {
                this.mob.setZza(0.0F);
                this.mob.setYya(0.0F);
            }
        }
    }
}