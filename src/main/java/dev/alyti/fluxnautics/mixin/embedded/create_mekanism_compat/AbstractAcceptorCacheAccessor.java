package dev.alyti.fluxnautics.mixin.embedded.create_mekanism_compat;

import mekanism.common.lib.transmitter.acceptor.AbstractAcceptorCache;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = AbstractAcceptorCache.class, remap = false)
public interface AbstractAcceptorCacheAccessor {
    @Accessor("cachedAcceptors")
    Map<Direction, ?> cmc$getCachedAcceptors();

    @Accessor("cachedListeners")
    Map<Direction, ?> cmc$getCachedListeners();
}