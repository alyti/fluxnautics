package dev.alyti.fluxnautics.content.blocks.assembly_terminal;

import dev.alyti.fluxnautics.registry.FNBlocks;
import dev.alyti.fluxnautics.registry.FNDataComponents;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.NBTUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class AssemblyTerminalBlockEntity extends TileEntityMekanism {

    public BlueprintConfig blueprintConfig;

    public AssemblyTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(FNBlocks.ASSEMBLY_TERMINAL, pos, state);
        this.blueprintConfig = BlueprintConfig.makeDefault(this);
    }

    @Override
    public CompoundTag getConfigurationData(HolderLookup.Provider provider, Player player) {
        CompoundTag data = super.getConfigurationData(provider, player);
        data.put("BPConfig", BlueprintConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.blueprintConfig).getOrThrow());

        return data;
    }

    @Override
    public void setConfigurationData(HolderLookup.Provider provider, Player player, CompoundTag data) {
        super.setConfigurationData(provider, player, data);
        NBTUtils.setCompoundIfPresent(data, "BPConfig", (CompoundTag config) -> {
            this.blueprintConfig = BlueprintConfig.CODEC.parse(NbtOps.INSTANCE, config).getOrThrow();
        });
    }

    @Override
    protected void applyImplicitComponents(@NotNull BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        this.setBlueprintConfig(input.getOrDefault(FNDataComponents.BLUEPRINT_CONFIG, BlueprintConfig.makeDefault(this)));
    }

    @Override
    protected void collectImplicitComponents(@NotNull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(FNDataComponents.BLUEPRINT_CONFIG, this.blueprintConfig);
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag(@NotNull HolderLookup.Provider provider) {
        CompoundTag updateTag = super.getReducedUpdateTag(provider);
        if (this.blueprintConfig != null) {
            updateTag.put("BPConfig", BlueprintConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.blueprintConfig).getOrThrow());
        }
        return updateTag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);

        NBTUtils.setCompoundIfPresent(tag, "BPConfig", (CompoundTag config) -> {
            this.blueprintConfig = BlueprintConfig.CODEC.parse(NbtOps.INSTANCE, config).getOrThrow();
        });
        if (this.blueprintConfig == null) {
            this.blueprintConfig = BlueprintConfig.makeDefault(this);
        }
    }

    @Override
    public void writeSustainedData(HolderLookup.Provider provider, CompoundTag dataMap) {
        super.writeSustainedData(provider, dataMap);
        if (this.blueprintConfig != null) {
            dataMap.put("BPConfig", BlueprintConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.blueprintConfig).getOrThrow());
        }
    }

    @Override
    public void readSustainedData(HolderLookup.Provider provider, @NotNull CompoundTag dataMap) {
        super.readSustainedData(provider, dataMap);
        NBTUtils.setCompoundIfPresent(dataMap, "BPConfig", (CompoundTag config) -> {
            this.blueprintConfig = BlueprintConfig.CODEC.parse(NbtOps.INSTANCE, config).getOrThrow();
        });
        if (this.blueprintConfig == null) {
            this.blueprintConfig = BlueprintConfig.makeDefault(this);
        }
    }

    public void setBlueprintConfig(BlueprintConfig config) {
        this.blueprintConfig = config;
    }
}
