---
navigation:
  title: "Controllers"
  icon: "fluxnautics:thruster_array_controller"
  position: 1
  parent: fluxnautics:thruster_array.md
item_ids:
  - fluxnautics:thruster_array_controller
  - fluxnautics:thruster_array_subcontroller

---

# One array at a time

## A required reading

To build an array you **must** have a Thruster Array Controller, there's no way around that.

It doesn't matter where you place it as long as its touching the rest of the thruster array.

## Groups

When you first interact with a controller you will find yourself staring at a GUI of the entire array.

Here you can create and manage something called "Groups", a group is basically just a configuration for a collection of thrusters.

Each group can have a name, a plume color, whatever it's rainbow or not, as well as some other settings, but most importantly the thrust value.

You can't assign thrusters to groups from this GUI, however you *can* do so by: 
1) using a <ItemImage id="mekanism:configurator" /> (in any Configurate mode) on a <ItemImage id="fluxnautics:ion_thruster" />, this will cycle that thruster's group assignment, to go back you can hold shift
2) using ComputerCraft api to assign thrusters to groups
   - this method is best suited for large [Vessel](/glossary.md#vessel)s as you can dynamically assign thrusters based on the thruster's block state (facing, position)

One of the main use cases for groups is directions, a single thruster array can have as many thrusters as your heart deems necesarry, but what if you wanted to also pivot or go up and down? Do you have to make a whole other array for that?
Well not necesarrily, you could build out thrusters in different directions and simply assign them to different groups.

<GameScene zoom={4} interactive={true}>
  <ImportStructure src="directions.snbt" />
</GameScene>

## Merged Thrust

Starting with 1.0.0-beta.4 every group gets a "Merged Thrust" toggle in the controller GUI, and it is exactly what it says on the tin.

Normally every thruster is its own little physics actor, each one doing its own driver math and pushing its own force into the [Vessel](/glossary.md#vessel) every physics tick. That is fine for a shuttle, but a few hundred thrusters means a few hundred actors all dragging the multiblock through the same lookups every substep, and it does add up.

With Merged Thrust enabled the group's thrusters go quiet, and the main controller (the same block that opens this GUI, so only the master one) takes over the whole group in a single pass. It still works out each thruster's contribution individually, position and facing included and alignment cores still do their thing, but the sums are done once every 10 ***game*** ticks and cached, and what lands on the physics engine is one merged force for the whole group - in the force overlay too, one entry instead of one per thruster.

The flight behavior might drift slightly depending on the driver mode, specifically Vector(AUTO) and Hover modes both will lag behind what the non-merged groups would yield as the vector math is done once every 10 game ticks, thus I would advise to keep this in mind when choosing to use this flag.

## Thrust Value (and how to control the array)

One of the easiest ways to tell the array to go somewhere is to apply a redstone signal to the main controller block.
But this has inherent limitations, the controller block will only ever control Group 0, the Default group that all thrusters listen to by... default.

Redstone signals have another limitation too, they are not precise, you only get 0-15, which is not a lot to work with considering boosters.

On the other end of the easiest to hardest scale we have ComputerCraft, the Controller block, and ONLY the Controller block, exposes a CC peripheral `thrusterArrayController`.

CC api offers bulk operations and indepth digital control over the thrust value.

But if redstone is what you wish to use, how would you control other groups if the main controller only controls Group 0? I am glad you asked!

## What's a "Subcontroller" anyway?

Subcontrollers, while mostly existing as an intermediate crafting ingredient, are also a functional block of the array.

Similar to Thrusters themselves, each side of the subcontroller can be tuned using <ItemImage id="mekanism:configurator" /> to control a specific Group.

Once a side has been tuned it can receive a redstone signal, this signal much like in the case of the controller, will drive a group's thrust value.

But unlike the controller, it will drive the group it was tuned to instead of the default group.

// TODO: Driver modes

<GameScene zoom={4} interactive={true}>
  <ImportStructure src="subcontroller.snbt" />
</GameScene>

## Why is my [Vessel](/glossary.md#vessel) not going to space?

Sable, the physics engine underlying everything, has a concept of Air Pressure, it affects thrust of most methods of movement, and it scales with height of the object.

The Ion propulsion you are using utilizes the surrounding Air Pressure to generate it's thrust, however when you reach a certain threshold there won't be enough pressure to keep up the thrust.

To mitigate this, you have the ability to supply the thruster array with your own source of chemicals, currently this means feeding Hydrogen in gas form into the thruster array.

Much like energy, hydrogen can be piped into any part of the thruster array.

<GameScene zoom={4} interactive={true}>
  <ImportStructure src="hydrogen.snbt" />
  <BlockAnnotationTemplate id="mekanism:creative_chemical_tank">
    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
      Container with Hydrogen gas
    </DiamondAnnotation>
  </BlockAnnotationTemplate>
</GameScene>