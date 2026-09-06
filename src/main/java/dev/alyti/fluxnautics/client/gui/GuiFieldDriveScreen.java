package dev.alyti.fluxnautics.client.gui;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.blocks.field.FieldDriveBlockEntity;
import dev.alyti.fluxnautics.index.ExtraWarningTypes;
import dev.alyti.fluxnautics.lang.FNLang;
import dev.alyti.fluxnautics.network.packets.PacketFluxGuiInteract;
import dev.alyti.fluxnautics.network.packets.PacketFluxGuiInteract.GuiFluxInteraction;
import mekanism.api.functions.CharPredicate;
import mekanism.api.security.IBlockSecurityUtils;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiDigitalSwitch;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.gui.element.tab.GuiRedstoneControlTab;
import mekanism.client.gui.element.tab.window.GuiUpgradeWindowTab;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.warning.WarningTracker;
import mekanism.common.network.PacketUtils;
import mekanism.common.util.text.EnergyDisplay;
import mekanism.common.util.text.InputValidator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GuiFieldDriveScreen extends GuiConfigurableTile<FieldDriveBlockEntity, MekanismTileContainer<FieldDriveBlockEntity>> {
    private static final ResourceLocation SWITCH = Fluxnautics.id("gui/switch/switch.png");

    public static final CharPredicate DECIMAL_OR_NEGATIVE = InputValidator.DIGIT_OR_NEGATIVE.or(InputValidator.from('.'));

    @Nullable
    private GuiUpgradeWindowTab upgradeWindowTab;
    private GuiTextField impulseMult, impulseVecX, impulseVecY, impulseVecZ;
    protected GuiInnerScreen innerScreen;

    public GuiFieldDriveScreen(MekanismTileContainer<FieldDriveBlockEntity> container, Inventory inv, Component title) {
        super(container, inv, title);
        dynamicSlots = true;
        imageHeight += 5;
        inventoryLabelY += 5;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();

        addRenderableWidget(new GuiEnergyTab(this, () -> {
            //Note: This isn't the most accurate using calculation as deactivation doesn't sync instantly
            // to the client, but it is close enough given a lot more things would have to be kept track of otherwise
            // which would lead to higher memory usage
            long using = tile.isActivatable() ? tile.getEnergyContainer().getEnergyPerTick() : 0L;
            return List.of(MekanismLang.USING.translate(EnergyDisplay.of(using)),
                    MekanismLang.NEEDED.translate(EnergyDisplay.of(tile.getEnergyCost())));
        }));
        addRenderableWidget(new GuiChemicalGauge(() -> tile.gasTank, () -> tile.getChemicalTanks(null), GaugeType.STANDARD, this, 5, 15).warning(ExtraWarningTypes.NOT_ENOUGH_WARPFUEL, () -> tile.gasTank.getStored() < tile.getWarpfuelCost()));
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 163, 15, 60).warning(WarningTracker.WarningType.NOT_ENOUGH_ENERGY, () -> tile.getEnergyContainer().getEnergy() < tile.getEnergyCost()));
        addRenderableWidget(new GuiProgress(tile::getScaledProgress, ProgressType.LARGE_RIGHT, this, 65, 75));

        innerScreen = addRenderableWidget(new GuiInnerScreen(this, 25, 15, 137, 50));

        impulseMult = addRenderableWidget(new GuiTextField(this, 25 + 90, 17, 44, 8));
        impulseMult.setMaxLength(7);
        impulseMult.setInputValidator(DECIMAL_OR_NEGATIVE);
        impulseMult.configureDigitalBorderInput(() -> setText(impulseMult, GuiFluxInteraction.IMPULSE_MULT));
        impulseVecX = addRenderableWidget(new GuiTextField(this, 25 + 90, 17 + 9, 44, 8));
        impulseVecX.setMaxLength(7);
        impulseVecX.setInputValidator(DECIMAL_OR_NEGATIVE);
        impulseVecX.configureDigitalBorderInput(() -> setText(impulseVecX, GuiFluxInteraction.IMPULSE_VEC_X));
        impulseVecY = addRenderableWidget(new GuiTextField(this, 25 + 90, 17 + 18, 44, 8));
        impulseVecY.setMaxLength(7);
        impulseVecY.setInputValidator(DECIMAL_OR_NEGATIVE);
        impulseVecY.configureDigitalBorderInput(() -> setText(impulseVecY, GuiFluxInteraction.IMPULSE_VEC_Y));
        impulseVecZ = addRenderableWidget(new GuiTextField(this, 25 + 90, 17 + 27, 44, 8));
        impulseVecZ.setMaxLength(7);
        impulseVecZ.setInputValidator(DECIMAL_OR_NEGATIVE);
        impulseVecZ.configureDigitalBorderInput(() -> setText(impulseVecZ, GuiFluxInteraction.IMPULSE_VEC_Z));

        addRenderableWidget(new GuiDigitalSwitch(this, 25 + 90, 17 + 36, SWITCH, tile::isGlobal, (element, mouseX, mouseY) ->
                PacketUtils.sendToServer(new PacketFluxGuiInteract(GuiFluxInteraction.IS_GLOBAL_BUTTON, ((GuiFieldDriveScreen) element.gui()).tile.getBlockPos(), new CompoundTag())), GuiDigitalSwitch.SwitchType.LEFT_ICON))
                .setTooltip(MekanismLang.AUTO_EJECT);
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);

        drawScreenText(guiGraphics, FNLang.GUI_FD_IMP_MULT.translate(tile.getImpulseMultiplier()), 2);
        drawScreenText(guiGraphics, FNLang.GUI_FD_IMP_X.translate(tile.getImpulseVectorX()), 11);
        drawScreenText(guiGraphics, FNLang.GUI_FD_IMP_Y.translate(tile.getImpulseVectorY()), 20);
        drawScreenText(guiGraphics, FNLang.GUI_FD_IMP_Z.translate(tile.getImpulseVectorZ()), 29);
        drawScreenText(guiGraphics, FNLang.GUI_FD_GLOBAL.translate(tile.isGlobal()), 38);
    }

    @Override
    protected void addGenericTabs() {
        if (tile.supportsUpgrades()) {
            upgradeWindowTab = addRenderableWidget(new GuiUpgradeWindowTab(this, tile, () -> upgradeWindowTab));
        }
        if (tile.supportsRedstone()) {
            addRenderableWidget(new GuiRedstoneControlTab(this, tile)
                   .warning(WarningTracker.WarningType.REDSTONE_PULSE_REQUIRED, tile::canActivate));
        }
        //Note: We check if the capability is present rather than calling hasSecurity so that we don't add the tab to the security desk
        if (tile.getLevel() != null && IBlockSecurityUtils.INSTANCE.securityCapability(tile.getLevel(), tile.getBlockPos(), tile) != null) {
            addSecurityTab();
        }
    }

    protected void drawScreenText(GuiGraphics guiGraphics, Component text, int y) {
        drawScreenText(guiGraphics, text, 0, y);
    }

    protected void drawScreenText(GuiGraphics guiGraphics, Component text, int x, int y) {
        //TODO: Do we want to make usages of this method eventually set the text to be rendered within the gui element for the screen?
        if (innerScreen != null) {//Validate it was properly set
            innerScreen.drawScaledScrollingString(guiGraphics, text, x, y, TextAlignment.LEFT, screenTextColor(), innerScreen.getXSize() - x, 5, false, 0.8F);
        }
    }

    private void setText(GuiTextField field, GuiFluxInteraction interaction) {
        if (!field.getText().isEmpty()) {
            try {
                CompoundTag tag = new CompoundTag();
                tag.putDouble("value", Double.parseDouble(field.getText()));
                PacketUtils.sendToServer(new PacketFluxGuiInteract(interaction, tile.getBlockPos(), tag));
            } catch (NumberFormatException ignored) {//Might not be valid if multiple negative signs
            }
            field.setText("");
        }
    }
}