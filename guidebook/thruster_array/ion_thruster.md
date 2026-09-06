---
navigation:
  title: "Ion Thruster"
  icon: "fluxnautics:ion_thruster"
  position: 2
  parent: fluxnautics:thruster_array.md
item_ids:
  - fluxnautics:ion_thruster
---

# Fancy shapes?

## All the shapes

The Ion Thruster is the main surface through which the thrust is produced, it can be placed in any direction, and will connection visually to other Ion Thrusters.

Starting with 1.0.0-beta.1 the model of the thruster is even more dynamic, the entire front face changes based on its neighbors, resulting in a hopefully more immersive experience.

If an Ion Thruster is directly obstructed by another Ion Thruster it will still generate thrust as usual, it will however not show any plume and will have a visually distinct model.


<GameScene zoom={4} interactive={true}>
  <ImportStructure src="bigger.snbt" />
</GameScene>

## Placement assist

Building a big array used to mean aiming at a face, eyeballing the orientation, placing, sighing, breaking it, and placing again. Now when you hold <ItemImage id="fluxnautics:ion_thruster" /> and look at an existing thruster, a translucent ghost shows exactly where the next one will land (on the sides of the hovered thruster, or just behind it), complete with an outline and a tint matching the group the new thruster is predicted to join.

The placement rules themselves live in the radial menu on the item, hold the mode key shown on the item's tooltip to open it. If you'd rather not open a menu at all, holding the same key and scrolling cycles the facing mode directly.

### Facing

How the placed thruster orients itself:

- **Auto: match neighbor** (default) - copies the facing of the thruster you are building off of, walls of thrusters finally line up without ritual sacrifice.
- **Face player** / **Face away** - points the thruster at you, or not at you.
- **Face clicked side** - points out of whichever face you clicked, the classic behavior.

### Snapping

- **Free placement** (default) - anywhere the ghost says it fits.
- **Extend arrays only** - placement is restricted to extending existing thruster formations, for people who know they will misclick eventually.

### Groups

By default the ghost inherits the group from any identical-facing thruster neighbor, so extending a wing of Group 2 thrusters keeps producing Group 2 thrusters without you touching anything.

If you want to force a specific group instead, you first need to bind yourself to an array: left-click a formed Thruster Array Controller while holding thrusters. Once bound, the radial's Group page lists that array's actual groups to pick from, and new thrusters land in the forced group regardless of their neighbors. The binding is per player, remembered server side, and can be cleared with the Unbind option at the root of the radial.

The tint you see on the ghost and its outline is the group the thruster is predicted to be placed in..