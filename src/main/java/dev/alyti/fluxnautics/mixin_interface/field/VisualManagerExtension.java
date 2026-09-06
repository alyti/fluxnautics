package dev.alyti.fluxnautics.mixin_interface.field;

import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.flywheel.SubLevelEmbedding;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;

public interface VisualManagerExtension {
    SubLevelEmbedding fluxnautics$getBEEmbeddingInfo(ClientSubLevel subLevel);
}