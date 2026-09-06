package dev.alyti.fluxnautics.mixin.mekanism;

import dev.alyti.fluxnautics.index.ExtraWarningTypes;
import dev.alyti.fluxnautics.lang.FNLang;
import mekanism.api.text.ILangEntry;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;

@Mixin(value = WarningType.class, remap = false)
public class WarningTypeMixin {
    @Shadow(remap = false)
    @Final
    @Mutable
    private static WarningType[] $VALUES;

    public WarningTypeMixin() {}


    @SuppressWarnings("InvokerTarget")
    @Invoker("<init>")
    public static WarningType warningType$init(String internalName, int order, ILangEntry langEntry, int expected) {
        throw new AssertionError();
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void warningTypeClinit(CallbackInfo ci) {
        ExtraWarningTypes.NOT_ENOUGH_WARPFUEL = fluxnautics$addVariant("NOT_ENOUGH_WARPFUEL", FNLang.ISSUE_NOT_ENOUGH_WARPFUEL, 1);
    }

    @Unique
    private static WarningType fluxnautics$addVariant(String name, ILangEntry locale, int expected) {
        ArrayList<WarningType> variants = new ArrayList<>(Arrays.asList($VALUES));
        WarningType upgrade = warningType$init(name, variants.get(variants.size() - 1).ordinal() + 1, locale, expected);
        variants.add(upgrade);
        WarningTypeMixin.$VALUES = variants.toArray(new WarningType[0]);
        return upgrade;
    }
}
