package dev.alyti.fluxnautics.client.gui.element;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.client.util.ForceClusterFinder;
import dev.alyti.fluxnautics.client.util.SimpleSubLevelGroupRenderer;
import dev.alyti.fluxnautics.client.util.UIRenderHelper;
import dev.alyti.fluxnautics.content.blocks.assembly_terminal.AssemblyTerminalBlockEntity;
import dev.alyti.fluxnautics.content.blocks.assembly_terminal.BlueprintConfig;
import dev.alyti.fluxnautics.lang.FNLang;
import dev.alyti.fluxnautics.network.packets.blueprint.BlueprintDataPacket;
import dev.alyti.fluxnautics.util.LangBuilder;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.common.lib.Color;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.*;

import java.lang.Math;
import java.util.*;
import java.util.function.Supplier;

public class GuiBlueprintWidget extends GuiInnerScreen {

    private static final double KEYBOARD_PAN_SPEED = 5;
    private static final double PAN_SENSITIVITY = 0.1;
    private static final float ZOOM_SENSITIVITY = 0.1f;
    private static final float MIN_ZOOM = 0.2f;
    private static final float MAX_ZOOM = 5.0f;
    private static final double ROTATION_SENSITIVITY = 0.5;

    private static final double VELOCITY_SCALE_FACTOR = 0.5;
    private static final double ANGULAR_VELOCITY_SCALE_FACTOR = 0.3;
    private static final double ANGULAR_ARC_RADIUS_FACTOR = 0.15;

    private static final int TOOLTIP_LABEL_COLOR = 0xffc2a3ad;
    private static final int BBOX_COLOR_X = 0xFFFF4444;
    private static final int BBOX_COLOR_Y = 0xFF44FF44;
    private static final int BBOX_COLOR_Z = 0xFF4444FF;
    private static final int BBOX_SHADOW_COLOR = 0xfff9f2de;
    private static final double BBOX_HOVER_DISTANCE = 8.0;
    private static final int LINEAR_VELOCITY_COLOR = 0xFFFFFF00; // Yellow
    private static final int ANGULAR_VELOCITY_COLOR = 0xFF00FFFF; // Cyan

    private static final int MIN_ARROW_SIZE_PX = 3;
    public static final float FPS = 60.0f;

    private final Vector3d localCameraPosition = new Vector3d();
    private final Vector3d cameraPosition = new Vector3d();
    private final Matrix4f projectionMat = new Matrix4f();
    private final Quaternionf localOrientation = new Quaternionf();

    private final ClientSubLevel subLevel;
    private BlueprintConfig config;
    private Supplier<BlueprintConfig>  configSupplier;
    private BlueprintDataPacket serverData;
    private Supplier<BlueprintDataPacket> serverDataSupplier;

    private AdvancedFbo fbo;
    private AdvancedFbo outlineFbo;
    private AdvancedFbo finalFbo;
    private boolean framebuffersInitialized = false;

    private float renderTime = FPS;
    private float viewportRadius;

    private boolean isShiftDown = false;
    private float yaw = -225;
    private float pitch = -45;
    private float zoom = 1.0f;
    private double panX = 0.0;
    private double panY = 0.0;


    public final List<Component> tooltipList = new ArrayList<>();

    public GuiBlueprintWidget(final IGuiWrapper gui, final int x, final int y,
                              final int width, final int height,
                              final ClientSubLevel subLevel,
                              Supplier<BlueprintDataPacket> dataSupplier, Supplier<BlueprintConfig> configSupplier) {
        super(gui, x, y, width, height);
        this.subLevel = subLevel;
        this.serverData = dataSupplier.get();
        this.serverDataSupplier = dataSupplier;
        this.config = configSupplier.get();
        this.configSupplier = configSupplier;
        this.padding(5).spacing(2).textScale(1.05F);

        updateViewportOrientation();
    }

    public GuiBlueprintWidget(final IGuiWrapper gui, final int x, final int y,
                              final int width, final int height,
                              final ClientSubLevel subLevel,
                              Supplier<BlueprintDataPacket> dataSupplier, TileEntityMekanism tile) {
        this(gui, x, y, width, height, subLevel, dataSupplier, () -> null);
        if (tile instanceof AssemblyTerminalBlockEntity atTile) {
            this.configSupplier = () -> atTile.blueprintConfig;
            this.config = atTile.blueprintConfig;
        }
    }


    public void updateViewportOrientation() {
        this.renderTime = Float.MAX_VALUE;
        this.localOrientation.identity()
                .rotateY((float) Math.toRadians(yaw))
                .rotateX((float) Math.toRadians(pitch));
    }

    private void initFramebuffers() {
        if (this.framebuffersInitialized) return;

        this.fbo = AdvancedFbo.withSize((this.width * 2), (this.height * 2))
                .addColorTextureBuffer()
                .setDepthTextureBuffer()
                .build(true);
        this.outlineFbo = AdvancedFbo.withSize((this.width * 2), (this.height * 2))
                .addColorTextureBuffer()
                .build(true);
        this.finalFbo = AdvancedFbo.withSize((this.width * 2), (this.height * 2))
                .addColorTextureBuffer()
                .build(true);

        this.framebuffersInitialized = true;
    }

    public void freeFramebuffers() {
        if (this.fbo != null) {
            this.fbo.free();
            this.fbo = null;

            this.outlineFbo.free();
            this.outlineFbo = null;

            this.finalFbo.free();
            this.finalFbo = null;
        }
        this.framebuffersInitialized = false;
    }

