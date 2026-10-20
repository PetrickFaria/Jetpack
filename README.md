# Jetpack

Independent Minecraft Java 1.19.2 / Forge 43.5.0 mod project (Java 17).

Initial milestone: six tiered jetpack items, shaped recipes, inventory icons based on the supplied reference sheet, and per-item Forge Energy capability. The flight, dedicated equipment slot, fueling UI, effects, and wearable 3D rendering are planned milestones and are not yet implemented or validated.

Build with `gradle --no-daemon build`. GitHub Actions installs Java 17 and Gradle 7.5.1, compiles the mod, and publishes a JAR workflow artifact. Local build validation is pending because Gradle is unavailable in the current workspace.