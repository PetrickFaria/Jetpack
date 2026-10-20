# Implementation status

## Implemented in source (build and unit tests passed in Actions run 39380550237)

- Forge 43.5.0 project targeting Minecraft 1.19.2 and Java 17
- Six item registrations and crafting recipes
- Six inventory icons cropped from the user-provided icon reference
- NBT-persisted Forge Energy capability (6,000 FE capacity)
- Sneak-right-click equipment swap stored independently of chest armor
- Visible inventory control beside the chestplate with server-validated equip, swap, and removal
- Server-to-client equipment snapshot used by inventory rendering and HUD
- Right-click fueling menu with persistent fuel slot and burn progress
- Proportional ER charging; 1,600 burn ticks map to 600 FE, exactly 10% of capacity
- Red ER HUD bar and percentage display
- Server-validated double-tap jump flight toggle
- Space ascent, Shift descent, five-to-fifty block tier ceiling, and last-safe-ground reference
- Energy drain during powered flight and three-block fall braking
- GitHub Actions build workflow

## Not implemented or validated yet

- In-game checks for slot clicks, fuel insertion/removal, charge display, and HUD synchronization
- Integration tests for real item persistence and fuel inventory transactions
- Red flame/redstone/smoke particles and continuous flight sound
- Back-mounted 3D player models and six material textures
- Client/server play tests, dedicated-server test, and world save/reload test

GitHub Actions compiled and packaged the mod and passed three unit tests for proportional fuel conversion. No Minecraft client, multiplayer session, dedicated server, or save/reload test has been run, so gameplay behavior remains unverified.

The six recipes follow the ingredient positions in the supplied specification. The Netherite recipe uses netherite ingots in the top/side positions and the lower-center position, as requested.
