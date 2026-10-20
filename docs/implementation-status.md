# Implementation status

## In the initial source commit

- Forge 43.5.0 project targeting Minecraft 1.19.2 and Java 17
- Six item registrations and crafting recipes
- Six inventory icons cropped from the user-provided icon reference
- NBT-persisted Forge Energy capability (6,000 FE capacity)
- GitHub Actions build workflow

## Not implemented or validated yet

- Separate player equipment slot and its multiplayer synchronization
- Fueling menu and gradual burn-to-energy processing
- Flight state, double-jump controls, tier height caps, and automatic landing brake
- HUD, red flame/redstone/smoke particles, and flight sound
- Back-mounted 3D player models and six material textures
- Client/server play tests, dedicated-server test, and world save/reload test

The six recipes follow the ingredient positions in the supplied specification. The Netherite recipe uses netherite ingots in the top/side positions and the lower-center position, as requested.
