---
navigation:
  title: "Stabilizer"
  icon: "fluxnautics:stabilizer"
  position: 3
  parent: fluxnautics:mass_machines.md
item_ids:
  - fluxnautics:stabilizer
---

# Stop the spinning!

Do you ever feel like your lunch is coming back up after a bumpy ride in your [Vessel](/glossary.md#vessel)? The Stabilizer is here to save the day (and your stomach).

When placed on a [Physics Object](/glossary.md#physics-object) and supplied with energy, the Stabilizer acts as a massive internal reaction wheel. It applies opposing torque to keep your [Vessel](/glossary.md#vessel) steady, fighting against unwanted rotation caused by collisions, uneven thruster placement, or just bad piloting.

## How does it work?

The stabilizer will actively fight to hold the [Vessel](/glossary.md#vessel) at the exact Pitch, Yaw, and Roll angles specified by the target sliders in its interface. This is perfect for keeping your ship level while cruising or maintaining a specific orientation for docking.

## Tuning the Gyro

Every ship handles differently. A tiny drone and a massive battleship have very different mass profiles, so the Stabilizer provides a suite of fine-tuning options in its interface:

- **Force Percent:** How much "muscle" the stabilizer uses to correct the ship's orientation. You can push this up to 200% for extremely heavy vessels, but be warned: higher force means drastically higher energy consumption!
- **Dampening Percent:** Controls how aggressively the stabilizer kills existing rotational momentum. High dampening makes the ship feel like it's flying through molasses (resisting all spin), while low dampening lets the ship drift and coast on its current rotation.
- **Accept Angle:** A deadzone measured in degrees. If the ship's angle is within this threshold of the target, the stabilizer will stop fighting and let it rest. Keep this at 0 for absolute precision, or raise it slightly to save power on larger ships where perfect precision isn't needed.
- **Axis Toggles (Pitch/Yaw/Roll):** You can independently enable or disable stabilization for each axis. Want to let your ship drift freely on its Yaw axis while keeping its Pitch and Roll perfectly level? Just turn Yaw off!
- **Bidirectional Torque:** By default, this is enabled, allowing the stabilizer to apply force in both directions equally. If disabled, the internal wheel is only allowed to spin one way per axis, meaning it will "wind up" over time and eventually lose the ability to correct in that direction until it unwinds. (Useful for advanced builders who want to build mechanical limits into their ships).

With a well-tuned Stabilizer, your [Vessel](/glossary.md#vessel) will fly straight and true, no matter what you bump into out there!