    @Override
    protected List<Component> getRenderStrings() {
        if (this.serverData == null)
            return Collections.emptyList();

        Component massComponent = FNLang.BLUEPRINT_INFO_MASS.translate(String.format("%,.2f", this.serverData.mass()));
        Component uuidComponent;
        if (this.subLevel.getName() != null && !this.subLevel.getName().isEmpty()) {
            uuidComponent = FNLang.BLUEPRINT_INFO_NAME_UUID.translate(this.subLevel.getName(), this.subLevel.getUniqueId());
        } else {
            uuidComponent = FNLang.BLUEPRINT_INFO_UUID.translate(this.subLevel.getUniqueId());
        }

        return List.of(uuidComponent, massComponent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            setDragging(true);
            setFocused(true);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        super.onRelease(mouseX, mouseY);
        setDragging(false);
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!contains(mouseX, mouseY)) return;

        if (isShiftDown) {
            this.panX -= deltaX * PAN_SENSITIVITY / this.zoom;
            this.panY += deltaY * PAN_SENSITIVITY / this.zoom;
            this.renderTime = Float.MAX_VALUE;
            return;
        }

        float newYaw = yaw + (float) (deltaX * ROTATION_SENSITIVITY);
        float newPitch = pitch + (float) (deltaY * ROTATION_SENSITIVITY);
        this.yaw = newYaw;
        this.pitch = Mth.clamp(newPitch, -180.0f, 180.0f);
        updateViewportOrientation();
        this.renderTime = Float.MAX_VALUE;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        final Minecraft mc = Minecraft.getInstance();
        final float dt = mc.getTimer().getRealtimeDeltaTicks() / 20.0f;
        if (keyCode == mc.options.keyUp.getKey().getValue())
            panDeltaY += KEYBOARD_PAN_SPEED * dt / this.zoom;
        if (keyCode == mc.options.keyDown.getKey().getValue())
            panDeltaY -= KEYBOARD_PAN_SPEED * dt / this.zoom;
        if (keyCode == mc.options.keyLeft.getKey().getValue())
            panDeltaX -= KEYBOARD_PAN_SPEED * dt / this.zoom;
        if (keyCode == mc.options.keyRight.getKey().getValue())
            panDeltaX += KEYBOARD_PAN_SPEED * dt / this.zoom;
        if (keyCode == mc.options.keyShift.getKey().getValue())
            isShiftDown = true;

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        final Minecraft mc = Minecraft.getInstance();

        if (keyCode == mc.options.keyShift.getKey().getValue()) {
            isShiftDown = false;
            return true;
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    double panDeltaX = 0.0;
    double panDeltaY = 0.0;

    private void handleKeyboardPanning() {
        if (panDeltaX != 0.0 || panDeltaY != 0.0) {
            this.panX += panDeltaX;
            panDeltaX = 0.0;
            this.panY += panDeltaY;
            panDeltaY = 0.0;
            this.renderTime = Float.MAX_VALUE; // Force immediate render
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xDelta, double yDelta)  {
        if (isMouseOver(mouseX, mouseY)) {
            this.zoom += (float) (yDelta * ZOOM_SENSITIVITY);
            this.zoom = Mth.clamp(this.zoom, MIN_ZOOM, MAX_ZOOM);
            this.renderTime = Float.MAX_VALUE; // Force immediate render
            return true; // Consume scroll event
        }
        return super.mouseScrolled(mouseX, mouseY, xDelta, yDelta);
    }

    private void renderContents(final float partialTicks) {
        if (VeilLevelPerspectiveRenderer.isRenderingPerspective()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();

        // Only render at framerate
        if (this.renderTime >= 20.0f / FPS) {
            this.renderTime = 0.0f;
        } else {
            this.renderTime += minecraft.getTimer().getRealtimeDeltaTicks();
            return;
        }

        if (!this.framebuffersInitialized) {
            initFramebuffers();
        }

        if (this.fbo == null) {
            return;
        }

        this.serverData = this.serverDataSupplier.get();

        final LevelPlot plot = this.subLevel.getPlot();

        final BoundingBox3ic plotBounds = plot.getBoundingBox();
        float radius = Math.max(
                Math.max(plotBounds.maxX() - plotBounds.minX(), plotBounds.maxY() - plotBounds.minY()),
                plotBounds.maxZ() - plotBounds.minZ()
        ) + 1;
        radius *= 0.55F;
        radius = Math.max(radius, 2.0f);

        float zoomedRadius = radius / this.zoom;
        this.viewportRadius = zoomedRadius;

        final Vector3d plotBoundsCenter = new Vector3d(
                (plotBounds.minX() + plotBounds.maxX() + 1) / 2.0,
                (plotBounds.minY() + plotBounds.maxY() + 1) / 2.0,
                (plotBounds.minZ() + plotBounds.maxZ() + 1) / 2.0
        );

        Vector3d right = this.localOrientation.transform(new Vector3d(1, 0, 0)).mul(this.panX);
        Vector3d up = this.localOrientation.transform(new Vector3d(0, 1, 0)).mul(this.panY);
        Vector3d adjustedCenter = plotBoundsCenter.add(right).add(up);

        final float aspect = (float) (this.width * 2) / (this.height * 2);
        final float zNear = -radius * 5.0f;
        final float zFar = radius * 10.0f;
        this.projectionMat.identity().ortho(-zoomedRadius * aspect, zoomedRadius * aspect, -zoomedRadius, zoomedRadius, zNear, zFar);

        this.localCameraPosition.set(adjustedCenter.add(
                this.localOrientation.transform(new Vector3d(0, 0, zoomedRadius))
        ));

        final Pose3dc renderPose = this.subLevel.renderPose(partialTicks);
        renderPose.transformPosition(this.cameraPosition.set(this.localCameraPosition));

        draw(this.subLevel, partialTicks, this.localOrientation, this.projectionMat,
                this.cameraPosition, (this.width * 2), (this.height * 2),
                this.fbo, this.outlineFbo, this.finalFbo,
                0.25f, 1.0f, 0x2E3032, 0x696965, renderPose);
    }

    public static void draw(final SubLevel subLevel, final float partialTicks,
                            final Quaternionf localOrientation, final Matrix4f projMatrix,
                            final Vector3d cameraPos, final float inWidth, final float inHeight,
                            final AdvancedFbo fbo, final AdvancedFbo outlineFbo,
                            final AdvancedFbo finalFbo, final float paletteOffset,
                            final float fadeScale, final int lineColor, final int lineShadowColor, final Pose3dc renderPose) {
        fbo.bind(true);
        fbo.clear();

        final Quaternionf orientation = new Quaternionf(renderPose.orientation()).conjugate();
        orientation.premul(localOrientation.conjugate(new Quaternionf()));

        SimpleSubLevelGroupRenderer.renderChain(subLevel, fbo, new Matrix4f(), projMatrix,
                cameraPos, orientation, partialTicks);

        final PostProcessingManager manager = VeilRenderSystem.renderer().getPostProcessingManager();
        final PostPipeline pipeline = manager.getPipeline(Fluxnautics.id("blueprint"));

        if (pipeline != null) {
            final Color LINE_SHADOW_COLOR = Color.rgb(lineShadowColor);
            final Color LINE_COLOR = Color.rgb(lineColor);


            pipeline.getUniformSafe("LineColor").setVector(LINE_COLOR.rf(), LINE_COLOR.gf(), LINE_COLOR.bf(), 1.0f);
            pipeline.getUniformSafe("LineShadowColor").setVector(LINE_SHADOW_COLOR.rf(), LINE_SHADOW_COLOR.gf(), LINE_SHADOW_COLOR.bf(), 1.0f);
            pipeline.getUniformSafe("InSize").setVector(inWidth, inHeight);
            pipeline.getUniformSafe("PaletteOffset").setFloat(paletteOffset);
            pipeline.getUniformSafe("FadeScale").setFloat(fadeScale);
        }

        final PostPipeline.Context context = manager.getPostPipelineContext();
        context.setFramebuffer(Fluxnautics.id("blueprint"), fbo);
        context.setFramebuffer(Fluxnautics.id("blueprint_outlined"), outlineFbo);
        context.setFramebuffer(Fluxnautics.id("blueprint_final"), finalFbo);

        manager.runPipeline(pipeline, false);
    }

    @Override
    public void drawBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (this.subLevel.isRemoved() || !this.visible) {
            return;
        }
        super.drawBackground(graphics, mouseX, mouseY, partialTicks);

        if (isFocused()) {
            this.handleKeyboardPanning();
        }

        this.renderContents(partialTicks);

        final PoseStack ps = graphics.pose();
        ps.pushPose();
        ps.translate(this.relativeX, this.relativeY, 0);
        ps.scale(0.5f, 0.5f, 1);

        renderFBO(graphics, this.finalFbo, (this.width * 2), (this.height * 2));

        this.renderBoundingBox(
                graphics,
                (mouseX - this.getX()) * 2,
                (mouseY - this.getY()) * 2,
                partialTicks
        );

        this.renderArrows(
                graphics,
                (mouseX - this.getX()) * 2,
                (mouseY - this.getY()) * 2,
                this.localOrientation,
                this.localCameraPosition,
                this.projectionMat
        );

        this.renderLinearVelocity(
                graphics,
                (mouseX - this.getX()) * 2,
                (mouseY - this.getY()) * 2,
                partialTicks
        );

        this.renderAngularVelocity(
                graphics,
                (mouseX - this.getX()) * 2,
                (mouseY - this.getY()) * 2,
                partialTicks
        );

        graphics.bufferSource().endBatch(RenderType.gui());

        if (this.config.displayCenterOfMass()) {
            this.renderCenterOfMass(graphics);
        }

        ps.popPose();
        if (!this.tooltipList.isEmpty()) {
            renderTooltip(graphics, mouseX - gui().getGuiLeft() + 12, mouseY - gui().getGuiTop() + 12, this.tooltipList);
        } else {
            this.clearTooltip();
        }
        this.tooltipList.clear();
    }


    public static void renderTooltip(final GuiGraphics guiGraphics, final int x, final int y, final List<Component> lines) {
        final Font font = Minecraft.getInstance().font;

        final Color colorBackground = Color.argb(0xaa000000);
        final Color colorBorderTop  = Color.argb(0xff514151);
        final Color colorBorderBot  = Color.argb(0xff664499);
        UIRenderHelper.drawHoveringText(guiGraphics, lines, x, y, guiGraphics.guiWidth(), guiGraphics.guiHeight(), -1, colorBackground.argb(), colorBorderTop.argb(), colorBorderBot.argb(), font);
    }

    private void renderBoundingBox(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        if (!this.config.displayBoundingBox())
            return;

        final LevelPlot plot = this.subLevel.getPlot();
        final BoundingBox3ic localBounds = plot.getBoundingBox();

        final Vector2d[] localScreenCorners = getBBoxScreenCorners(localBounds);

        graphics.enableScissor(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1);
        drawBBoxEdges(graphics, localScreenCorners);
        graphics.bufferSource().endBatch(RenderType.gui());
        graphics.disableScissor();

        boolean localHovered = isNearBBoxEdges(localScreenCorners, mouseX, mouseY);

        if (localHovered) {
            final TextColor labelColor = TextColor.fromRgb(TOOLTIP_LABEL_COLOR);
            final TextColor valueColor = TextColor.fromRgb(0xffffffff);

            final int sizeX = localBounds.maxX() - localBounds.minX() + 1;
            final int sizeY = localBounds.maxY() - localBounds.minY() + 1;
            final int sizeZ = localBounds.maxZ() - localBounds.minZ() + 1;


            tooltipList.add(Component.literal("Local Bounding Box (XYZ)").withStyle(style -> style.withColor(labelColor)));

            MutableComponent m = MutableComponent.create(Component.literal("  ").getContents());
            m.append(Component.literal(String.valueOf(sizeX)).withColor(BBOX_COLOR_X));
            m.append(Component.literal(" x "));
            m.append(Component.literal(String.valueOf(sizeY)).withColor(BBOX_COLOR_Y));
            m.append(Component.literal(" x "));
            m.append(Component.literal(String.valueOf(sizeZ)).withColor(BBOX_COLOR_Z));
            tooltipList.add(m.withStyle(style -> style.withColor(valueColor)));
        }
    }

    private Vector2d[] getBBoxScreenCorners(final BoundingBox3ic bounds) {
        final Vector2d[] corners = new Vector2d[8];
        for (int i = 0; i < 8; i++) {
            final Vector3d point = new Vector3d(
                    (i & 1) == 0 ? bounds.minX() : bounds.maxX() + 1,
                    (i & 2) == 0 ? bounds.minY() : bounds.maxY() + 1,
                    (i & 4) == 0 ? bounds.minZ() : bounds.maxZ() + 1
            );
            corners[i] = getScreenCoords(
                    point,
                    this.localOrientation,
                    this.localCameraPosition,
                    this.projectionMat,
                    (this.width * 2),
                    (this.height * 2)
            );
        }
        return corners;
    }

    private void drawBBoxEdges(final GuiGraphics graphics, final Vector2d[] corners) {
        final MultiBufferSource.BufferSource bufferSource = graphics.bufferSource();
        final VertexConsumer builder = bufferSource.getBuffer(RenderType.gui());
        final Matrix4f pose = graphics.pose().last().pose();

        drawBBoxLine(builder, pose, corners[0], corners[1], BBOX_COLOR_X);
        drawBBoxLine(builder, pose, corners[2], corners[3], BBOX_COLOR_X);
        drawBBoxLine(builder, pose, corners[4], corners[5], BBOX_COLOR_X);
        drawBBoxLine(builder, pose, corners[6], corners[7], BBOX_COLOR_X);

        drawBBoxLine(builder, pose, corners[0], corners[2], BBOX_COLOR_Y);
        drawBBoxLine(builder, pose, corners[1], corners[3], BBOX_COLOR_Y);
        drawBBoxLine(builder, pose, corners[4], corners[6], BBOX_COLOR_Y);
        drawBBoxLine(builder, pose, corners[5], corners[7], BBOX_COLOR_Y);

        drawBBoxLine(builder, pose, corners[0], corners[4], BBOX_COLOR_Z);
        drawBBoxLine(builder, pose, corners[1], corners[5], BBOX_COLOR_Z);
        drawBBoxLine(builder, pose, corners[2], corners[6], BBOX_COLOR_Z);
        drawBBoxLine(builder, pose, corners[3], corners[7], BBOX_COLOR_Z);
    }

    private void drawBBoxLine(final VertexConsumer builder, final Matrix4f pose,
                              final Vector2d start, final Vector2d end, final int color) {
        drawLine(builder, pose, (int) start.x, (int) start.y, (int) end.x, (int) end.y, BBOX_SHADOW_COLOR, 1);
        drawLine(builder, pose, (int) start.x, (int) start.y, (int) end.x, (int) end.y, color, 0);
    }

    private boolean isNearBBoxEdges(final Vector2d[] corners, final int mouseX, final int mouseY) {
        final Vector2d mousePos = new Vector2d(mouseX, mouseY);
        final double threshold = BBOX_HOVER_DISTANCE * BBOX_HOVER_DISTANCE;

        for (int y : new int[]{0, 1}) {
            for (int z : new int[]{0, 1}) {
                final int i0 = (y << 1) | (z << 2);
                final int i1 = i0 | 1;
                if (isNearLine(mousePos, corners[i0], corners[i1], threshold)) {
                    return true;
                }
            }
        }

        for (int x : new int[]{0, 1}) {
            for (int z : new int[]{0, 1}) {
                final int i0 = x | (z << 2);
                final int i1 = i0 | 2;
                if (isNearLine(mousePos, corners[i0], corners[i1], threshold)) {
                    return true;
                }
            }
        }

        for (int x : new int[]{0, 1}) {
            for (int y : new int[]{0, 1}) {
                final int i0 = x | (y << 1);
                final int i1 = i0 | 4;
                if (isNearLine(mousePos, corners[i0], corners[i1], threshold)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isNearLine(final Vector2d point, final Vector2d lineStart, final Vector2d lineEnd, final double thresholdSquared) {
        final Vector2d lineDir = new Vector2d(lineEnd).sub(lineStart);
        final double lineLengthSq = lineDir.lengthSquared();

        if (lineLengthSq < 0.001) {
            return point.distanceSquared(lineStart) < thresholdSquared;
        }

        double t = new Vector2d(point).sub(lineStart).dot(lineDir) / lineLengthSq;
        t = Mth.clamp(t, 0.0, 1.0);

        final Vector2d closestPoint = new Vector2d(lineStart).fma(t, lineDir);
        return point.distanceSquared(closestPoint) < thresholdSquared;
    }

    private void renderArrows(final GuiGraphics graphics, final int mouseX, final int mouseY,
                              final Quaternionfc orientation, final Vector3dc cameraPos,
                              final Matrix4fc projMatrix) {
        if (this.serverData != null) {
            double maxArrowLengthSquared = 0.0;

            final Map<ForceGroup, List<ForceClusterFinder.Cluster>> clusters = new HashMap<>();

            for (final ResourceLocation groupId : this.config.enabledForceGroups()) {
                final ForceGroup group = ForceGroups.REGISTRY.get(groupId);
                if (group == null) continue;

                final List<QueuedForceGroup.PointForce> forces = this.serverData.forces().get(group);
                if (forces == null) continue;

                final List<ForceClusterFinder.Cluster> cluster = this.config.mergeForces()
                        ? ForceClusterFinder.getMergedClusters(forces)
                        : ForceClusterFinder.passThrough(forces);

                clusters.put(group, cluster);

                for (final ForceClusterFinder.Cluster force : cluster) {
                    maxArrowLengthSquared = Math.max(maxArrowLengthSquared, force.force().lengthSquared());
                }
            }

            for (final ResourceLocation groupId : this.config.enabledForceGroups()) {
                final ForceGroup group = ForceGroups.REGISTRY.get(groupId);
                if (group == null) continue;

                final List<ForceClusterFinder.Cluster> cluster = clusters.get(group);
                if (cluster == null) continue;

                for (final ForceClusterFinder.Cluster force : cluster) {
                    this.renderForceArrow(
                            graphics,
                            group,
                            force,
                            Math.sqrt(maxArrowLengthSquared),
                            mouseX,
                            mouseY,
                            this.tooltipList,
                            orientation,
                            cameraPos,
                            projMatrix
                    );
                }
            }
        }
    }

    private void renderForceArrow(final GuiGraphics graphics,
                                  final ForceGroup forceGroup,
                                  final ForceClusterFinder.Cluster pointForce,
                                  final double maxArrowLength,
                                  final int mouseX,
                                  final int mouseY,
                                  final List<Component> tooltipLines,
                                  final Quaternionfc orientation,
                                  final Vector3dc cameraPos,
                                  final Matrix4fc projMatrix) {
        final double forceMagnitude = pointForce.force().length();

        if (forceMagnitude <= 0.01 || this.viewportRadius == 0.0) {
            return;
        }

        final Vector3d globalFirstDir = pointForce.force().normalize(new Vector3d());
        final Vector3d forceOffset = globalFirstDir.mul(
                Math.max(0.25, forceMagnitude / maxArrowLength) * this.viewportRadius * 0.5,
                new Vector3d()
        );

        final Vector2d originCoords = getScreenCoords(
                new Vector3d(pointForce.pos()),
                orientation, cameraPos, projMatrix, (this.width * 2), (this.height * 2)
        );

        if (!this.canDrawArrowAt((int) originCoords.x, (int) originCoords.y)) {
            return;
        }

        final Vector2d mousePos = new Vector2d(mouseX, mouseY);
        final int color = (255 << 24) | forceGroup.color();
        final int shadowColor = 0xfff9f2de;

        final double facingDot = orientation.transformInverse(globalFirstDir, new Vector3d())
                .dot(OrientedBoundingBox3d.FORWARD);

        if (Math.abs(facingDot) > 0.95) {
            final PoseStack ps = graphics.pose();
            ps.pushPose();
            ps.translate(0.0, 0.0, 1.0f);

            if (mousePos.sub(originCoords, new Vector2d()).lengthSquared() < 16.0 * 16.0) {
                addForceArrowTooltip(forceGroup, pointForce.groupSize().getValue(), forceMagnitude, color, tooltipLines);
            }

            if (facingDot < 0.0) {
                GuiTextureElement.BLUEPRINT_ICON_ARROW_IN_PAGE_SHADOW.render(
                        graphics, (int) originCoords.x - 8, (int) originCoords.y - 8, Color.rgb(shadowColor));
                GuiTextureElement.BLUEPRINT_ICON_ARROW_IN_PAGE.render(
                        graphics, (int) originCoords.x - 8, (int) originCoords.y - 8, Color.rgb(color));
            } else {
                GuiTextureElement.BLUEPRINT_ICON_ARROW_OUT_PAGE_SHADOW.render(
                        graphics, (int) originCoords.x - 8, (int) originCoords.y - 8, Color.rgb(shadowColor));
                GuiTextureElement.BLUEPRINT_ICON_ARROW_OUT_PAGE.render(
                        graphics, (int) originCoords.x - 8, (int) originCoords.y - 8, Color.rgb(color));
            }

            ps.popPose();
            return;
        }

        final Vector2d resultCoords = getScreenCoords(
                pointForce.pos().add(forceOffset, new Vector3d()),
                orientation, cameraPos, projMatrix, (this.width * 2), (this.height * 2)
        );

        final Vector2d arrowDir = resultCoords.sub(originCoords, new Vector2d());
        float arrowLength = (float) arrowDir.length();
        arrowDir.div(arrowLength);

        while (arrowLength > 0 && !this.canDrawArrowAt((int) resultCoords.x, (int) resultCoords.y)) {
            resultCoords.fma(-3.0, arrowDir);
            arrowLength -= 3.0f;
        }

        final int x1 = (int) originCoords.x();
        final int y1 = (int) originCoords.y();
        final int x2 = (int) resultCoords.x();
        final int y2 = (int) resultCoords.y();

        final MultiBufferSource.BufferSource bufferSource = graphics.bufferSource();
        final VertexConsumer builder = bufferSource.getBuffer(RenderType.gui());
        final Matrix4f pose = graphics.pose().last().pose();

        final Vector2d arrowLeft = new Vector2d(-arrowDir.y(), arrowDir.x()).mul(4.0);
        final Vector2d arrowRight = new Vector2d(arrowDir.y(), -arrowDir.x()).mul(4.0);
        final float headLen = 3.0f;
        final boolean drawArrow = originCoords.distanceSquared(resultCoords) > MIN_ARROW_SIZE_PX * MIN_ARROW_SIZE_PX;

        double distanceAlongLine = mousePos.sub(originCoords, new Vector2d()).dot(arrowDir);
        distanceAlongLine = Mth.clamp(distanceAlongLine, 0.0, arrowLength);

        final boolean displayTooltip = new Vector2d(originCoords)
                .fma(distanceAlongLine, arrowDir).distance(mousePos) < 10.0;
        if (displayTooltip) {
            addForceArrowTooltip(forceGroup, pointForce.groupSize().getValue(), forceMagnitude, color, tooltipLines);
        }

        int inflation = 3;
        final int z = 1;
        builder.addVertex(pose, (float) x1 - inflation, (float) y1 - inflation, (float) z).setColor(shadowColor);
        builder.addVertex(pose, (float) x1 - inflation, (float) y1 + 1 + inflation, (float) z).setColor(shadowColor);
        builder.addVertex(pose, (float) x1 + 1 + inflation, (float) y1 + 1 + inflation, (float) z).setColor(shadowColor);
        builder.addVertex(pose, (float) x1 + 1 + inflation, (float) y1 - inflation, (float) z).setColor(shadowColor);

        if (drawArrow) {
            // Arrow shadow
            drawLine(builder, pose, x2, y2, (int) (x2 - arrowDir.x * headLen + arrowLeft.x),
                    (int) (y2 - arrowDir.y * headLen + arrowLeft.y), shadowColor, 1);
            drawLine(builder, pose, x2, y2, (int) (x2 - arrowDir.x * headLen + arrowRight.x),
                    (int) (y2 - arrowDir.y * headLen + arrowRight.y), shadowColor, 1);
            drawLine(builder, pose, x1, y1, x2, y2, shadowColor, 1);

            // Actual arrow
            drawLine(builder, pose, x2, y2, (int) (x2 - arrowDir.x * headLen + arrowLeft.x),
                    (int) (y2 - arrowDir.y * headLen + arrowLeft.y), color, 0);
            drawLine(builder, pose, x2, y2, (int) (x2 - arrowDir.x * headLen + arrowRight.x),
                    (int) (y2 - arrowDir.y * headLen + arrowRight.y), color, 0);
            drawLine(builder, pose, x1, y1, x2, y2, color, 0);
        }

        inflation = 2;
        builder.addVertex(pose, (float) x1 - inflation, (float) y1 - inflation, (float) z).setColor(color);
        builder.addVertex(pose, (float) x1 - inflation, (float) y1 + 1 + inflation, (float) z).setColor(color);
        builder.addVertex(pose, (float) x1 + 1 + inflation, (float) y1 + 1 + inflation, (float) z).setColor(color);
        builder.addVertex(pose, (float) x1 + 1 + inflation, (float) y1 - inflation, (float) z).setColor(color);
    }

    private static void addForceArrowTooltip(final ForceGroup forceGroup, final int forceCount,
                                             final double forceMagnitude, final int color,
                                             final List<Component> tooltipLines) {
        final LangBuilder forceNameText = FNLang.builder().add(forceGroup.name()).color(color);
        final MutableComponent forceMagnitudeText = FNLang.BLUEPRINT_FORCE_ARROW_MAGNITUDE.translateColored(
                TextColor.fromRgb(0xffffffff),
                String.format("%,.2f", forceMagnitude)
        );

        if (forceCount > 1) {
            tooltipLines.add(FNLang.BLUEPRINT_MERGED_FORCE_ARROW.translateColored(
                    TextColor.fromRgb(TOOLTIP_LABEL_COLOR),
                    FNLang.BLUEPRINT_MERGING_NUMERAL.with(Integer.toString(forceCount)).color(0xffffffff).component(),
                    forceNameText.component(),
                    forceMagnitudeText
            ));
        } else {
            tooltipLines.add(FNLang.BLUEPRINT_FORCE_ARROW.translateColored(
                    TextColor.fromRgb(TOOLTIP_LABEL_COLOR),
                    forceNameText.component(),
                    forceMagnitudeText
            ));
        }
    }

    private boolean canDrawArrowAt(final int x, final int y) {
        final int padding = 8;
        return x >= padding && x < (this.width * 2) - padding && y >= padding && y < (this.height * 2) - padding;
    }

    private static void drawLine(final VertexConsumer builder, final Matrix4f pose,
                                 int x1, int y1, final int x2, final int y2,
                                 final int color, final int inflation) {
        final int z = 1;
        final int dx = Math.abs(x2 - x1);
        final int dy = Math.abs(y2 - y1);
        final int sx = x1 < x2 ? 1 : -1;
        final int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            builder.addVertex(pose, (float) x1 - inflation, (float) y1 - inflation, (float) z).setColor(color);
            builder.addVertex(pose, (float) x1 - inflation, (float) y1 + 1 + inflation, (float) z).setColor(color);
            builder.addVertex(pose, (float) x1 + 1 + inflation, (float) y1 + 1 + inflation, (float) z).setColor(color);
            builder.addVertex(pose, (float) x1 + 1 + inflation, (float) y1 - inflation, (float) z).setColor(color);

            if (x1 == x2 && y1 == y2) break;

            final int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    private void renderCenterOfMass(final GuiGraphics graphics) {
        final Vector3d centerOfMass = new Vector3d(this.subLevel.logicalPose().rotationPoint());
        final Vector2d screenCoords = getScreenCoords(
                centerOfMass,
                this.localOrientation,
                this.localCameraPosition,
                this.projectionMat,
                (this.width * 2),
                (this.height * 2)
        );

        final GuiTextureElement tex = GuiTextureElement.BLUEPRINT_ICON_COM;

        final PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(screenCoords.x - 8, screenCoords.y - 8, 0);
        graphics.blit(
                tex.location, 0, 0, 5,
                tex.startX, tex.startY,
                tex.width, tex.height,
                tex.texWidth, tex.texHeight
        );
        pose.popPose();
    }

    public static void renderFBO(final GuiGraphics graphics, final AdvancedFbo fbo,
                                 final int width, final int height) {
        final int id = fbo.getColorTextureAttachment(0).getId();

        RenderSystem.setShaderTexture(0, id);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        final Matrix4f matrix4f = graphics.pose().last().pose();
        final BufferBuilder bufferbuilder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR
        );

        final float x1 = 0.0f;
        final float y1 = 0.0f;
        bufferbuilder.addVertex(matrix4f, x1, y1, 0.0f).setUv(0.0f, 1.0f).setColor(0xFFFFFFFF);
        bufferbuilder.addVertex(matrix4f, x1, height, 0.0f).setUv(0.0f, 0.0f).setColor(0xFFFFFFFF);
        bufferbuilder.addVertex(matrix4f, width, height, 0.0f).setUv(1.0f, 0.0f).setColor(0xFFFFFFFF);
        bufferbuilder.addVertex(matrix4f, width, y1, 0.0f).setUv(1.0f, 1.0f).setColor(0xFFFFFFFF);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private void renderLinearVelocity(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {

        if (this.serverData == null) return;

        final Vector3dc globalLinearVelocity = this.serverData.linearVelocity();
        final double velocityMagSq = globalLinearVelocity.lengthSquared();
        if (velocityMagSq < 0.0001) return;

        final Vector3d centerOfMass = new Vector3d(this.subLevel.logicalPose().rotationPoint());

        // Transform global velocity to local/plot space using the inverse of sublevel's orientation
        final Pose3dc renderPose = this.subLevel.renderPose(partialTicks);
        final Quaternionf inverseOrientation = new Quaternionf(renderPose.orientation()).conjugate();
        final Vector3d localVelocity = inverseOrientation.transform(globalLinearVelocity, new Vector3d());

        final double velocityMag = Math.sqrt(velocityMagSq);

        // Scale arrow length based on velocity magnitude, capped at viewport radius
        final double scaledLength = Math.min(velocityMag * VELOCITY_SCALE_FACTOR, this.viewportRadius * 0.5);

        final Vector3d direction = localVelocity.normalize(new Vector3d());
        final Vector3d endPoint = centerOfMass.add(direction.mul(scaledLength, new Vector3d()), new Vector3d());

        final Vector2d startScreen = getScreenCoords(
                centerOfMass, this.localOrientation, this.localCameraPosition,
                this.projectionMat, (this.width * 2), (this.height * 2)
        );

        final Vector2d endScreen = getScreenCoords(
                endPoint, this.localOrientation, this.localCameraPosition,
                this.projectionMat, (this.width * 2), (this.height * 2)
        );

        if (!canDrawArrowAt((int) startScreen.x, (int) startScreen.y)) return;

        final int color = LINEAR_VELOCITY_COLOR;
        final int shadowColor = 0xfff9f2de;

        drawVelocityArrow(graphics, startScreen, endScreen, color, shadowColor, 6.0f);

        checkVelocityTooltip(startScreen, endScreen, mouseX, mouseY, "Linear Velocity",
                velocityMag, globalLinearVelocity, color);
    }

    private void renderAngularVelocity(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        if (this.serverData == null) return;

        final Vector3dc globalAngularVelocity = this.serverData.angularVelocity();
        final double angularVelMagSq = globalAngularVelocity.lengthSquared();
        if (angularVelMagSq < 0.0001) return;

        final Vector3d centerOfMass = new Vector3d(this.subLevel.logicalPose().rotationPoint());

        final Pose3dc renderPose = this.subLevel.renderPose(partialTicks);
        final Quaternionf inverseOrientation = new Quaternionf(renderPose.orientation()).conjugate();
        final Vector3d localAngularVelocity = inverseOrientation.transform(globalAngularVelocity, new Vector3d());

        final double angularVelMag = Math.sqrt(angularVelMagSq);

        final double scaledLength = Math.min(angularVelMag * ANGULAR_VELOCITY_SCALE_FACTOR, this.viewportRadius * 0.4);

        final Vector3d axisDirection = localAngularVelocity.normalize(new Vector3d());
        final Vector3d endPoint = centerOfMass.add(axisDirection.mul(scaledLength, new Vector3d()), new Vector3d());

        final Vector2d startScreen = getScreenCoords(
                centerOfMass, this.localOrientation, this.localCameraPosition,
                this.projectionMat, (this.width * 2), (this.height * 2)
        );

        final Vector2d endScreen = getScreenCoords(
                endPoint, this.localOrientation, this.localCameraPosition,
                this.projectionMat, (this.width * 2), (this.height * 2)
        );

        if (!canDrawArrowAt((int) startScreen.x, (int) startScreen.y)) return;

        final int color = ANGULAR_VELOCITY_COLOR;
        final int shadowColor = 0xfff9f2de;

        drawVelocityArrow(graphics, startScreen, endScreen, color, shadowColor, 6.0f);

        renderRotationArc(graphics, centerOfMass, axisDirection, angularVelMag, color, shadowColor);

        checkVelocityTooltip(startScreen, endScreen, mouseX, mouseY, "Angular Velocity",
                angularVelMag, globalAngularVelocity, color);
    }

    private void renderRotationArc(final GuiGraphics graphics, final Vector3d center,
                                   final Vector3d axis, final double magnitude,
                                   final int color, final int shadowColor) {
        // Find two perpendicular vectors to the axis
        final Vector3d perp1 = new Vector3d();
        if (Math.abs(axis.y()) < 0.9) {
            perp1.set(axis).cross(new Vector3d(0, 1, 0)).normalize();
        } else {
            perp1.set(axis).cross(new Vector3d(1, 0, 0)).normalize();
        }
        final Vector3d perp2 = new Vector3d(axis).cross(perp1).normalize();

        final double arcRadius = this.viewportRadius * ANGULAR_ARC_RADIUS_FACTOR;
        final int segments = 16;
        final double arcSpan = Math.PI * 1.5; // 270 degrees

        final MultiBufferSource.BufferSource bufferSource = graphics.bufferSource();
        final VertexConsumer builder = bufferSource.getBuffer(RenderType.gui());
        final Matrix4f pose = graphics.pose().last().pose();

        Vector2d prevScreen = null;

        for (int i = 0; i <= segments; i++) {
            final double t = (double) i / segments;
            final double angle = t * arcSpan;

            final Vector3d point = new Vector3d(center)
                    .add(perp1.mul(Math.cos(angle) * arcRadius, new Vector3d()))
                    .add(perp2.mul(Math.sin(angle) * arcRadius, new Vector3d()));

            final Vector2d screenPoint = getScreenCoords(
                    point, this.localOrientation, this.localCameraPosition,
                    this.projectionMat, (this.width * 2), (this.height * 2)
            );

            if (prevScreen != null) {
                drawLine(builder, pose, (int) prevScreen.x, (int) prevScreen.y,
                        (int) screenPoint.x, (int) screenPoint.y, shadowColor, 1);
                drawLine(builder, pose, (int) prevScreen.x, (int) prevScreen.y,
                        (int) screenPoint.x, (int) screenPoint.y, color, 0);
            }
            prevScreen = screenPoint;
        }

        // Draw arrowhead at the end of the arc
        if (prevScreen != null) {
            final double endAngle = arcSpan;
            final Vector3d tangentDir = new Vector3d(perp1)
                    .mul(-Math.sin(endAngle))
                    .add(perp2.mul(Math.cos(endAngle)));

            final Vector3d arrowTip = new Vector3d(center)
                    .add(perp1.mul(Math.cos(endAngle) * arcRadius, new Vector3d()))
                    .add(perp2.mul(Math.sin(endAngle) * arcRadius, new Vector3d()))
                    .add(tangentDir.mul(arcRadius * 0.15));

            final Vector2d tipScreen = getScreenCoords(
                    arrowTip, this.localOrientation, this.localCameraPosition,
                    this.projectionMat, (this.width * 2), (this.height * 2)
            );

            drawLine(builder, pose, (int) prevScreen.x, (int) prevScreen.y,
                    (int) tipScreen.x, (int) tipScreen.y, shadowColor, 1);
            drawLine(builder, pose, (int) prevScreen.x, (int) prevScreen.y,
                    (int) tipScreen.x, (int) tipScreen.y, color, 0);
        }
    }

    /**
     * Draws a velocity-style arrow with arrowhead.
     */
    private void drawVelocityArrow(final GuiGraphics graphics, final Vector2d start,
                                   final Vector2d end, final int color, final int shadowColor,
                                   final float headLength) {
        final MultiBufferSource.BufferSource bufferSource = graphics.bufferSource();
        final VertexConsumer builder = bufferSource.getBuffer(RenderType.gui());
        final Matrix4f pose = graphics.pose().last().pose();

        final double distSq = start.distanceSquared(end);
        if (distSq < MIN_ARROW_SIZE_PX * MIN_ARROW_SIZE_PX) return;

        final Vector2d arrowDir = end.sub(start, new Vector2d()).normalize(new Vector2d());
        final Vector2d arrowLeft = new Vector2d(-arrowDir.y(), arrowDir.x()).mul(4.0);
        final Vector2d arrowRight = new Vector2d(arrowDir.y(), -arrowDir.x()).mul(4.0);

        final int x1 = (int) start.x();
        final int y1 = (int) start.y();
        final int x2 = (int) end.x();
        final int y2 = (int) end.y();

        // Shadow
        drawLine(builder, pose, x1, y1, x2, y2, shadowColor, 1);
        drawLine(builder, pose, x2, y2,
                (int) (x2 - arrowDir.x * headLength + arrowLeft.x),
                (int) (y2 - arrowDir.y * headLength + arrowLeft.y), shadowColor, 1);
        drawLine(builder, pose, x2, y2,
                (int) (x2 - arrowDir.x * headLength + arrowRight.x),
                (int) (y2 - arrowDir.y * headLength + arrowRight.y), shadowColor, 1);

        // Arrow
        drawLine(builder, pose, x1, y1, x2, y2, color, 0);
        drawLine(builder, pose, x2, y2,
                (int) (x2 - arrowDir.x * headLength + arrowLeft.x),
                (int) (y2 - arrowDir.y * headLength + arrowLeft.y), color, 0);
        drawLine(builder, pose, x2, y2,
                (int) (x2 - arrowDir.x * headLength + arrowRight.x),
                (int) (y2 - arrowDir.y * headLength + arrowRight.y), color, 0);
    }

    private void checkVelocityTooltip(final Vector2d start, final Vector2d end,
                                      final int mouseX, final int mouseY,
                                      final String label, final double magnitude,
                                      final Vector3dc vector, final int color) {
        final Vector2d mousePos = new Vector2d(mouseX, mouseY);
        final Vector2d arrowDir = end.sub(start, new Vector2d()).normalize(new Vector2d());
        final double arrowLength = start.distance(end);

        double distanceAlongLine = mousePos.sub(start, new Vector2d()).dot(arrowDir);
        distanceAlongLine = Mth.clamp(distanceAlongLine, 0.0, arrowLength);

        if (new Vector2d(start).fma(distanceAlongLine, arrowDir).distance(mousePos) < 12.0) {
            final TextColor labelColor = TextColor.fromRgb(TOOLTIP_LABEL_COLOR);
            final TextColor valueColor = TextColor.fromRgb(0xffffffff);

            tooltipList.add(Component.literal(label).withStyle(s -> s.withColor(labelColor)));
            tooltipList.add(Component.literal(String.format("  |v| = %.4f", magnitude))
                    .withStyle(s -> s.withColor(TextColor.fromRgb(color))));
            tooltipList.add(Component.literal(String.format("  (%.3f, %.3f, %.3f)",
                            vector.x(), vector.y(), vector.z()))
                    .withStyle(s -> s.withColor(valueColor)));
        }
    }

    /**
     * Projects a 3D point into pixel coordinates
     */
    public static Vector2d getScreenCoords(final Vector3d plotSpacePoint, final Quaternionfc orientation,
                                           final Vector3dc localPosition, final Matrix4fc projMatrix,
                                           final int width, final int height) {
        plotSpacePoint.sub(localPosition);
        orientation.transformInverse(plotSpacePoint);

        final Vector4f clipSpace = new Vector4f(
                (float) plotSpacePoint.x,
                (float) plotSpacePoint.y,
                (float) plotSpacePoint.z,
                1.0f
        );
        clipSpace.mul(projMatrix);
        clipSpace.div(clipSpace.w);

        final double projectedX = ((clipSpace.x() * 0.5f + 0.5f) * width);
        final double projectedY = ((-clipSpace.y() * 0.5f + 0.5f) * height);
        return new Vector2d(projectedX, projectedY);
    }

    /**
     * Projects a blueprint-space coordinate into plot-space
     */
    public static Vector3d getPlotCoords(final Vector2dc blueprintSpacePoint, final Quaternionfc orientation,
                                         final Vector3dc localPosition, final Matrix4fc projMatrix,
                                         final int width, final int height) {
        final Vector3d clipSpace = new Vector3d(
                2 * blueprintSpacePoint.x() / width - 1,
                1 - 2 * blueprintSpacePoint.y() / height,
                0
        );
        final Vector3d point = clipSpace.sub(projMatrix.getTranslation(new Vector3f()))
                .div(projMatrix.m00(), projMatrix.m11(), projMatrix.m22());

        orientation.transform(point);
        point.add(localPosition);
        return point;
    }

    public boolean contains(double x, double y) {
        x -= this.getX();
        y -= this.getY();
        return x > 0 && x < this.width && y > 0 && y < this.height;
    }

    public Vector2d clamp(final Vector2d dest) {
        final float minX = this.relativeX;
        final float minY = this.relativeY;
        dest.max(new Vector2d(minX, minY));
        dest.min(new Vector2d(minX + (this.width * 2) - 1, minY + (this.height * 2) - 1));
        return dest;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!visible) {
            return false;
        }
        return this.contains(mouseX, mouseY);
    }
}