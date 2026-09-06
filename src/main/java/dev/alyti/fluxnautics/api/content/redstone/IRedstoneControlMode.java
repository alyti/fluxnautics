package dev.alyti.fluxnautics.api.content.redstone;

import mekanism.api.text.EnumColor;
import net.minecraft.network.chat.Component;

/**
 * The redstone counterpart of Mekanism's {@code DataType}: enums implement this to define the
 * "modes" a machine's redstone side configuration can assign to each of its six relative sides.
 * Each mode carries a driver that turns the raw 0-15 redstone signal measured on a configured
 * side into machine state changes.
 *
 * <p>Semantics implemented by {@link TileComponentRedstoneControl}:
 * <ul>
 *   <li>The strongest signal across all sides configured with the same mode wins, and
 *       {@link #drive} runs once per server tick with that aggregated value (0-15 inclusive;
 *       a configured side drives even at signal 0).</li>
 *   <li>Sides set to the enum's FIRST constant (the inert default) are never sampled.</li>
 *   <li>When the aggregated signal transitions 0 -> nonzero, {@link #onRisingEdge} fires right
 *       before {@link #drive} for that tick, so pulse/toggle modes only need that override.</li>
 * </ul>
 *
 * @param <T> the tile type this mode knows how to drive
 */
public interface IRedstoneControlMode<T> {

    /** Color used for the side config buttons and configurator feedback. */
    EnumColor color();

    /** Full display name (configurator action bar messages, tooltips). */
    Component text();

    /** Compact label for the side config button face; defaults to {@link #text()}. */
    default Component shortText() {
        return text();
    }

    /**
     * Drives the machine from the aggregated 0-15 signal of every side configured with this mode.
     * Called every server tick while at least one side is set to this mode.
     */
    void drive(T tile, int signal);

    /** Fired on a 0 -> nonzero transition of the aggregated signal, right before {@link #drive}. */
    default void onRisingEdge(T tile, int signal) {
    }

    /**
     * Whether this mode drives the machine while the aggregated signal is 0. Defaults to false
     * ("latch"): a configured side with no signal leaves the config untouched, so browsing modes
     * in the GUI without any redstone present does not wipe the current values. Override to true
     * for modes that must actively pull the config back to their zero-signal state.
     */
    default boolean drivesAtZeroSignal() {
        return false;
    }
}