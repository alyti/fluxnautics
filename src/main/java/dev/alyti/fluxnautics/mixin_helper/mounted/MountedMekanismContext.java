package dev.alyti.fluxnautics.mixin_helper.mounted;

import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record MountedMekanismContext(
    ServerLevel level,
    SubLevel subLevel,
    UUID subLevelId,
    BlockPos localBlockPos,
    Vec3 localCenter,
    Vec3 globalCenter,
    BlockPos globalBlockPos
) {
}