package dev.alyti.fluxnautics.mixin.mekanism;

import mekanism.api.chemical.Chemical;
import mekanism.api.datamaps.chemical.attribute.IChemicalAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Chemical.class)
public interface ChemicalExtension {
    @Accessor("attributes")
    List<IChemicalAttribute> getAttributes();
}
