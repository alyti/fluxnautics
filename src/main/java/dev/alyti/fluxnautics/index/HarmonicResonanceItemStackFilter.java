package dev.alyti.fluxnautics.index;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mekanism.api.SerializationConstants;
import mekanism.common.content.filter.FilterType;
import mekanism.common.content.filter.IItemStackFilter;
import mekanism.common.lib.inventory.Finder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class HarmonicResonanceItemStackFilter extends HarmonicResonanceFilter<HarmonicResonanceItemStackFilter> implements IItemStackFilter<HarmonicResonanceItemStackFilter> {

    public static final MapCodec<HarmonicResonanceItemStackFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> baseQIOCodec(instance)
            .and(ItemStack.SINGLE_ITEM_CODEC.fieldOf(SerializationConstants.TARGET_STACK).forGetter(HarmonicResonanceItemStackFilter::getItemStack))
            .apply(instance, HarmonicResonanceItemStackFilter::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, HarmonicResonanceItemStackFilter> STREAM_CODEC = StreamCodec.composite(
            baseQIOStreamCodec(HarmonicResonanceItemStackFilter::new), Function.identity(),
            ItemStack.OPTIONAL_STREAM_CODEC, HarmonicResonanceItemStackFilter::getItemStack,
            (filter, itemType) -> {
                filter.itemType = itemType;
                return filter;
            }
    );

    private ItemStack itemType = ItemStack.EMPTY;

    public HarmonicResonanceItemStackFilter() {
    }

    protected HarmonicResonanceItemStackFilter(boolean enabled, ItemStack itemType) {
        super(enabled);
        this.itemType = itemType;
    }

    public HarmonicResonanceItemStackFilter(HarmonicResonanceItemStackFilter filter) {
        super(filter);
        this.itemType = filter.itemType.copy();
    }

    @Override
    public Finder getFinder() {
        return Finder.strict(itemType);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + itemType.getItem().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass() || !super.equals(o)) {
            return false;
        }
        HarmonicResonanceItemStackFilter other = (HarmonicResonanceItemStackFilter) o;
        return ItemStack.isSameItemSameComponents(itemType, other.itemType);
    }

    @Override
    public HarmonicResonanceItemStackFilter clone() {
        return new HarmonicResonanceItemStackFilter(this);
    }

    @Override
    public FilterType getFilterType() {
        return ExtraFilterTypes.HARMONIC_RESONANCE_ITEM_STACK_FILTER;
    }

    @NotNull
    @Override
    public ItemStack getItemStack() {
        return itemType;
    }

    @Override
    public void setItemStack(@NotNull ItemStack stack) {
        itemType = stack;
    }
}