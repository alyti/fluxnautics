package dev.alyti.fluxnautics.util.placement.ghost;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class GhostBlockParams {

    protected final BlockState state;
    protected BlockPos pos;
    protected Supplier<Float> alphaSupplier;
    protected float tintRed = -1, tintGreen = -1, tintBlue = -1;

    private GhostBlockParams(BlockState state) {
        this.state = state;
        this.pos = BlockPos.ZERO;
        this.alphaSupplier = () -> 1f;
    }

    public static GhostBlockParams of(BlockState state) {
        return new GhostBlockParams(state);
    }

    public static GhostBlockParams of(Block block) {
        return of(block.defaultBlockState());
    }

    public GhostBlockParams at(BlockPos pos) {
        this.pos = pos;
        return this;
    }

    public GhostBlockParams at(int x, int y, int z) {
        return this.at(new BlockPos(x, y, z));
    }

    public GhostBlockParams alpha(Supplier<Float> alphaSupplier) {
        this.alphaSupplier = alphaSupplier;
        return this;
    }

    public GhostBlockParams alpha(float alpha) {
        return this.alpha(() -> alpha);
    }

    public GhostBlockParams breathingAlpha() {
        return this.alpha(() -> (float) GhostBlocks.getBreathingAlpha());
    }

    public GhostBlockParams tint(float red, float green, float blue) {
        this.tintRed = red;
        this.tintGreen = green;
        this.tintBlue = blue;
        return this;
    }

    public boolean isTinted() {
        return tintRed >= 0 && tintGreen >= 0 && tintBlue >= 0;
    }

    public BlockState getState() {
        return state;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Supplier<Float> getAlphaSupplier() {
        return alphaSupplier;
    }

    public float getTintRed() {
        return tintRed;
    }

    public float getTintGreen() {
        return tintGreen;
    }

    public float getTintBlue() {
        return tintBlue;
    }
}
