package dev.alyti.fluxnautics.mixin.compatibility.flywheel;

import com.bawnorton.mixinsquared.TargetHandler;
import dev.alyti.fluxnautics.client.CloakedSublevelCache;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.flywheel.SubLevelEmbedding;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(value = BlockEntityStorage.class, priority = 1500, remap = false)
public class BlockEntityStorageMixinMixin {

    @Unique
    private static final Matrix4f FLUXNAUTICS$HIDDEN_TRANSFORM = new Matrix4f().translation(0.0f, -999999.0f, 0.0f);

    @Unique
    private static final Matrix3f FLUXNAUTICS$HIDDEN_NORMAL = new Matrix3f();

    @Unique
    private final List<VisualEmbedding> fluxnautics$cloakedEmbeddingsToHide = new ArrayList<>();

    @TargetHandler(
            mixin = "dev.ryanhcode.sable.neoforge.mixin.compatibility.flywheel.BlockEntityStorageMixin",
            name = "sable$updateSubLevelEmbeddingsFrame"
    )
    @Redirect(
            method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;")
    )
    private Iterator<Map.Entry<ClientSubLevel, SubLevelEmbedding>> fluxnautics$trackCloakedEmbeddings(Set<Map.Entry<ClientSubLevel, SubLevelEmbedding>> instance) {
        this.fluxnautics$cloakedEmbeddingsToHide.clear();

        Iterator<Map.Entry<ClientSubLevel, SubLevelEmbedding>> originalIter = instance.iterator();

        if (CloakedSublevelCache.isEmpty()) {
            return originalIter;
        }

        return new Iterator<>() {
            private Map.Entry<ClientSubLevel, SubLevelEmbedding> lastEntry = null;

            @Override
            public boolean hasNext() {
                return originalIter.hasNext();
            }

            @Override
            public Map.Entry<ClientSubLevel, SubLevelEmbedding> next() {
                lastEntry = originalIter.next();
                if (CloakedSublevelCache.isCloaked(lastEntry.getKey())) {
                    fluxnautics$cloakedEmbeddingsToHide.add(lastEntry.getValue().embedding());
                }
                return lastEntry;
            }

            @Override
            public void remove() {
                if (lastEntry != null) {
                    fluxnautics$cloakedEmbeddingsToHide.remove(lastEntry.getValue().embedding());
                }
                originalIter.remove();
                lastEntry = null;
            }
        };
    }

    @TargetHandler(
            mixin = "dev.ryanhcode.sable.neoforge.mixin.compatibility.flywheel.BlockEntityStorageMixin",
            name = "sable$updateSubLevelEmbeddingsFrame"
    )
    @Inject(method = "@MixinSquared:Handler", at = @At("TAIL"))
    private void fluxnautics$applyHiddenTransforms(CallbackInfo ci) {
        for (VisualEmbedding embedding : this.fluxnautics$cloakedEmbeddingsToHide) {
            embedding.transforms(FLUXNAUTICS$HIDDEN_TRANSFORM, FLUXNAUTICS$HIDDEN_NORMAL);
        }
    }
}
