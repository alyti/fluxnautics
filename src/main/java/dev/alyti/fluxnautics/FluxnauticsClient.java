package dev.alyti.fluxnautics;

import dev.alyti.fluxnautics.api.render.MachineISTER;
import dev.alyti.fluxnautics.client.gui.*;
import dev.alyti.fluxnautics.client.gui.hr.GuiForcefieldHarmonizerScreen;
import dev.alyti.fluxnautics.client.render.MekaRobitItemRenderer;
import dev.alyti.fluxnautics.client.render.SFRenderTypes;
import dev.alyti.fluxnautics.client.render.ThrusterTrailManager;
import dev.alyti.fluxnautics.client.render.cloak.CloakFadePreProcessor;
import dev.alyti.fluxnautics.client.render.field.FieldFadeTransformer;
import dev.alyti.fluxnautics.client.render.field.FieldShadowRenderer;
import dev.alyti.fluxnautics.client.render.model.MekaRobit;
import dev.alyti.fluxnautics.content.blocks.antigrav.AntiGravRenderer;
import dev.alyti.fluxnautics.content.blocks.conduit.ConduitConnectorRenderer;
import dev.alyti.fluxnautics.content.blocks.conduit.ConduitPortModelLoader;
import dev.alyti.fluxnautics.content.blocks.daemon_protocol.DaemonProtocolRenderer;
import dev.alyti.fluxnautics.content.blocks.field.FieldDriveRenderer;
import dev.alyti.fluxnautics.content.blocks.forcefield.ForcefieldEmitterRenderer;
import dev.alyti.fluxnautics.content.blocks.holomap.HoloRenderer;
import dev.alyti.fluxnautics.content.blocks.flux_tether.FluxTetherRenderer;
import dev.alyti.fluxnautics.content.blocks.inertia_dampener.InertiaDampenerRenderer;
import dev.alyti.fluxnautics.content.blocks.spatial_anchor.SpatialAnchorRenderer;
import dev.alyti.fluxnautics.content.blocks.stabilizer.StabilizerRenderer;
import dev.alyti.fluxnautics.content.blocks.thruster.IonThrusterBakedModel;
import dev.alyti.fluxnautics.content.blocks.thruster.IonThrusterRenderer;
import dev.alyti.fluxnautics.client.render.MekaRobitRenderer;
import dev.alyti.fluxnautics.content.items.drone.MekaRobitClientHandler;
import dev.alyti.fluxnautics.content.items.manipulator.MekaToolClientHandler;
import dev.alyti.fluxnautics.registry.*;
import foundry.veil.Veil;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.platform.VeilEventPlatform;
import mekanism.client.ClientRegistration;
import mekanism.client.ClientRegistrationUtil;
import mekanism.client.render.RenderPropertiesProvider;
import mekanism.common.registration.INamedEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Fluxnautics.MODID, value = Dist.CLIENT)
public class FluxnauticsClient {
    private static final Map<ResourceLocation, ClientRegistration.CustomModelRegistryObject> customModels = new ConcurrentHashMap<>();
    public static MekaRobitClientHandler MEKA_ROBIT_CLIENT_HANDLER = new MekaRobitClientHandler();
    public static MekaToolClientHandler MEKA_TOOL_CLIENT_HANDLER = new MekaToolClientHandler();

    public FluxnauticsClient(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        addCustomModel(FNBlocks.ION_THRUSTER, (orig, evt) -> new IonThrusterBakedModel(orig));

        // Field Drive effect
        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage(FieldShadowRenderer::renderShadowMap);

        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage(MekaRobitItemRenderer.MekaRobitHandRenderer::onRenderLevel);

        if (Veil.IMGUIMC)
            dev.alyti.fluxnautics.client.editor.VeilPlumeInspector.register();

        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage((stage, levelRenderer, bufferSource, matrixStack, frustumMatrix, projectionMatrix, renderTick, deltaTracker, camera, frustum) -> {
            if (stage == VeilRenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                HoloRenderer.renderLevel(Minecraft.getInstance().level, projectionMatrix, deltaTracker, VeilRenderBridge.create(frustum), camera);
            } else if (stage == VeilRenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
                ThrusterTrailManager.onWorldRender(bufferSource, camera, frustumMatrix, projectionMatrix);
            }
        });

        VeilEventPlatform.INSTANCE.onVeilAddShaderProcessors((provider, registry) -> {
            registry.addPreprocessor(new FieldFadeTransformer(), false);
            registry.addPreprocessor(new CloakFadePreProcessor(), false);
        });
        VeilEventPlatform.INSTANCE.onVeilRegisterFixedBuffers(registry -> {
            registry.registerFixedBuffer(VeilRenderLevelStageEvent.Stage.AFTER_PARTICLES, SFRenderTypes.classicLaser());
        });
    }

