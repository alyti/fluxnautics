package dev.alyti.fluxnautics.util;

import dev.alyti.fluxnautics.client.gui.GuiAssemblyTerminalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

public class FNDistUtil {
    @Nullable
    public static Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    public static float getPartialTick() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
    }

    public static HitResult getHitResult() {
        return Minecraft.getInstance().hitResult;
    }

    public static Level getClientLevel() {
        return Minecraft.getInstance().level;
    }

    @Nullable
    public static GuiAssemblyTerminalScreen getAssemblyTerminalScreen() {
        if (Minecraft.getInstance().screen instanceof GuiAssemblyTerminalScreen screen) {
            return screen;
        }
        return null;
    }
}
