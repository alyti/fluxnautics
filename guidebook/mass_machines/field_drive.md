---
navigation:
  title: "Field Drive"
  icon: "fluxnautics:field_drive"
  position: 5
  parent: fluxnautics:mass_machines.md
item_ids:
  - fluxnautics:field_drive
---

# All the speed, in one go!

This is the more end-game alternative to building large thruster arrays, all in one machine.

It works like so, you:
1) specify a movement vector
2) specify whatever the vector is global (dimension's orientation) or local (ship's orientation) grid
3) specify the multiplier (by default for sanity its limited to 1000, but can be changed in the config)
4) provide Antimatter chemical - based off of the vector and multiplier specified
5) provide energy - based off of [Vessel](/glossary.md#vessel)'s mass, vector and multiplier
6) provide redstone pulse signal

After a short amount of charging, which can be reduced using Mekanism's upgrades the [Vessel](/glossary.md#vessel) will receive a momentary impulse from its center of mass into the vector specified.

For example if one was to use this machine in global grid mode and specify X0,Y1,Z0 and a multiplier of 1000, the ship would consume 1000mb antimatter, some amount of power and produce an impulse large enough to send the [Vessel](/glossary.md#vessel) UP about 1000 blocks, but the actual distance traveled will be hard to predict, this is not an exact transportation method, think of it as making a large jump with your eyes closed.

// TODO: Rendering field drive seems to break GuideME so no scenes for it for now.