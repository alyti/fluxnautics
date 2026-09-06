package dev.alyti.fluxnautics.index;

import mekanism.common.content.qio.filter.QIOFilter;
import mekanism.common.lib.inventory.Finder;

public abstract class HarmonicResonanceFilter<FILTER extends HarmonicResonanceFilter<FILTER>> extends QIOFilter<FILTER> {

    //Note: This method exists so that if we add any qio filter specific stuff, we can easily add support for it here
//    protected static <FILTER extends HarmonicResonanceFilter<FILTER>> P1<Mu<FILTER>, Boolean> baseQIOCodec(Instance<FILTER> instance) {
//        return baseCodec(instance)
//                ;
//    }
//
//    //Note: This method exists so that if we add any qio filter specific stuff, we can easily add support for it here
//    protected static <FILTER extends HarmonicResonanceFilter<FILTER>> StreamCodec<ByteBuf, FILTER> baseQIOStreamCodec(Supplier<FILTER> constructor) {
//        return baseStreamCodec(constructor);
//    }

    protected HarmonicResonanceFilter() {
    }

    protected HarmonicResonanceFilter(boolean enabled) {
        super(enabled);
    }

    protected HarmonicResonanceFilter(FILTER filter) {
        super(filter);
    }

    public abstract Finder getFinder();

    @Override
//    @ComputerMethod(threadSafe = true)
    public abstract FILTER clone();
}