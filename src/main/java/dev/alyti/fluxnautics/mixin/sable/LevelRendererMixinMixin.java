package dev.alyti.fluxnautics.mixin.sable;

import com.bawnorton.mixinsquared.TargetHandler;
import dev.alyti.fluxnautics.client.CloakedSublevelCache;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = LevelRenderer.class, priority = 1500)
public class LevelRendererMixinMixin {

    @TargetHandler(
            mixin = "dev.ryanhcode.sable.mixin.debug_render.LevelRendererMixin",
            name = "renderLevel"
    )
    @Redirect(method = "@MixinSquared:Handler", at = @At( value = "INVOKE", target = "Ldev/ryanhcode/sable/api/sublevel/SubLevelContainer;getAllSubLevels()Ljava/util/List;"))
    private List<? extends SubLevel> fluxnautics$renderLevelCloakedDebug(SubLevelContainer instance) {
        return fluxnautics$filterCloakedSublevels((instance.getAllSubLevels()));
    }

    @Unique
    private static List<? extends SubLevel> fluxnautics$filterCloakedSublevels(List<? extends SubLevel> sublevels) {
        if (CloakedSublevelCache.isEmpty()) {
            return sublevels;
        }

        List<SubLevel> filtered = new ArrayList<>();
        for (SubLevel sublevel : sublevels) {
            if (sublevel instanceof ClientSubLevel clientSubLevel) {
                if (!CloakedSublevelCache.isCloaked(clientSubLevel)) {
                    filtered.add(clientSubLevel);
                }
            } else {
                filtered.add(sublevel);
            }
        }
        return filtered;
    }
}
