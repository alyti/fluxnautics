---
navigation:
  title: "Glossary"
  position: 99
---

# Glossary

## Vessel

Usually the main ship, base, space station you are building that is not attached to the world.

A way for me to refer to a Sable physics engine's sublevel without directly saying Sublevel.

## Physics Object

Sable Sublevel, usually attached to a Vessel in some way.

## Connected

Any [Physics Object](/glossary.md#physics-object)s that are connected to the main [Vessel](/glossary.md#vessel):
    - a rope
    - a spring
    - a swivel bearing
    - any sort of joint
    - anything that is reported by `sable$getConnectionDependencies` on actors)
