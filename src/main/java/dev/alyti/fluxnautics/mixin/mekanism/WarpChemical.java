package dev.alyti.fluxnautics.mixin.mekanism;

import mekanism.api.chemical.Chemical;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chemical.class)
public abstract class WarpChemical implements ChemicalExtension {
    @Inject(method = "updateFromDataMap", at = @At("TAIL"))
    public void updateFromDataMap(Holder<Chemical> holder, CallbackInfo ci) {
//        IChemicalAttribute attribute = holder.getData(DataMapTypeRegistry.WARP_FUEL);
//        List<IChemicalAttribute> att = ((ChemicalExtension) this).getAttributes();
//        att.add(attribute);
    }
}
