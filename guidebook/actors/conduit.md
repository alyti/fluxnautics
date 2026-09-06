---
navigation:
  title: "Conduits"
  icon: "fluxnautics:conduit_core"
  position: 1
  parent: fluxnautics:actors.md
item_ids:
  - fluxnautics:conduit_core
  - fluxnautics:conduit_connector
  - fluxnautics:conduit_port
---

# Can I reach it?

## Conduits!

<ItemGrid>
  <ItemIcon id="fluxnautics:conduit_port" />
  <ItemIcon id="fluxnautics:conduit_core" />
  <ItemIcon id="fluxnautics:conduit_connector" />
</ItemGrid>

A very new addition to the family of Fluxnautics, Conduits are essentially a longer energy-based way to connect 2 points.
By themselves connectors don't do much aside from being able to extend and retract the conduit between the connectors.
They are the same as regular ropes from another mod, but they have a unique feature provided by companion block Conduit Port.

## Connecting

A connection can be established by using a pair (2) of Conduit Cores on Conduit Connector blocks, as long as the Connectors are not too far apart (by default 64 blocks), a new Conduit will be formed.

## Ports

These blocks have a special placement rules:
- you must have *at least* 2 of them on you
- they must be placed on a side of a Conduit Connector
- the Conduit Connector must have a Conduit connected
- same side on the other end of the Conduit must not be occupied

Assuming all 3 conditions are fulfilled both ends of the conduit will have a port appear.
These ports are essentially Capability Proxies, while they don't have the infinite range of Mekanism's QP, being a capability proxy means there are no limits to throughput, or what can be proxied.
Any blocks with exposed capabilities, like cables, machines, chests, will have their capabilities proxied to the other end's Port block, you can easily identify which face of the Port block has the new capability available by both its color (they are color matched on both ends) and the fact that any block entity will cause the face to be lit up on the other end.

## WIP

This system is still early access. It might change, the rendering of the conduit will likely be more customizable and there's probably issues that I've yet to uncover.



// TODO: Look into adding render of conduit to this somehow?
<GameScene zoom={6} interactive={true}>
  <ImportStructure src="conduit_ports.snbt" />
</GameScene>
