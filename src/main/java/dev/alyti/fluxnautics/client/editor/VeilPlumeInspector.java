package dev.alyti.fluxnautics.client.editor;

import dev.alyti.fluxnautics.client.render.ThrusterTrailManager;
import foundry.veil.api.client.editor.SingleWindowInspector;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.platform.VeilEventPlatform;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class VeilPlumeInspector extends SingleWindowInspector {

    private static final Component TITLE = Component.literal("Fluxnautics Veil Plume Inspector");
    private static final String DEFAULT_PROGRAM = "fluxnautics:laser/laser_classic";
    private static final String FBM_PROGRAM = "fluxnautics:laser/laser_fbm";
    private static final String TRAIL_PROGRAM = "fluxnautics:trail_fbm";

    private record Knob(String uniform, String label, float min, float max, float def) {}

    private static final Knob[] COMMON = {
        new Knob("uDisplace", "Master displacement", 0.0f, 0.3f, 0.0f),
        new Knob("uTessBulge", "Throat bulge (tess, ta-27)", 0.0f, 0.4f, 0.0f),
        new Knob("uTessFit", "Billow size fit (full at radius)", 0.0f, 3.0f, 0.0f),
        new Knob("uTessDensity", "Tess density (motes need this up)", 0.0f, 48.0f, 0.0f),
    };


    private static final Knob[] RAINBOW = {
        new Knob("uHueSpan", "Hue sweep nozzle->tip", 0.0f, 1.5f, 1.0f),
        new Knob("uHueWave", "Wave amplitude (hue)", 0.0f, 0.6f, 0.3f),
        new Knob("uHueWaveLen", "Waves along plume", 0.25f, 6.0f, 1.0f),
        new Knob("uHueWaveSpeed", "Wave travel (lengthPos/s)", 0.0f, 2.5f, 0.4f),
    };

    private static final Knob[] TRAIL = {
        new Knob("uTrailSpan", "Hue sweep nozzle->tail", 0.0f, 1.0f, 0.0f),
        new Knob("uTrailGamma", "Edge softness", 0.5f, 4.0f, 2.2f),
        new Knob("uTrailFade", "Tail fade power", 0.25f, 4.0f, 1.4f),
        new Knob("uTrailNoise", "Wisp amount", 0.0f, 1.0f, 0.45f),
        new Knob("uTrailScale", "Wisp scale (cells/block)", 0.1f, 4.0f, 0.9f),
        new Knob("uTrailSpeed", "Wisp drift (cells/s)", 0.0f, 2.0f, 0.35f),
        new Knob("uTrailBrightness", "Brightness", 0.0f, 2.0f, 1.0f),
    };


    private final ImString programId = new ImString(DEFAULT_PROGRAM);

    private final float[][] commonState = defaults(COMMON);
    private final float[][] rainbowState = defaults(RAINBOW);
    private final float[][] trailState = defaults(TRAIL);

    private final float[] trailLifetime = {ThrusterTrailManager.lifetimeS};
    private final float[] trailWidth = {ThrusterTrailManager.widthScale};
    private final float[] trailMinDist = {ThrusterTrailManager.minDist};
    private final float[] trailFadeTau = {ThrusterTrailManager.fadeTauS};
    private final ImBoolean trailEnabled = new ImBoolean(ThrusterTrailManager.enabled);
    private final int[] trailShape = {ThrusterTrailManager.shapeMode};
    private final float[] trailCore = {ThrusterTrailManager.coreGlow};
    private final float[] trailGate = {ThrusterTrailManager.gateDegrees};
    private final float[] trailHueSpan = {ThrusterTrailManager.hueSpan};
    private static final String[] TRAIL_SHAPE_NAMES = {"Ribbon", "Walls", "Profile"};



    private static float[][] defaults(Knob[] knobs) {
        float[][] state = new float[knobs.length][1];
        for (int i = 0; i < knobs.length; i++) {
            state[i][0] = knobs[i].def();
        }
        return state;
    }

    public static void register() {
        VeilEventPlatform.INSTANCE.onVeilRegisterInspectors(registry -> registry.registerInspector(new VeilPlumeInspector()));
    }

    @Override
    protected void renderComponents() {
        ImGui.inputText("Program", this.programId);
        ImGui.sameLine();
        if (ImGui.smallButton("classic##prog")) {
            this.programId.set(DEFAULT_PROGRAM);
        }
        ImGui.sameLine();
        if (ImGui.smallButton("fbm##prog")) {
            this.programId.set(FBM_PROGRAM);
        }
        ImGui.sameLine();
        if (ImGui.smallButton("trail##prog")) {
            this.programId.set(TRAIL_PROGRAM);
        }
        ResourceLocation id = ResourceLocation.tryParse(this.programId.get());
        ShaderProgram program = null;
        if (id != null) {
            Map<ResourceLocation, ShaderProgram> shaders = VeilRenderSystem.renderer().getShaderManager().getShaders();
            program = shaders.get(id);
        }
        if (program == null) {
            ImGui.textColored(0xFF5555FF, "Program not loaded - enter a world and check the id.");
            return;
        }

        if (ImGui.collapsingHeader("Common")) {
            this.section(COMMON, this.commonState, program);
            ImGui.separator();
        }
        if (ImGui.collapsingHeader("Rainbow State")) {
            this.section(RAINBOW, this.rainbowState, program);
            ImGui.separator();
        }
        if (ImGui.collapsingHeader("Trail shader")) {
            this.section(TRAIL, this.trailState, program);
            ImGui.separator();
        }
        if (ImGui.collapsingHeader("Trail manager")) {
            ImGui.checkbox("Trails enabled", this.trailEnabled);
            ImGui.sliderFloat("Lifetime (s)##trailLife", this.trailLifetime, 0.25f, 4.0f);
            ImGui.sliderFloat("Width scale##trailWidth", this.trailWidth, 0.25f, 4.0f);
            ImGui.sliderFloat("Min sample dist##trailMin", this.trailMinDist, 0.05f, 1.0f);
            ImGui.sliderFloat("Cutoff fade tau (s)##trailTau", this.trailFadeTau, 0.1f, 1.5f);
            ImGui.sameLine();
            if (ImGui.smallButton("r##trailTau")) {
                this.trailFadeTau[0] = 0.45f;
            }
            if (ImGui.smallButton("Shape: " + TRAIL_SHAPE_NAMES[Math.floorMod(this.trailShape[0], 3)] + "##trailShape")) {
                this.trailShape[0] = (this.trailShape[0] + 1) % 3;
            }
            ImGui.sameLine();
            ImGui.sliderFloat("Core glow##trailCore", this.trailCore, 0.0f, 1.0f);
            ImGui.sameLine();
            if (ImGui.smallButton("r##trailCore")) {
                this.trailCore[0] = 0.45f;
            }
            ImGui.sliderFloat("Motion gate (deg)##trailGate", this.trailGate, 10.0f, 180.0f);
            ImGui.sameLine();
            if (ImGui.smallButton("r##trailGate")) {
                this.trailGate[0] = 75.0f;
            }
            ImGui.sliderFloat("Hue sweep (trail)##trailHue", this.trailHueSpan, 0.0f, 1.0f);
            ImGui.sameLine();
            if (ImGui.smallButton("r##trailHue")) {
                this.trailHueSpan[0] = 0.0f;
            }
            ImGui.separator();
        }

        if (ImGui.button("Reset all to shader defaults")) {
            this.applyDefaults(COMMON, this.commonState);
            this.applyDefaults(RAINBOW, this.rainbowState);
            this.applyDefaults(TRAIL, this.trailState);
            this.trailLifetime[0] = 2.0f;
            this.trailWidth[0] = 1.0f;
            this.trailMinDist[0] = 0.25f;
            this.trailFadeTau[0] = 0.45f;
            this.trailEnabled.set(true);
            this.trailShape[0] = ThrusterTrailManager.SHAPE_PROFILE;
            this.trailCore[0] = 0.45f;
            this.trailGate[0] = 75.0f;
        }

        ThrusterTrailManager.enabled = this.trailEnabled.get();
        ThrusterTrailManager.lifetimeS = this.trailLifetime[0];
        ThrusterTrailManager.widthScale = this.trailWidth[0];
        ThrusterTrailManager.minDist = this.trailMinDist[0];
        ThrusterTrailManager.fadeTauS = this.trailFadeTau[0];
        ThrusterTrailManager.shapeMode = Math.floorMod(this.trailShape[0], 3);
        ThrusterTrailManager.coreGlow = this.trailCore[0];
        ThrusterTrailManager.gateDegrees = this.trailGate[0];
    }

    private void section(Knob[] knobs, float[][] state, ShaderProgram program) {
        for (int i = 0; i < knobs.length; i++) {
            Knob knob = knobs[i];
            ImGui.sliderFloat(knob.label() + "##" + knob.uniform(), state[i], knob.min(), knob.max());
            ImGui.sameLine();
            if (ImGui.smallButton("r##" + knob.uniform())) {
                state[i][0] = knob.def();
            }
            program.getUniformSafe(knob.uniform()).setFloat(state[i][0]);
        }
    }

    private void applyDefaults(Knob[] knobs, float[][] state) {
        for (int i = 0; i < knobs.length; i++) {
            state[i][0] = knobs[i].def();
        }
    }

    @Override
    public Component getDisplayName() {
        return TITLE;
    }
}
