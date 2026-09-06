package dev.alyti.fluxnautics.content.items;

import dev.alyti.fluxnautics.content.blocks.thruster.ThrusterArrayBoosterBlockEntity;
import dev.alyti.fluxnautics.index.BoosterTier;
import dev.alyti.fluxnautics.lang.FNLang;
import mekanism.api.text.EnumColor;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.prefab.BlockBasicMultiblock;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.util.text.TextUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;


public class ItemBlockBooster extends ItemBlockTooltip<BlockBasicMultiblock<ThrusterArrayBoosterBlockEntity>> {

    public ItemBlockBooster(BlockBasicMultiblock<ThrusterArrayBoosterBlockEntity> block, Item.Properties properties) {
        super(block, false, properties);
    }

    @NotNull
    @Override
    public BoosterTier getTier() {
        return Objects.requireNonNull(Attribute.getTier(getBlock(), BoosterTier.class));
    }

//    @Override
//    protected void addDetails(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
//        super.addDetails(stack, context, tooltip, flag);
////        tooltip.add(MekanismLang.CAPABLE_OF_TRANSFERRING.translateColored(EnumColor.DARK_GRAY));
////        tooltip.add(MekanismLang.FLUIDS.translateColored(EnumColor.PURPLE, EnumColor.GRAY, MekanismLang.FORGE));
//    }

    @Override
    protected void addStats(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.addStats(stack, context, tooltip, flag);
        BoosterTier tier = getTier();
        tooltip.add(FNLang.ITEM_THRUSTER_ARRAY_BOOSTER_THRUST_CAPACITY.translateColored(EnumColor.INDIGO, EnumColor.GRAY, TextUtils.format(tier.getThrustBudget())));
    }
}