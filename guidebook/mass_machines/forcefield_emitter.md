---
navigation:
  title: "Forcefield"
  icon: "fluxnautics:forcefield_emitter"
  position: 6
  parent: fluxnautics:mass_machines.md
item_ids:
  - fluxnautics:forcefield_emitter
  - fluxnautics:forcefield_harmonizer
---

# None shall pass!

Are you tired of pesky entities pelting your poor [Vessel](/glossary.md#vessel) to bits? Worry not, yet another wonderful solution is presented to you: Forcefield Emitters!

Now with ability to expand the force bubble and control what can be inside the bubble.

Note that this force field is different from ones you are used to, instead of only preventing entry at the outer border, this field protects the entire volume it envelopes.

What this means for you is adding new entities, projectiles or [Physics Object](/glossary.md#physics-object)s inside the field, depending on the settings of the field emitter, might result in the newly created object being ejected beyond the constraints of the [Vessel](/glossary.md#vessel).

Additionally, in the innermost core of the bubble, depending on the size of it any foreign entities will find themselves teleported outside instead of merely ejected, as a measure to solve some objects being stuck inside the [Vessel](/glossary.md#vessel).

## Connected objects

Even if the field emitter is configured to not allow foreign [Physics Object](/glossary.md#physics-object)s (as in, ones that are not the one its placed on), [Connected](/glossary.md#connected) objects are considered part of the [Vessel](/glossary.md#vessel) and as such are ignored.

## What if it's not connected?

There is a machine called Forcefield Harmonizer which can be installed on any other [Physics Object](/glossary.md#physics-object)s that you want to allow inside the bubble.

These machines once installed must be tuned to have the same Item Frequency as the forcefield itself, a Harmonizer can be attuned to multiple frequencies at the same time.

Note that this machine still requires power, but it's a fixed cost unlike all the other machines in this mod.

## Entities

This is the order of checks done on entities that interact with the field:

If Player, following conditions will let you pass:
  - Configured to allow foreign players
  - Player is in creative mode
  - Player owns the forcefield emitter
  - Player can access the forcefield emitter, using Mekanism security modes
    - Public means anyone is free to get through the field
    - Trusted means you need to use the Security Desk to add players to the list
    - Private means only you can get through the field

Entity's resource location (mod:id) is checked against
  - Entity tag `fluxnautics:forcefield_global_block`, matches get rejected
  - Entity tag `fluxnautics:forcefield_global_allow`, matches get allowed
  - Config's `deny` tag/entity list is checked, matches get rejected
  - Config's `allow` tag/entity list is checked, matches get allowed

If LivingEntity, the type's category is used to decide if its friendly or not (`getType().getCategory().isFriendly()`):
  - Friendly entities check allow_passive_mobs
  - Hostile entities check allow_hostile_mobs

If ItemEntity, you are allowed.

If Projectile or a FallingBlockEntity, you are checked against `allow_projectiles` (the latter due to primed TNT)

Everything else is checked against `allow_everything_else`.

## Overload

Starting with 1.0.0-beta.3 the Forcefield Bubble Emitter introduces a new balancing feature, an Overload buffer is the amount of damage the shield has sustained over a specified window of time plus permanent damage from direct damage sources.
The permanent damage doesn't recover over time like normal sources do, and instead will only reset when the shield is overloaded.

The Overload buffer size derived from the machine's energy buffer and is multiplied by a configurable threshold value, by default the buffer size is 100% of the energy buffer.
The rollingImpactWindow configuration value controls how many ticks you have to fill the buffer, any non-permanent damage will fully recover once its outside the set time window.

Once the shield is overloaded it will enter a cooldown similar to one triggered by low energy, but depending on configuration settings (and by default) the overload cooldown uses a different formula: 
> `cooldownTicks + ((sqrt(volume(field)) * log(overload_joules) / 10) * overloadCooldownMult)`

## Raycast Impacts

Mekanism Laser and similar sources of damage, ones that perform their damage via Level.clip checks, will now also be redirected to the edge of the field, applying a small amount of impact damage.

### Direct Damage Sources

Additionally, currently applicable only to Mekanism Lasers, the forcefield will receive the full amount of energy x the configured laser damage multiplier directly to the overload meter.

