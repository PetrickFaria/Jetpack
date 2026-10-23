# Implementation status

## Current implementation

- Minecraft 1.19.2, Forge 43.5.0, Java 17
- Six jetpack tiers, recipes, Forge Energy storage, ten-second full charging, and 6000 FE capacity at one FE per active powered-flight tick (300 seconds at 20 TPS)
- Separate persistent equipment storage and the inventory slot left of the chest slot
- Powered flight reuses Minecraft's native `mayfly`/`flying` abilities at vanilla's 0.05 flying speed; it leaves sprint input untouched, so native sprint flight behavior remains available. Terrain limiting only constrains vertical motion at the ceiling. Powered Shift descent is left to vanilla flight input; the separate slow descent assist remains for flight after the motor is switched off.
- Powered flight ceiling follows the highest valid collision surface under the player's horizontal center; slabs, stairs and waterlogged solids count, fluid-only blocks and leaves do not
- The terrain scan is limited to 128 blocks and samples at most once every three game ticks; if no new support is found, the last validated surface remains the safety reference
- A newly lower terrain ceiling applies a gradual descent cap without moving the player with `setPos`
- Shift descent works with powered flight or with the main flight toggle off; descent with the main toggle off consumes 1 FE per tick, requires an equipped jetpack and remaining energy, and does not enable Creative flight abilities
- The six 3D models are attached to the torso; their geometry is shifted down eight model pixels to place the tanks and nozzles on the back
- Redstone and smoke particles use the two modeled nozzle outlet positions, follow player yaw, and emit only while powered flight is active
- Refueling accepts only `minecraft:coal` and `minecraft:charcoal`; every other furnace fuel is rejected. Each coal supplies 240 FE (4%), and 25 coal supply 6000 FE. The existing furnace burn energy is processed over eight game ticks per coal, so an empty pack charges in 200 game ticks (10 seconds at 20 TPS). Unused burn energy and fractional ER stay stored on the item when it reaches full charge, and resume after energy is consumed.
- Shift-click in the vanilla player inventory transfers a jetpack only when the exclusive slot is empty; right-clicking an inventory jetpack opens its own fuel menu
- Right-clicking a jetpack held in the hotbar continues to open its fuel menu
- The inventory accessory slot swap moves the item between cursor and storage without copying it

## Automated validation

- Unit tests cover the six tier ceilings, dynamic ceiling arithmetic, vertical-only ceiling constraints, powered descent preserving native velocity, coal/charcoal allowlisting, 4% per coal, 25-coal full charge in 200 game ticks, 6000 active flight ticks per full charge, charging arithmetic, and vanilla inventory-slot hit mapping
- GitHub Actions compiles and tests the mod, verifies `pack.mcmeta`, all six JSON models and transparent PNGs in the final JAR, and uploads a single distributable

## Manual Minecraft validation still required

- Visual check of back-model height/centering and both nozzle particle origins in third person
- Manual side-by-side movement comparison against Creative flight (forward/back/strafe/diagonal, vertical movement, sprint flight, acceleration and stopping) and flight tests over flat ground, slopes, cliffs, slabs, platforms, leaves, water, and cave overhangs for all six height tiers
- Client/server feel and multiplayer rendering/synchronization checks
- Powered and unpowered Shift descent, landing, and energy-depletion checks
- Shift-click and right-click refueling from the player inventory and hotbar
- Right-click from other mods' container screens is intentionally not intercepted; those layouts require their own safe integration
- Dedicated-server startup and in-game launch have not been run in this validation stage
