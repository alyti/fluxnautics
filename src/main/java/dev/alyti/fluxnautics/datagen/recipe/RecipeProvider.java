package dev.alyti.fluxnautics.datagen.recipe;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.items.drone.MekaRobitItem;
import dev.alyti.fluxnautics.datagen.recipe.builder.ExtendedShapedRecipeBuilder;
import dev.alyti.fluxnautics.datagen.recipe.pattern.RecipePattern;
import dev.alyti.fluxnautics.datagen.recipe.pattern.RecipePattern.TripleLine;
import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.alyti.fluxnautics.registry.FNItems;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.datagen.recipe.builder.NucleosynthesizingRecipeBuilder;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismChemicals;
import mekanism.common.registries.MekanismItems;
import mekanism.common.tags.MekanismTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@NothingNullByDefault
public class RecipeProvider extends BaseRecipeProvider {


    public RecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, ExistingFileHelper existingFileHelper) {
        super(output, provider, existingFileHelper);
    }

    @Override
    protected void addRecipes(RecipeOutput consumer, HolderLookup.Provider registries) {
        addThrusterRecipes(consumer);
        addUtilityRecipes(consumer);
        addForcefieldRecipes(consumer);
        addConduitRecipes(consumer);
        addMekaRobitRecipes(consumer);
        addFluxTetherRecipes(consumer);
    }

    private void addFluxTetherRecipes(RecipeOutput consumer) {
        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.FLUX_TETHER)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('C', 'B', 'A'),
                TripleLine.of('B', 'Q', 'B'),
                TripleLine.of('A', 'B', 'C')
            ))
            .key('C', MekanismTags.Items.CIRCUITS_ULTIMATE)
            .key('A', FNBlocks.CONDUIT_CONNECTOR)
            .key('B', MekanismBlocks.LASER_AMPLIFIER)
            .key('Q', MekanismItems.TELEPORTATION_CORE)
            .build(consumer);
    }

    private  void addMekaRobitRecipes(RecipeOutput consumer) {
        ExtendedShapedRecipeBuilder.shapedRecipe(FNItems.MEKA_ROBIT)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('C', 'S', 'C'),
                TripleLine.of('B', 'Q', 'B'),
                TripleLine.of('A', 'M', 'A')
            ))
            .key('C', MekanismItems.ULTIMATE_CONTROL_CIRCUIT)
            .key('S', Items.NETHER_STAR)
            .key('Q', MekanismItems.ROBIT)
            .key('M', MekanismBlocks.ULTIMATE_INDUCTION_CELL)
            .key('B', Items.NETHERITE_INGOT)
            .key('A', MekanismTags.Items.PELLETS_POLONIUM)
            .category(RecipeCategory.COMBAT)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNItems.MODULE_SWARM_UNIT)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('A', 'S', 'A'),
                TripleLine.of('M', 'Q', 'M'),
                TripleLine.of('B', 'B', 'B')
            ))
            .key('S', Items.NETHER_STAR)
            .key('Q', MekanismItems.MODULE_BASE)
            .key('M', FNItems.MEKA_ROBIT)
            .key('B', Items.NETHERITE_INGOT)
            .key('A', MekanismTags.Items.ALLOYS_ULTIMATE)
            .category(RecipeCategory.COMBAT)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.DAEMON_PROTOCOL)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('C', 'S', 'C'),
                TripleLine.of('B', 'Q', 'B'),
                TripleLine.of('A', 'M', 'A')
            ))
            .key('S', Items.NETHER_STAR)
            .key('Q', MekanismBlocks.DIGITAL_MINER)
            .key('M', MekanismBlocks.ULTIMATE_INDUCTION_CELL)
            .key('C', MekanismItems.ULTIMATE_CONTROL_CIRCUIT)
            .key('B', Items.NETHERITE_BLOCK)
            .key('A', MekanismTags.Items.PELLETS_ANTIMATTER)
            .category(RecipeCategory.COMBAT)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNItems.MODULE_QUANTUM_PULL_UNIT)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('A', 'S', 'A'),
                TripleLine.of('M', 'Q', 'M'),
                TripleLine.of('B', 'B', 'B')
            ))
            .key('Q', MekanismItems.MODULE_MAGNETIC_ATTRACTION)
            .key('S', MekanismBlocks.QIO_IMPORTER)
            .key('M', MekanismTags.Items.CIRCUITS_ULTIMATE)
            .key('B', MekanismItems.HDPE_SHEET)
            .key('A', MekanismTags.Items.ALLOYS_ULTIMATE)
            .category(RecipeCategory.TOOLS)
            .build(consumer);
    }

    private void addConduitRecipes(RecipeOutput consumer) {
        ExtendedShapedRecipeBuilder.shapedRecipe(FNItems.CONDUIT_CORE, 4)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('C', 'A', 'C'),
                TripleLine.of('A', 'Q', 'A'),
                TripleLine.of('C', 'A', 'C')
            ))
            .key('C', MekanismItems.ELITE_CONTROL_CIRCUIT)
            .key('A', MekanismItems.ATOMIC_ALLOY)
            .key('Q', MekanismItems.REFINED_OBSIDIAN_INGOT)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.CONDUIT_CONNECTOR, 2)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('C', 'B', 'C'),
                TripleLine.of('B', 'Q', 'B'),
                TripleLine.of('C', 'B', 'C')
            ))
            .key('C', MekanismItems.ULTIMATE_CONTROL_CIRCUIT)
            .key('Q', MekanismBlocks.LASER_TRACTOR_BEAM)
            .key('B', MekanismBlocks.LASER)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.CONDUIT_PORT, 2)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('C', 'S', 'C'),
                TripleLine.of('S', 'Q', 'S'),
                TripleLine.of('C', 'S', 'C')
            ))
            .key('C', MekanismItems.ELITE_CONTROL_CIRCUIT)
            .key('S', MekanismItems.REINFORCED_ALLOY)
            .key('Q', MekanismBlocks.QUANTUM_ENTANGLOPORTER)
            .build(consumer);
    }

    private void addForcefieldRecipes(RecipeOutput consumer) {
        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.FORCEFIELD_EMITTER)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('C', 'S', 'C'),
                TripleLine.of('B', 'Q', 'B'),
                TripleLine.of('A', 'M', 'A')
            ))
            .key('C', MekanismItems.ULTIMATE_CONTROL_CIRCUIT)
            .key('S', FNBlocks.FORCEFIELD_HARMONIZER)
            .key('Q', MekanismBlocks.QUANTUM_ENTANGLOPORTER)
            .key('B', MekanismBlocks.ULTIMATE_INDUCTION_CELL)
            .key('M', FNItems.FORCEFIELD_MATRIX)
            .key('A', Blocks.NETHERITE_BLOCK.asItem())
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.FORCEFIELD_HARMONIZER)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('C', 'S', 'C'),
                TripleLine.of('B', 'Q', 'B'),
                TripleLine.of('A', 'A', 'A')
            ))
            .key('C', MekanismItems.REINFORCED_ALLOY)
            .key('S', MekanismItems.NETWORK_READER)
            .key('Q', MekanismBlocks.STEEL_CASING)
            .key('B', MekanismItems.ADVANCED_CONTROL_CIRCUIT)
            .key('A', MekanismItems.ENERGY_TABLET)
            .build(consumer);

        String basePath = "nucleosynthesizing/";
        NucleosynthesizingRecipeBuilder.nucleosynthesizing(
            IngredientCreatorAccess.item().from(MekanismBlocks.TELEPORTER),
            IngredientCreatorAccess.chemicalStack().fromHolder(MekanismChemicals.ANTIMATTER, 5),
            new ItemStack(FNItems.FORCEFIELD_MATRIX.asItem()),
            1000,
            true
        ).build(consumer, Fluxnautics.id(basePath + "forcefield_matrix"));
    }

    private void addUtilityRecipes(RecipeOutput consumer) {
        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.FIELD_DRIVE)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('C', 'S', 'C'),
                TripleLine.of('B', 'Q', 'B'),
                TripleLine.of('A', 'A', 'A')
            ))
            .key('C', MekanismItems.ULTIMATE_CONTROL_CIRCUIT)
            .key('S', Items.NETHER_STAR)
            .key('Q', MekanismBlocks.QUANTUM_ENTANGLOPORTER)
            .key('B', MekanismBlocks.ULTIMATE_INDUCTION_CELL)
            .key('A', MekanismItems.ANTIMATTER_PELLET)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.HOLO)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('C', 'S', 'C'),
                TripleLine.of('B', 'Q', 'B'),
                TripleLine.of('C', 'S', 'C')
            ))
            .key('Q', MekanismBlocks.QUANTUM_ENTANGLOPORTER)
            .key('C', MekanismItems.HDPE_SHEET)
            .key('B', MekanismItems.ULTIMATE_CONTROL_CIRCUIT)
            .key('S', MekanismBlocks.SEISMIC_VIBRATOR)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.SPATIAL_ANCHOR)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('C', 'S', 'C'),
                TripleLine.of('B', 'Q', 'B'),
                TripleLine.of('D', 'S', 'D')
            ))
            .key('D', FNBlocks.INERTIA_DAMPENER)
            .key('C', FNBlocks.STABILIZER)
            .key('Q', FNBlocks.ANTI_GRAV)
            .key('B', MekanismItems.ULTIMATE_CONTROL_CIRCUIT)
            .key('S', MekanismBlocks.DIMENSIONAL_STABILIZER)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.ANTI_GRAV)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('C', 'S', 'C'),
                TripleLine.of('B', 'Q', 'B'),
                TripleLine.of('C', 'S', 'C')
            ))
            .key('Q', MekanismItems.TELEPORTATION_CORE)
            .key('C', MekanismItems.ANTIMATTER_PELLET)
            .key('B', MekanismItems.ULTIMATE_CONTROL_CIRCUIT)
            .key('S', MekanismBlocks.SUPERCHARGED_COIL)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNItems.MODULE_DECLOAKER)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('B', 'C', 'B'),
                TripleLine.of('B', 'Q', 'B'),
                TripleLine.of('S', 'S', 'S')
            ))
            .key('C', MekanismItems.REFINED_GLOWSTONE_NUGGET)
            .key('Q', MekanismItems.MODULE_BASE)
            .key('B', MekanismItems.ATOMIC_ALLOY)
            .key('S', MekanismItems.POLONIUM_PELLET)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.CLOAK_GENERATOR)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('C', 'S', 'C'),
                TripleLine.of('B', 'Q', 'B'),
                TripleLine.of('A', 'M', 'A')
            ))
            .key('C', MekanismItems.ATOMIC_ALLOY)
            .key('S', Items.FERMENTED_SPIDER_EYE)
            .key('B', MekanismItems.ULTIMATE_CONTROL_CIRCUIT)
            .key('Q', MekanismBlocks.QUANTUM_ENTANGLOPORTER)
            .key('A', MekanismItems.TELEPORTATION_CORE)
            .key('M', MekanismItems.ANTIMATTER_PELLET)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.STABILIZER)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('C', 'S', 'C'),
                TripleLine.of('B', 'Q', 'B'),
                TripleLine.of('A', 'M', 'A')
            ))
            .key('C', MekanismItems.ATOMIC_ALLOY)
            .key('S', Items.ENDER_PEARL)
            .key('B', MekanismItems.ULTIMATE_CONTROL_CIRCUIT)
            .key('Q', MekanismBlocks.QUANTUM_ENTANGLOPORTER)
            .key('A', MekanismItems.ANTIMATTER_PELLET)
            .key('M', MekanismItems.MODULE_GYROSCOPIC_STABILIZATION)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.INERTIA_DAMPENER)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('C', 'S', 'C'),
                TripleLine.of('B', 'Q', 'B'),
                TripleLine.of('A', 'M', 'A')
            ))
            .key('C', MekanismItems.ATOMIC_ALLOY)
            .key('S', Items.ENDER_PEARL)
            .key('B', MekanismItems.ULTIMATE_CONTROL_CIRCUIT)
            .key('Q', MekanismBlocks.QUANTUM_ENTANGLOPORTER)
            .key('A', MekanismItems.ANTIMATTER_PELLET)
            .key('M', MekanismItems.MODULE_GRAVITATIONAL_MODULATING)
            .build(consumer);
    }

    private void addThrusterRecipes(RecipeOutput consumer) {
        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.THRUSTER_ARRAY_STRUCTURAL, 4)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('A', 'B', 'A'),
                TripleLine.of('B', 'S', 'B'),
                TripleLine.of('A', 'B', 'A'))
            )
            .key('S', MekanismBlocks.STEEL_CASING)
            .key('A', MekanismItems.REFINED_GLOWSTONE_NUGGET)
            .key('B', MekanismItems.BASIC_CONTROL_CIRCUIT)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.THRUSTER_ARRAY_CONTROLLER)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('A', 'B', 'A'),
                TripleLine.of('B', 'S', 'B'),
                TripleLine.of('A', 'B', 'A'))
            )
            .key('S', FNBlocks.THRUSTER_ARRAY_STRUCTURAL)
            .key('A', MekanismItems.ADVANCED_CONTROL_CIRCUIT)
            .key('B', FNBlocks.THRUSTER_ARRAY_SUBCONTROLLER)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.THRUSTER_ARRAY_SUBCONTROLLER, 4)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('A', 'B', 'A'),
                TripleLine.of('B', 'S', 'B'),
                TripleLine.of('A', 'B', 'A'))
            )
            .key('S', FNBlocks.THRUSTER_ARRAY_STRUCTURAL)
            .key('A', MekanismItems.BASIC_CONTROL_CIRCUIT)
            .key('B', MekanismItems.ELECTROLYTIC_CORE)
            .build(consumer);


        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.ION_THRUSTER)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('G', 'I', 'G'),
                TripleLine.of('I', 'S', 'I'),
                TripleLine.of('G', 'I', 'G'))
            ).key('G', MekanismTags.Items.ENRICHED_DIAMOND)
            .key('S', FNBlocks.THRUSTER_ARRAY_STRUCTURAL)
            .key('I', MekanismBlocks.LASER)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.THRUSTER_ARRAY_ALIGNMENT_X)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('G', ' ', 'G'),
                TripleLine.of('I', 'S', 'I'),
                TripleLine.of('G', ' ', 'G'))
            ).key('I', MekanismTags.Items.PELLETS_ANTIMATTER)
            .key('S', FNBlocks.THRUSTER_ARRAY_STRUCTURAL)
            .key('G', MekanismBlocks.LASER_TRACTOR_BEAM)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.THRUSTER_ARRAY_ALIGNMENT_Z)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('G', 'I', 'G'),
                TripleLine.of(' ', 'S', ' '),
                TripleLine.of('G', 'I', 'G'))
            ).key('I', MekanismTags.Items.PELLETS_ANTIMATTER)
            .key('S', FNBlocks.THRUSTER_ARRAY_STRUCTURAL)
            .key('G', MekanismBlocks.LASER_TRACTOR_BEAM)
            .build(consumer);

        ExtendedShapedRecipeBuilder.shapedRecipe(FNBlocks.THRUSTER_ARRAY_ALIGNMENT_Y)
            .pattern(RecipePattern.createPattern(
                TripleLine.of('G', 'I', 'G'),
                TripleLine.of('I', 'S', 'I'),
                TripleLine.of('G', 'I', 'G'))
            ).key('I', MekanismTags.Items.PELLETS_ANTIMATTER)
            .key('S', FNBlocks.THRUSTER_ARRAY_STRUCTURAL)
            .key('G', MekanismBlocks.LASER_TRACTOR_BEAM)
            .build(consumer);

        addTieredBooster(consumer, FNBlocks.THRUSTER_ARRAY_BOOSTER_BASIC, MekanismBlocks.LASER_AMPLIFIER, MekanismBlocks.BASIC_INDUCTION_PROVIDER);
        addTieredBooster(consumer, FNBlocks.THRUSTER_ARRAY_BOOSTER_ADVANCED, FNBlocks.THRUSTER_ARRAY_BOOSTER_BASIC, MekanismBlocks.ADVANCED_INDUCTION_PROVIDER);
        addTieredBooster(consumer, FNBlocks.THRUSTER_ARRAY_BOOSTER_ELITE, FNBlocks.THRUSTER_ARRAY_BOOSTER_ADVANCED, MekanismBlocks.ELITE_INDUCTION_PROVIDER);
        addTieredBooster(consumer, FNBlocks.THRUSTER_ARRAY_BOOSTER_ULTIMATE, FNBlocks.THRUSTER_ARRAY_BOOSTER_ELITE, MekanismBlocks.ULTIMATE_INDUCTION_PROVIDER);
    }


    private static final RecipePattern BOOSTER_PATTERN = RecipePattern.createPattern(
        TripleLine.of('X', 'I', 'X'),
        TripleLine.of('P', 'O', 'P'),
        TripleLine.of('X', 'I', 'X'));

    private void addTieredBooster(RecipeOutput consumer, BlockRegistryObject<?, ?> booster, BlockRegistryObject<?, ?> previousBooster, BlockRegistryObject<?, ?> tierItem) {
        String tierName = Objects.requireNonNull(Attribute.getBaseTier(booster)).getLowerName();
        ExtendedShapedRecipeBuilder.shapedRecipe(booster)
            .pattern(BOOSTER_PATTERN)
            .key('O', FNBlocks.THRUSTER_ARRAY_STRUCTURAL)
            .key('P', previousBooster)
            .key('I', tierItem)
            .key('X', MekanismItems.HDPE_SHEET)
            .build(consumer, Fluxnautics.id("booster/" + tierName));
    }
}