# Jetpack

Independent Minecraft Java 1.19.2 / Forge 43.5.0 mod project (Java 17).

The repository was bootstrapped from the six user-provided concept sheets and the inventory icon sheet. It registers six tiered jetpacks, shaped recipes, and a per-item Forge Energy capability. A full charge is 6,000 FE; fuel burn time is converted proportionally, with coal's 1,600-tick burn yielding exactly 600 FE (10%).

The equipment slot is drawn beside the chestplate slot in the vanilla inventory. Slot clicks are validated on the server; the jetpack and its FE remain in player persistent data. Right-click a jetpack to open its fueling menu. The fuel stack and burn progress are saved on the jetpack item. The client receives the equipped stack for inventory rendering and the HUD. Double-tap jump toggles survival flight; hold Space to climb and Shift to descend. Flight is capped by tier relative to the last safe ground position.

## Build

```sh
gradle --no-daemon build
```

The GitHub Actions workflow installs Java 17 and Gradle 7.5.1, runs Java tests, and uploads the built JAR as a workflow artifact. Local execution was not available in this environment because Gradle is not installed. Run 39380550237 passed compilation, packaging, and the current unit tests for fuel conversion.

## Planned implementation milestones

- Gameplay verification of the inventory slot, fuel menu, HUD, and flight
- Red flame/redstone/smoke particles and continuous flight sound
- Back-mounted 3D model layer for all six materials
- Dedicated-server and multiplayer validation

No gameplay feature is described as complete until it has passed a corresponding test.
