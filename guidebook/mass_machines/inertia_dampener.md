---
navigation:
  title: "Inertia Dampener"
  icon: "fluxnautics:inertia_dampener"
  position: 7
  parent: fluxnautics:mass_machines.md
item_ids:
  - fluxnautics:inertia_dampener
---

# Brakes? On a ship?

Between "full send" and a [Spatial Anchor](spatial_anchor.md) there is a whole lot of middle ground, and the <ItemImage id="fluxnautics:inertia_dampener" /> lives there. Where the Anchor locks the entire [Physics Object](/glossary.md#physics-object) in place and burns energy to cancel *everything*, the Dampener only eats the motion you tell it to, one axis at a time.

## How it works

Every physics step the Dampener reads the [Vessel](/glossary.md#vessel)'s velocity in the ship's own frame of reference, and for each axis you have enabled applies a counter-impulse against it. The interface gives you the three linear axes (X, Y and Z of your ship, not the world, so a dampener set to stop "X" stops you sliding sideways no matter which way the nose is pointing) and each axis has three modes:

- **OFF** - free motion, that axis is none of the Dampener's business.
- **DAMPEN** - proportional resistance, tuned by a strength slider from 0 to 1. At full strength the axis is bled down to zero every step, at half strength you get half of that, and so on. This is the "soft stop" setting, good for killing drift without feeling like the hand of god grabbed your ship.
- **BRAKE** - no slider, no mercy, the axis is driven to a stop as hard as the calibration allows. Useful for landings.

Fresh out of the box all three axes start in Dampen at full strength, so be ready for your test flight to be very short and very stationary.

## The fine print
    
Two things to know before you complain about either of them:

1) The correction applied each step is capped (the linear calibration value in the config), nothing produces infinite force here. A very fast, very heavy ship takes a few ticks to actually stop, which is arguably more physically honest anyway.
2) You pay for the kinetic energy actually removed, half Mass times Δv squared times the config's joules per kinetic unit. Idling with nothing to dampen costs nothing, and a ship that is already at rest costs nothing. You are billed for work, not for existing.

Rotation is deliberately not included, spinning is the [Stabilizer's](stabilizer.md) job and it is quite territorial about it.

## Redstone and CC

The sides accept per-side redstone control modes like the rest of the machines in this mod, and a digital override hands control to ComputerCraft, which can read and write each axis' mode and strength, along with the last measured velocity and how much kinetic energy the machine removed on the previous tick. If you want a landing computer that brakes specific axes based on approach speed, everything you need is exposed.

// TODO: Scenes
