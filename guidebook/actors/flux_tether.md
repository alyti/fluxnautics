---
navigation:
  title: "Flux Tether"
  icon: "fluxnautics:flux_tether"
  position: 2
  parent: fluxnautics:actors.md
item_ids:
  - fluxnautics:flux_tether
  - fluxnautics:flux_tether_link
---

# A leash for loose ends!

Sometimes two things need to stay near each other, but you don't want to actually weld them together. A docking clamp. A sensor boom. A very expensive plumb bob. That is what the <ItemImage id="fluxnautics:flux_tether" /> is for, it connects the machine itself to a floating <ItemImage id="fluxnautics:flux_tether_link" /> with a real physics constraint, and lets you decide exactly how much the two ends are allowed to move.

## Assembly

Place the Tether like any machine, a Link plate will appear on the face it is pointing at. Then crouch and right-click the Tether with an **empty hand** to assemble, the Link plate (and whatever is attached to it) gets detached into its own [Physics Object](/glossary.md#physics-object) held to the Tether by an invisible servo joint. Assembled this way the Link counts as [Connected](/glossary.md#connected) to the body the Tether sits on, so forcefields, cloaks and the rest of the gang will treat it as part of the family.

Assembling the same way again takes it apart and rebuilds the plate back in the world.

## Linear and Angular axes

The Tether has two independent axes to configure, Linear (the distance between the ends) and Angular (the angle around the Tether's facing axis). Each one has three modes:

- **FREE** - no servo at all, the Link drifts and swings on the joint. Soft limits still apply, the Link can't be pushed further out than the maximum extension or folded closer than the minimum.
- **STATIC** - a rigid servo holds the configured value. A distance for the Linear axis, an angle in degrees for the Angular one.
- **SERVO** - continuous motion. Linear sweeps the Link out and back in a loop you shape with the rate (blocks per second) and loop length (blocks), Angular spins it around at a rate in degrees per second. Conveyor belt of one block, coming right up.

The **Force Boost** sliders (1.0 to 4.0) multiply how hard the servo fights to hold its target, with energy cost growing alongside. If whatever you tethered is heavier than your ambitions, this is the knob to turn.

## Energy

While any axis is engaged the Tether bills a baseline drain per tick, multiplied by your boost settings, plus work energy for every block of distance and bit of inertia it actually moves. That last part scales with the Mass of what you attached, holding a single Link plate is cheap, tethering it to a battleship that wants to keep drifting is not. With no axis engaged it just idles.

## The Beam

Once the extension is at least a block and a half or so, the Tether starts projecting a cutting beam along its facing axis. It behaves like a Mekanism laser, blocks caught in it are broken (drops included, TNT gets lit instead of broken) for the usual energy-per-hardness cost, and any living creature standing in the beam gets hurt for as long as it stays there. Speed upgrades reduce the beam's tick interval, Energy upgrades push the maximum extension from 64 blocks up by 32 each.

## Taking it apart

Disassembling while the Link is further out than the disassemble threshold (4 blocks by default) will first pull the Link back in, and only then teleport it home and rebuild the blocks. Anything sitting in the return path gets broken, the beam has already taught you this lesson by now.

## Redstone and CC

Like the other machines here, the sides of the Tether can each be assigned a redstone control role with a Configurator, driving targets, servo rates, force boosts, stepping modes on a rising edge, or forcing modes outright. Digital override flips all of it to ComputerCraft, which gets the full config object, partial updates included, so you can build a control room that gently reels things in and out on a schedule.

// TODO: Scenes
