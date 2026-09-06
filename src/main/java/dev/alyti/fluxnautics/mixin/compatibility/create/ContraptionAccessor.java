package dev.alyti.fluxnautics.mixin.compatibility.create;

import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Contraption.class)
public interface ContraptionAccessor {

    @Accessor("superglue")
    List<AABB> getSuperGlue();

}