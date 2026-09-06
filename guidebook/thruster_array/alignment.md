---
navigation:
  title: "Alignment Core"
  icon: "fluxnautics:thruster_array_alignment_x"
  position: 9
  parent: fluxnautics:thruster_array.md
item_ids:
  - fluxnautics:thruster_array_alignment_x
  - fluxnautics:thruster_array_alignment_y
  - fluxnautics:thruster_array_alignment_z
---

# Is your [Vessel](/glossary.md#vessel) too unwieldy?

You might choose to design your ship in a more asymmetric way or otherwise unusual layout that produces uneven or even unmanageable thrust.

But worry not! For there is a very simple solution to this problem, introducing Alignment Cores!

## Alignment Cores

<ItemGrid>
  <ItemIcon id="fluxnautics:thruster_array_alignment_x" />
  <ItemIcon id="fluxnautics:thruster_array_alignment_y" />
  <ItemIcon id="fluxnautics:thruster_array_alignment_z" />
</ItemGrid>

A very late-game (price wise) solution to moving thrust origin away from the thrusters themselves and to the Center of Mass of the [Vessel](/glossary.md#vessel).

Each core shifts all thruster's point of propulsion closer to [Vessel](/glossary.md#vessel)'s Center of Mass by 1.0 in an axis of its type.

For example, below structure has an Ion Thruster several blocks above the rest of the structure, but with Y Axis alignments present the origin's Y coordinate is still located at the center of mass.
There are no X or Z cores present in this array however, as a result the X and Z axis of the thrust origin is still at the original values.

<GameScene zoom={4} interactive={true}>
  <ImportStructure src="alignment.snbt" />
</GameScene>
