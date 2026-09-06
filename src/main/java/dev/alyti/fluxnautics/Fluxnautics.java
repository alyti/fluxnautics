package dev.alyti.fluxnautics;

import dev.alyti.fluxnautics.capabilities.Capabilities;
import dev.alyti.fluxnautics.content.blocks.cloak.CloakGeneratorEvents;
import dev.alyti.fluxnautics.datagen.DatagenHelper;
import dev.alyti.fluxnautics.datagen.lang.LangProvider;
import dev.alyti.fluxnautics.datagen.loot.LootProvider;
import dev.alyti.fluxnautics.datagen.recipe.RecipeProvider;
import dev.alyti.fluxnautics.datagen.registries.DatapackRegistryProvider;
import dev.alyti.fluxnautics.datagen.sound.FNSoundProvider;
import dev.alyti.fluxnautics.datagen.tag.TagProvider;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayCache;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayMultiblockData;
import dev.alyti.fluxnautics.multiblock.ThrusterArrayValidator;
import dev.alyti.fluxnautics.network.FluxPacketHandler;
import dev.alyti.fluxnautics.registry.*;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import mekanism.api.MekanismIMC;
import mekanism.api.gear.ModuleData;
import mekanism.common.Mekanism;
import mekanism.common.base.IModModule;
import mekanism.common.lib.Version;
import mekanism.common.lib.multiblock.MultiblockManager;
import mekanism.common.registries.MekanismModules;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.concurrent.CompletableFuture;

@Mod(Fluxnautics.MODID)
public class Fluxnautics implements IModModule {
    public static final String MODID = "fluxnautics";

    public static Fluxnautics instance;

    private final FluxPacketHandler packetHandler;
    public final Version versionNumber;

    public static final MultiblockManager<ThrusterArrayMultiblockData> thrusterArrayManager = new MultiblockManager<>("thrusterArray", ThrusterArrayCache::new, ThrusterArrayValidator::new);

    public Fluxnautics(IEventBus modEventBus, ModContainer modContainer) {
        Mekanism.addModule(instance = this);
        FluxnauticsConfig.registerConfigs(modContainer);

        FNDataComponents.DATA_COMPONENTS.register(modEventBus);
        FNItems.ITEMS.register(modEventBus);
        FNBlocks.BLOCKS.register(modEventBus);
        FNBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        FNEntityTypes.ENTITY_TYPES.register(modEventBus);
        FNDataMapTypes.REGISTER.register(modEventBus);
        FNSounds.SOUND_EVENTS.register(modEventBus);
        FNContainerTypes.CONTAINER_TYPES.register(modEventBus);
        FNModules.MODULES.register(modEventBus);
        FNCreativeTab.register(modEventBus);

        modEventBus.addListener(EventPriority.LOWEST, Fluxnautics::gatherData);

        modEventBus.addListener(this::imcQueue);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(FluxnauticsConfig::onLoad);
        modEventBus.addListener(this::registerCapabilities);

        IEventBus neoEventBus = NeoForge.EVENT_BUS;
        neoEventBus.register(CloakGeneratorEvents.class);

        versionNumber = new Version(modContainer);
        packetHandler = new FluxPacketHandler(modEventBus, versionNumber);
    }

    public static void gatherData(GatherDataEvent event) {
        DatagenHelper.bootstrapConfigs(Fluxnautics.MODID);
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        DatapackRegistryProvider drProvider = new DatapackRegistryProvider(output, event.getLookupProvider());
        CompletableFuture<HolderLookup.Provider> lookupProvider = drProvider.getRegistryProvider();
        gen.addProvider(event.includeClient(), new LangProvider(output));
        gen.addProvider(event.includeClient(), new FNSoundProvider(output, existingFileHelper));
        gen.addProvider(event.includeServer(), new LootProvider(output, lookupProvider));
        gen.addProvider(event.includeServer(), new RecipeProvider(output, lookupProvider, existingFileHelper));
        gen.addProvider(event.includeServer(), new DatapackRegistryProvider(output, lookupProvider));
        gen.addProvider(event.includeServer(), new TagProvider(output, lookupProvider, existingFileHelper));
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        SableEventPlatform.INSTANCE.onPhysicsTick(FluxnauticsSableCommonEvents::onPhysicsTick);
        SableEventPlatform.INSTANCE.onPostPhysicsTick(FluxnauticsSableCommonEvents::onPostPhysicsTick);
        SableEventPlatform.INSTANCE.onSubLevelContainerReady(FluxnauticsSableCommonEvents::onContainerReady);
    }

    private final static String ADD_MEKA_ROBIT_MODULES = "add_meka_robit_modules";

    private void imcQueue(InterModEnqueueEvent event) {
        MekanismIMC.addMekaSuitHelmetModules(FNModules.DECLOAKER);
        MekanismIMC.addMekaSuitBodyarmorModules(FNModules.SWARM_UNIT, MekanismModules.ATTACK_AMPLIFICATION_UNIT);
        MekanismIMC.addMekaToolModules(FNModules.FIELD_MANIPULATOR_UNIT);

        MekanismIMC.addModuleContainer((Holder<Item>) FNItems.MEKA_ROBIT, ADD_MEKA_ROBIT_MODULES);
        addMekaRobitModules(
            MekanismModules.ENERGY_UNIT,
            MekanismModules.ATTACK_AMPLIFICATION_UNIT,
            MekanismModules.LASER_DISSIPATION_UNIT,
            MekanismModules.RADIATION_SHIELDING_UNIT,
            MekanismModules.FORTUNE_UNIT,
            MekanismModules.BLASTING_UNIT,
            MekanismModules.SILK_TOUCH_UNIT,
            MekanismModules.VEIN_MINING_UNIT,
            FNModules.SWARM_UNIT,
            FNModules.QUANTUM_PULL_UNIT
        );
    }

    @SafeVarargs
    public static void addMekaRobitModules(Holder<ModuleData<?>>... moduleDataProviders) {
        MekanismIMC.sendModuleIMC(ADD_MEKA_ROBIT_MODULES, moduleDataProviders);
    }

    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(Capabilities.LASER_DISSIPATION_ENTITY, FNEntityTypes.MEKA_ROBIT.get(), (e, context) -> e.getLaserCapability());

        for (BlockCapability<?, ?> blockCapability : BlockCapability.getAll()) {
            event.registerBlockEntity(
                (BlockCapability) blockCapability, FNBlockEntityTypes.CONDUIT_PORT.get(),
                (blockEntity, context) -> blockEntity.getCapability((BlockCapability) blockCapability, context)
            );
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {}

    @Override
    public Version getVersion() {
        return versionNumber;
    }

    @Override
    public String getName() {
        return MODID;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
