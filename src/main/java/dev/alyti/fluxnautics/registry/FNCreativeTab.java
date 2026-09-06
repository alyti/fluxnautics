package dev.alyti.fluxnautics.registry;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.items.drone.MekaRobitItem;
import dev.alyti.fluxnautics.lang.FNLang;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

public class FNCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Fluxnautics.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SF_ITEM_TAB = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
        .icon(() -> FNBlocks.FORCEFIELD_EMITTER.asItem().getDefaultInstance())
        .title(Component.translatable(FNLang.GUI_ITEM_GROUP.getTranslationKey()))
        .displayItems((displayParameters, output) -> {

            output.accept(FNBlocks.ION_THRUSTER);
            output.accept(FNBlocks.THRUSTER_ARRAY_STRUCTURAL);
            output.accept(FNBlocks.THRUSTER_ARRAY_CONTROLLER);
            output.accept(FNBlocks.THRUSTER_ARRAY_SUBCONTROLLER);
            output.accept(FNBlocks.THRUSTER_ARRAY_BOOSTER_BASIC);
            output.accept(FNBlocks.THRUSTER_ARRAY_BOOSTER_ADVANCED);
            output.accept(FNBlocks.THRUSTER_ARRAY_BOOSTER_ELITE);
            output.accept(FNBlocks.THRUSTER_ARRAY_BOOSTER_ULTIMATE);
            output.accept(FNBlocks.THRUSTER_ARRAY_BOOSTER_CREATIVE);
            output.accept(FNBlocks.THRUSTER_ARRAY_ALIGNMENT_X);
            output.accept(FNBlocks.THRUSTER_ARRAY_ALIGNMENT_Y);
            output.accept(FNBlocks.THRUSTER_ARRAY_ALIGNMENT_Z);

            output.accept(FNBlocks.FORCEFIELD_EMITTER);
            output.accept(FNBlocks.FORCEFIELD_HARMONIZER);
            output.accept(FNItems.FORCEFIELD_MATRIX.get());

            output.accept(FNBlocks.INERTIA_DAMPENER);
            output.accept(FNBlocks.STABILIZER);
            output.accept(FNBlocks.ANTI_GRAV);
            output.accept(FNBlocks.SPATIAL_ANCHOR);

            output.accept(FNBlocks.FIELD_DRIVE);

            output.accept(FNBlocks.CONDUIT_CONNECTOR);
            output.accept(FNBlocks.CONDUIT_PORT);
            output.accept(FNItems.CONDUIT_CORE);
            output.accept(FNBlocks.FLUX_TETHER);

            output.accept(FNBlocks.DAEMON_PROTOCOL);
            FNItems.MEKA_ROBIT.get().addItems(FNItems.MEKA_ROBIT.get().getDefaultInstance().getItemHolder(), output::accept, displayParameters.holders());
            output.accept(FNItems.MODULE_SWARM_UNIT);
            output.accept(FNItems.MODULE_QUANTUM_PULL_UNIT);

            output.accept(FNBlocks.CLOAK_GENERATOR);
            output.accept(FNItems.MODULE_DECLOAKER.get());

            output.accept(FNBlocks.ASSEMBLY_TERMINAL);
            output.accept(FNItems.MODULE_FIELD_MANIPULATOR_UNIT);

            ItemStack guideBookStack = getGuideBookStack();
            if (guideBookStack != null) {
                output.accept(guideBookStack);
            }
        })
        .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }

    @Nullable
    private static ItemStack getGuideBookStack() {
        Item guideItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("guideme", "guide"));
        if (guideItem == Items.AIR) return null;

        // noinspection unchecked
        DataComponentType<ResourceLocation> guideComponent = (DataComponentType<ResourceLocation>) BuiltInRegistries.DATA_COMPONENT_TYPE
            .get(ResourceLocation.fromNamespaceAndPath("guideme", "guide_id"));
        if (guideComponent == null) return null;

        ItemStack guideStack = guideItem.getDefaultInstance();
        guideStack.set(guideComponent, Fluxnautics.id("guide"));

        return guideStack;
    }
}