    @SubscribeEvent
    public static void registerContainers(RegisterMenuScreensEvent event) {
        ClientRegistrationUtil.registerScreen(event, FNContainerTypes.THRUSTER_ARRAY, GuiThrusterArrayScreen::new);
        ClientRegistrationUtil.registerScreen(event, FNContainerTypes.FIELD_DRIVE, GuiFieldDriveScreen::new);
        ClientRegistrationUtil.registerScreen(event, FNContainerTypes.SPATIAL_ANCHOR, GuiSpatialAnchorScreen::new);
        ClientRegistrationUtil.registerScreen(event, FNContainerTypes.ANTI_GRAV, GuiAntiGravScreen::new);
        ClientRegistrationUtil.registerScreen(event, FNContainerTypes.FORCEFIELD_HARMONIZER, GuiForcefieldHarmonizerScreen::new);
        ClientRegistrationUtil.registerScreen(event, FNContainerTypes.FORCEFIELD_EMITTER, GuiForcefieldEmitterScreen::new);
        ClientRegistrationUtil.registerScreen(event, FNContainerTypes.CLOAK_GENERATOR, GuiCloakGeneratorScreen::new);
        ClientRegistrationUtil.registerScreen(event, FNContainerTypes.ASSEMBLY_TERMINAL, GuiAssemblyTerminalScreen::new);
        ClientRegistrationUtil.registerScreen(event, FNContainerTypes.STABILIZER, GuiStabilizerScreen::new);
        ClientRegistrationUtil.registerScreen(event, FNContainerTypes.CONDUIT_CONNECTOR, GuiConduitConnectorScreen::new);
        ClientRegistrationUtil.registerScreen(event, FNContainerTypes.DAEMON_PROTOCOL, GuiAngelProtocolScreen::new);
        ClientRegistrationUtil.registerScreen(event, FNContainerTypes.FLUX_TETHER, GuiFluxTetherScreen::new);
        ClientRegistrationUtil.registerScreen(event, FNContainerTypes.INERTIA_DAMPENER, GuiInertiaDampenerScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ClientRegistrationUtil.bindTileEntityRenderer(event, IonThrusterRenderer::new, FNBlockEntityTypes.THRUSTER_ARRAY_CONTROLLER);
        ClientRegistrationUtil.bindTileEntityRenderer(event, HoloRenderer::new, FNBlockEntityTypes.HOLO);
        ClientRegistrationUtil.bindTileEntityRenderer(event, FieldDriveRenderer::new, FNBlockEntityTypes.FIELD_DRIVE);
        ClientRegistrationUtil.bindTileEntityRenderer(event, SpatialAnchorRenderer::new, FNBlockEntityTypes.SPATIAL_ANCHOR);
        ClientRegistrationUtil.bindTileEntityRenderer(event, AntiGravRenderer::new, FNBlockEntityTypes.ANTI_GRAV);
        ClientRegistrationUtil.bindTileEntityRenderer(event, InertiaDampenerRenderer::new, FNBlockEntityTypes.INERTIA_DAMPENER);
        ClientRegistrationUtil.bindTileEntityRenderer(event, ForcefieldEmitterRenderer::new, FNBlockEntityTypes.FORCEFIELD_EMITTER);
        ClientRegistrationUtil.bindTileEntityRenderer(event, StabilizerRenderer::new, FNBlockEntityTypes.STABILIZER);
        ClientRegistrationUtil.bindTileEntityRenderer(event, ConduitConnectorRenderer::new, FNBlockEntityTypes.CONDUIT_CONNECTOR);
        ClientRegistrationUtil.bindTileEntityRenderer(event, DaemonProtocolRenderer::new, FNBlockEntityTypes.DAEMON_PROTOCOL);
        ClientRegistrationUtil.bindTileEntityRenderer(event, FluxTetherRenderer::new, FNBlockEntityTypes.FLUX_TETHER);

        event.registerEntityRenderer(FNEntityTypes.MEKA_ROBIT.get(), MekaRobitRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new RenderPropertiesProvider.MekRenderProperties(MekaRobitItemRenderer.RENDERER), FNItems.MEKA_ROBIT);

        event.registerItem(new RenderPropertiesProvider.MekRenderProperties(new MachineISTER<>(StabilizerRenderer.FX) {}), FNBlocks.STABILIZER.getItemHolder());
        event.registerItem(new RenderPropertiesProvider.MekRenderProperties(new MachineISTER<>(AntiGravRenderer.FX) {}), FNBlocks.ANTI_GRAV.getItemHolder());
        event.registerItem(new RenderPropertiesProvider.MekRenderProperties(new MachineISTER<>(SpatialAnchorRenderer.FX) {}), FNBlocks.SPATIAL_ANCHOR.getItemHolder());
        event.registerItem(new RenderPropertiesProvider.MekRenderProperties(new MachineISTER<>(InertiaDampenerRenderer.FX) {}), FNBlocks.INERTIA_DAMPENER.getItemHolder());
    }

    @SubscribeEvent
    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        ClientRegistrationUtil.registerClientReloadListeners(event,
            //ISTERs
            MekaRobitItemRenderer.RENDERER
        );
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MekaRobit.LAYER_LOCATION, MekaRobit::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        FNModelCache.INSTANCE.setup(event);
    }

    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((rl, model) -> {
            ClientRegistration.CustomModelRegistryObject obj = customModels.get(rl.id());
            return obj == null ? model : obj.createModel(model, event);
        });
    }

    @SubscribeEvent
    public static void onModelBake(ModelEvent.BakingCompleted event) {
        FNModelCache.INSTANCE.onBake(event);
    }

    @SubscribeEvent
    public static void registerModelLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(Fluxnautics.id("conduit_ports"), ConduitPortModelLoader.INSTANCE);
    }

    public static void addCustomModel(INamedEntry provider, ClientRegistration.CustomModelRegistryObject object) {
        customModels.put(provider.getId(), object);
    }
}
