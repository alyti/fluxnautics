package dev.alyti.fluxnautics.content.blocks.forcefield;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.alyti.fluxnautics.FluxnauticsConfig;
import dev.alyti.fluxnautics.util.RegistryFilterList;
import dev.alyti.fluxnautics.util.SFColors;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;
import java.util.Optional;

import static dev.alyti.fluxnautics.util.SFColors.COLOR_CODEC;

public class ForcefieldBubbleEmitterConfig {
    public static final ForcefieldBubbleEmitterConfig INITIAL_SERVER = makeDefault(null);

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ForcefieldBubbleEmitterConfig that = (ForcefieldBubbleEmitterConfig) o;
        return allowPassiveMobs == that.allowPassiveMobs && allowHostileMobs == that.allowHostileMobs && allowProjectiles == that.allowProjectiles && allowForeignPlayers == that.allowForeignPlayers && allowEverythingElse == that.allowEverythingElse && allowForeignSublevels == that.allowForeignSublevels && Double.compare(fieldExpansion, that.fieldExpansion) == 0 && passiveColor == that.passiveColor && passiveColorRainbow == that.passiveColorRainbow && activeColor == that.activeColor && activeColorRainbow == that.activeColorRainbow && Objects.equals(getAllowEntries(), that.getAllowEntries()) && Objects.equals(getDenyEntries(), that.getDenyEntries());
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowPassiveMobs, allowHostileMobs, allowProjectiles, allowForeignPlayers, allowEverythingElse, getAllowEntries(), getDenyEntries(), allowForeignSublevels, fieldExpansion, passiveColor, passiveColorRainbow, activeColor, activeColorRainbow);
    }

    public static final Codec<ForcefieldBubbleEmitterConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("allow_passive", true).forGetter(ForcefieldBubbleEmitterConfig::allowPassiveMobs),
            Codec.BOOL.optionalFieldOf("allow_hostile", false).forGetter(ForcefieldBubbleEmitterConfig::allowHostileMobs),
            Codec.BOOL.optionalFieldOf("allow_projectiles", false).forGetter(ForcefieldBubbleEmitterConfig::allowProjectiles),
            Codec.BOOL.optionalFieldOf("allow_foreign_players", false).forGetter(ForcefieldBubbleEmitterConfig::allowForeignPlayers),
            Codec.BOOL.optionalFieldOf("allow_everything_else", true).forGetter(ForcefieldBubbleEmitterConfig::allowEverythingElse),
            RegistryFilterList.codec(BuiltInRegistries.ENTITY_TYPE).optionalFieldOf("allow", RegistryFilterList.empty()).forGetter(ForcefieldBubbleEmitterConfig::getAllowEntries),
            RegistryFilterList.codec(BuiltInRegistries.ENTITY_TYPE).optionalFieldOf("deny", RegistryFilterList.empty()).forGetter(ForcefieldBubbleEmitterConfig::getDenyEntries),
            Codec.BOOL.optionalFieldOf("allow_foreign_sublevels", false).forGetter(ForcefieldBubbleEmitterConfig::allowForeignSublevels),
            Codec.DOUBLE.optionalFieldOf("field_expansion", 5.0).forGetter(ForcefieldBubbleEmitterConfig::fieldExpansion),
            COLOR_CODEC.optionalFieldOf("passive_color", SFColors.DARK_GREY_ARGB).forGetter(ForcefieldBubbleEmitterConfig::passiveColor),
            Codec.BOOL.optionalFieldOf("passive_color_rainbow", false).forGetter(ForcefieldBubbleEmitterConfig::passiveColorRainbow),
            COLOR_CODEC.optionalFieldOf("active_color", SFColors.MEDIA_OURPLE_ARGB).forGetter(ForcefieldBubbleEmitterConfig::activeColor),
            Codec.BOOL.optionalFieldOf("active_color_rainbow", true).forGetter(ForcefieldBubbleEmitterConfig::activeColorRainbow)
    ).apply(instance, ForcefieldBubbleEmitterConfig::new));

    private static final StreamCodec<ByteBuf, RegistryFilterList<EntityType<?>>> FILTER_STREAM_CODEC =
            RegistryFilterList.streamCodec(Registries.ENTITY_TYPE);

    public static final StreamCodec<ByteBuf, ForcefieldBubbleEmitterConfig> STREAM_CODEC = StreamCodec.of(
            (buf, config) -> {
                ByteBufCodecs.BOOL.encode(buf, config.allowPassiveMobs());
                ByteBufCodecs.BOOL.encode(buf, config.allowHostileMobs());
                ByteBufCodecs.BOOL.encode(buf, config.allowProjectiles());
                ByteBufCodecs.BOOL.encode(buf, config.allowForeignPlayers());
                ByteBufCodecs.BOOL.encode(buf, config.allowEverythingElse());

                FILTER_STREAM_CODEC.encode(buf, config.getAllowEntries());
                FILTER_STREAM_CODEC.encode(buf, config.getDenyEntries());

                ByteBufCodecs.BOOL.encode(buf, config.allowForeignSublevels());
                ByteBufCodecs.DOUBLE.encode(buf, config.fieldExpansion());
                ByteBufCodecs.INT.encode(buf, config.passiveColor());
                ByteBufCodecs.BOOL.encode(buf, config.passiveColorRainbow());
                ByteBufCodecs.INT.encode(buf, config.activeColor());
                ByteBufCodecs.BOOL.encode(buf, config.activeColorRainbow());
            },
            (buf) -> {
                boolean allowPassiveMobs = ByteBufCodecs.BOOL.decode(buf);
                boolean allowHostileMobs = ByteBufCodecs.BOOL.decode(buf);
                boolean allowProjectiles = ByteBufCodecs.BOOL.decode(buf);
                boolean allowForeignPlayers = ByteBufCodecs.BOOL.decode(buf);
                boolean allowEverythingElse = ByteBufCodecs.BOOL.decode(buf);

                RegistryFilterList<EntityType<?>> allowEntries = FILTER_STREAM_CODEC.decode(buf);
                RegistryFilterList<EntityType<?>> denyEntries = FILTER_STREAM_CODEC.decode(buf);

                boolean allowForeignSublevels = ByteBufCodecs.BOOL.decode(buf);
                double fieldExpansion = ByteBufCodecs.DOUBLE.decode(buf);
                int passiveColor = ByteBufCodecs.INT.decode(buf);
                boolean passiveColorRainbow = ByteBufCodecs.BOOL.decode(buf);
                int activeColor = ByteBufCodecs.INT.decode(buf);
                boolean activeColorRainbow = ByteBufCodecs.BOOL.decode(buf);

                return new ForcefieldBubbleEmitterConfig(allowPassiveMobs, allowHostileMobs, allowProjectiles, allowForeignPlayers, allowEverythingElse, allowEntries, denyEntries, allowForeignSublevels, fieldExpansion, passiveColor, passiveColorRainbow, activeColor, activeColorRainbow);
            }
    );

    private boolean allowPassiveMobs;
    private boolean allowHostileMobs;
    private boolean allowProjectiles;
    private boolean allowForeignPlayers;
    private boolean allowEverythingElse;

    private RegistryFilterList<EntityType<?>> allowEntries;
    private RegistryFilterList<EntityType<?>> denyEntries;

    private boolean allowForeignSublevels;

    private double fieldExpansion;
    private int passiveColor;
    private boolean passiveColorRainbow;
    private int activeColor;
    private boolean activeColorRainbow;

    public ForcefieldBubbleEmitterConfig(boolean allowPassiveMobs, boolean allowHostileMobs, boolean allowProjectiles, boolean allowForeignPlayers, boolean allowEverythingElse, RegistryFilterList<EntityType<?>> allowEntries, RegistryFilterList<EntityType<?>> denyEntries, boolean allowForeignSublevels, double fieldExpansion, int passiveColor, boolean passiveColorRainbow, int activeColor, boolean activeColorRainbow) {
        this.allowPassiveMobs = allowPassiveMobs;
        this.allowHostileMobs = allowHostileMobs;
        this.allowProjectiles = allowProjectiles;
        this.allowForeignPlayers = allowForeignPlayers;
        this.allowEverythingElse = allowEverythingElse;

        this.allowEntries = allowEntries;
        this.denyEntries = denyEntries;

        this.allowForeignSublevels = allowForeignSublevels;

        this.fieldExpansion = fieldExpansion;
        this.passiveColor = passiveColor;
        this.passiveColorRainbow = passiveColorRainbow;
        this.activeColor = activeColor;
        this.activeColorRainbow = activeColorRainbow;
    }

    public static ForcefieldBubbleEmitterConfig makeDefault(final ForcefieldEmitterBlockEntity tile) {
        return new ForcefieldBubbleEmitterConfig(true, false, false, false, false, RegistryFilterList.empty(), RegistryFilterList.empty(), false, 5.0, SFColors.DARK_GREY_ARGB, false, SFColors.MEDIA_OURPLE_ARGB, true);
    }

    public boolean allowPassiveMobs() {
        return allowPassiveMobs;
    }

    public void setAllowPassiveMobs(boolean allowPassiveMobs) {
        this.allowPassiveMobs = allowPassiveMobs;
    }

    public boolean allowHostileMobs() {
        return allowHostileMobs;
    }

    public void setAllowHostileMobs(boolean allowHostileMobs) {
        this.allowHostileMobs = allowHostileMobs;
    }

    public double fieldExpansion() {
        return fieldExpansion;
    }

    public void setFieldExpansion(double fieldExpansion) {
        this.fieldExpansion = Math.clamp(fieldExpansion, 0.0, FluxnauticsConfig.common.forcefieldMaxFieldExpansion.getAsDouble());
    }

    public int activeColor() {
        return activeColor;
    }

    public void setActiveColor(int activeColor) {
        this.activeColor = activeColor;
    }

    public boolean activeColorRainbow() {
        return activeColorRainbow;
    }

    public void setActiveColorRainbow(boolean activeColorRainbow) {
        this.activeColorRainbow = activeColorRainbow;
    }

    public boolean allowForeignSublevels() {
        return allowForeignSublevels;
    }

    public void setAllowForeignSublevels(boolean allowForeignSublevels) {
        this.allowForeignSublevels = allowForeignSublevels;
    }

    public int passiveColor() {
        return passiveColor;
    }

    public void setPassiveColor(int passiveColor) {
        this.passiveColor = passiveColor;
    }

    public boolean passiveColorRainbow() {
        return passiveColorRainbow;
    }

    public void setPassiveColorRainbow(boolean passiveColorRainbow) {
        this.passiveColorRainbow = passiveColorRainbow;
    }

    public boolean allowProjectiles() {
        return allowProjectiles;
    }

    public void setAllowProjectiles(boolean allowProjectiles) {
        this.allowProjectiles = allowProjectiles;
    }

    public RegistryFilterList<EntityType<?>> getAllowEntries() {
        return allowEntries;
    }

    public void setAllowEntries(RegistryFilterList<EntityType<?>> allowEntries) {
        this.allowEntries = allowEntries;
    }

    public RegistryFilterList<EntityType<?>> getDenyEntries() {
        return denyEntries;
    }

    public void setDenyEntries(RegistryFilterList<EntityType<?>> denyEntries) {
        this.denyEntries = denyEntries;
    }

    public boolean allowForeignPlayers() {
        return allowForeignPlayers;
    }

    public void setAllowForeignPlayers(boolean allowForeignPlayers) {
        this.allowForeignPlayers = allowForeignPlayers;
    }

    public boolean allowEverythingElse() {
        return allowEverythingElse;
    }

    public void setAllowEverythingElse(boolean allowEverythingElse) {
        this.allowEverythingElse = allowEverythingElse;
    }

    public record PartialUpdate(
            Optional<Boolean> allowPassiveMobs,
            Optional<Boolean> allowHostileMobs,
            Optional<Boolean> allowProjectiles,
            Optional<Boolean> allowForeignPlayers,
            Optional<Boolean> allowEverythingElse,
            Optional<RegistryFilterList<EntityType<?>>> allowEntries,
            Optional<RegistryFilterList<EntityType<?>>> denyEntries,
            Optional<Boolean> allowForeignSublevels,
            Optional<Double> fieldExpansion,
            Optional<Integer> passiveColor,
            Optional<Boolean> passiveColorRainbow,
            Optional<Integer> activeColor,
            Optional<Boolean> activeColorRainbow
    ) {
        public static final Codec<PartialUpdate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("allow_passive").forGetter(PartialUpdate::allowPassiveMobs),
                Codec.BOOL.optionalFieldOf("allow_hostile").forGetter(PartialUpdate::allowHostileMobs),
                Codec.BOOL.optionalFieldOf("allow_projectiles").forGetter(PartialUpdate::allowProjectiles),
                Codec.BOOL.optionalFieldOf("allow_foreign_players").forGetter(PartialUpdate::allowForeignPlayers),
                Codec.BOOL.optionalFieldOf("allow_everything_else").forGetter(PartialUpdate::allowEverythingElse),
                RegistryFilterList.codec(BuiltInRegistries.ENTITY_TYPE).optionalFieldOf("allow").forGetter(PartialUpdate::allowEntries),
                RegistryFilterList.codec(BuiltInRegistries.ENTITY_TYPE).optionalFieldOf("deny").forGetter(PartialUpdate::denyEntries),
                Codec.BOOL.optionalFieldOf("allow_foreign_sublevels").forGetter(PartialUpdate::allowForeignSublevels),
                Codec.DOUBLE.optionalFieldOf("field_expansion").forGetter(PartialUpdate::fieldExpansion),
                COLOR_CODEC.optionalFieldOf("passive_color").forGetter(PartialUpdate::passiveColor),
                Codec.BOOL.optionalFieldOf("passive_color_rainbow").forGetter(PartialUpdate::passiveColorRainbow),
                COLOR_CODEC.optionalFieldOf("active_color").forGetter(PartialUpdate::activeColor),
                Codec.BOOL.optionalFieldOf("active_color_rainbow").forGetter(PartialUpdate::activeColorRainbow)
        ).apply(instance, PartialUpdate::new));

        public void applyTo(ForcefieldBubbleEmitterConfig target) {
            allowPassiveMobs.ifPresent(target::setAllowPassiveMobs);
            allowHostileMobs.ifPresent(target::setAllowHostileMobs);
            allowProjectiles.ifPresent(target::setAllowProjectiles);
            allowForeignPlayers.ifPresent(target::setAllowForeignPlayers);
            allowEverythingElse.ifPresent(target::setAllowEverythingElse);
            allowEntries.ifPresent(target::setAllowEntries);
            denyEntries.ifPresent(target::setDenyEntries);
            allowForeignSublevels.ifPresent(target::setAllowForeignSublevels);
            fieldExpansion.ifPresent(target::setFieldExpansion);
            passiveColor.ifPresent(target::setPassiveColor);
            passiveColorRainbow.ifPresent(target::setPassiveColorRainbow);
            activeColor.ifPresent(target::setActiveColor);
            activeColorRainbow.ifPresent(target::setActiveColorRainbow);
        }

    }
}