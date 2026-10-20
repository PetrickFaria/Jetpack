# Jetpack

Independent Minecraft Java 1.19.2 / Forge 43.5.0 mod project (Java 17).

The repository was bootstrapped from the six user-provided concept sheets and the inventory icon sheet. The first implementation milestone registers six tiered jetpack items, defines their shaped recipes, and adds a per-item Forge Energy capability. The five-minute capacity is 6,000 FE; coal's vanilla 1,600-tick burn maps to 600 FE (exactly 10%).

## Build

```sh
gradle --no-daemon build
```

The GitHub Actions workflow installs Java 17 and Gradle 7.5.1 and uploads the built JAR as a workflow artifact. Local execution was not available in this environment because Gradle is not installed; the first Actions run is the build validation gate.

## Planned implementation milestones

- Custom persistent equipment slot and client/server synchronization
- Fueling menu, gradual fuel conversion, and percentage display
- Double-jump flight controls, height limits, automatic braking, HUD, particles, and sound
- Back-mounted 3D model layer for all six materials
- Dedicated-server and multiplayer validation

No gameplay feature is described as complete until it has passed a corresponding test.
