package dev.alyti.fluxnautics.mixin.sable;

import dev.alyti.fluxnautics.content.blocks.conduit.strand.client.ClientLevelConduitManager;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.network.client.ClientSableInterpolationState;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientSubLevelContainer.class)
public abstract class ClientSubLevelContainerMixin {
    @Shadow @Final private ClientSableInterpolationState interpolation;

    @Shadow public abstract ClientLevel getLevel();

    @Inject(method = "tick", at = @At("TAIL"))
    private void sable$tickRopeInterpolation(final CallbackInfo ci) {
        final ClientLevel level = this.getLevel();
        final ClientLevelConduitManager ropeManager = ClientLevelConduitManager.getOrCreate(level);

        ropeManager.tickInterpolation(this.interpolation.getTickPointer());
    }
}
