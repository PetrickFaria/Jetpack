# Implementation status

## Current implementation

- Minecraft 1.19.2, Forge 43.5.0, Java 17
- Six jetpack tiers, recipes, Forge Energy storage, ten-second full charging, and 6000 FE capacity at one FE per active powered-flight tick (300 seconds at 20 TPS)
- Separate persistent equipment storage and the inventory slot left of the chest slot
- Powered flight reuses Minecraft's native `mayfly`/`flying` abilities at vanilla's 0.05 flying speed; it leaves sprint input untouched, so native sprint flight behavior remains available. Terrain limiting only constrains vertical motion at the ceiling. Powered Shift descent is left to vanilla flight input; the separate slow descent assist remains for flight after the motor is switched off.
- Powered flight ceiling follows the highest valid collision surface under the player's horizontal center; slabs, stairs and waterlogged solids count, fluid-only blocks and leaves do not
- The terrain scan is limited to 128 blocks and samples once per powered-flight tick; if no new support is found, the last validated surface remains the safety reference
- At the ceiling, the client suppresses only vanilla's upward input. A single normal flight-step overshoot is aligned to the boundary; the server validates and corrects larger overshoots. If terrain lowers the ceiling, upward input is blocked and a server-side gradual descent correction is used
- Double-space is detected once on the client and delivered as one toggle command; the server validates the equipped jetpack and available energy. Reactivating while falling zeros only the downward velocity and does not add upward impulse
- Powered Shift flight stops when the server reports actual ground contact; Shift alone does not stop flight
- Shift descent works with powered flight or with the main flight toggle off; descent with the main toggle off consumes 1 FE per tick, requires an equipped jetpack and remaining energy, and does not enable Creative flight abilities
- The six 3D models are attached to the torso; their geometry is shifted down eight model pixels to place the tanks and nozzles on the back
- Redstone and smoke particles use the two modeled nozzle outlet positions, follow player yaw, and emit only while powered flight is active
- Refueling accepts only `minecraft:coal` and `minecraft:charcoal`; every other furnace fuel is rejected. Each coal supplies 240 FE (4%), and 25 coal supply 6000 FE. The existing furnace burn energy is processed over eight game ticks per coal, so an empty pack charges in 200 game ticks (10 seconds at 20 TPS). Unused burn energy and fractional ER stay stored on the item when it reaches full charge, and resume after energy is consumed.
- Shift-click in the vanilla player inventory transfers a jetpack only when the exclusive slot is empty; right-clicking an inventory jetpack opens its own fuel menu
- Right-clicking a jetpack held in the hotbar continues to open its fuel menu
- The inventory accessory slot swap moves the item between cursor and storage without copying it

## Automated validation

- Unit tests cover the six tier ceilings, boundary input suppression, bounded overshoot correction, gradual lower-terrain correction, Shift-plus-ground landing detection, powered descent preserving native velocity, coal/charcoal allowlisting, 4% per coal, 25-coal full charge in 200 game ticks, 6000 active flight ticks per full charge, charging arithmetic, and vanilla inventory-slot hit mapping
- GitHub Actions compiles and tests the mod, verifies `pack.mcmeta`, all six JSON models and transparent PNGs in the final JAR, and uploads a single distributable

## Manual Minecraft validation still required

- No GameTest task is configured in this repository, and no Minecraft client/server was launched in this run. To reproduce the remaining checks: in Survival, equip a charged wooden jetpack over a known flat block, double-tap Space, hold Space for 10 seconds, and confirm the player's feet never rise above surface Y + 5 while horizontal movement remains available. Repeat at 10/15/20/25/50 blocks on each tier, then cross a slope and a lower ledge.
- With flight active in midair, double-tap Space to stop, wait until clearly falling, then double-tap Space again; verify flight resumes without upward impulse. Repeat with zero energy and confirm activation is rejected.
- While powered flight is active, hold Shift down to a full block, slab, and stair; verify the flight toggle disables only after the game reports ground contact. Press Shift while still airborne and confirm flight stays active.
- Visual check of back-model height/centering and both nozzle particle origins in third person
- Actual player-position verification while holding Space at each of the six limits, plus terrain transitions across flat ground, slopes, cliffs, slabs, platforms, leaves, water, and cave overhangs
- In-game double-space shutdown, reactivation during a fall, no-energy reactivation rejection, and Shift landing auto-shutdown tests
- Manual side-by-side movement comparison against Creative flight (forward/back/strafe/diagonal, vertical movement, sprint flight, acceleration and stopping)
- Client/server feel and multiplayer rendering/synchronization checks
- Powered and unpowered Shift descent, landing, and energy-depletion checks
- Shift-click and right-click refueling from the player inventory and hotbar
- Right-click from other mods' container screens is intentionally not intercepted; those layouts require their own safe integration
- Dedicated-server startup and in-game launch have not been run in this validation stage
