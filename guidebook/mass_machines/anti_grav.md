---
navigation:
  title: "Anti Grav 9000"
  icon: "fluxnautics:anti_grav"
  position: 1
  parent: fluxnautics:mass_machines.md
item_ids:
  - fluxnautics:anti_grav
---

# No more gravity! Or so we were promised...

## Known Issues

There is one big limitation to how cancelling gravity works right now in Sable, and that is there is only ***one*** way to do so, and that is to apply the reverse value of gravity that was applied to the sublevel, but there's a catch:

There is no way to know the exact amount of gravity being applied down to the smallest value as that information is within the rust side of things.

There's also no way to cancel gravity being applied to any specific sublevel either.

That leaves us with the burden of guessing the right amount of thrust to apply.

Currently, as long as there are no other active physics sources (e.g. Stabilizer, Thrusters, most other machines here) on the sublevel it should remain stable, but if you want to ensure it never moves you really should consider using a Spatial Anchor.

// TODO: Scenes