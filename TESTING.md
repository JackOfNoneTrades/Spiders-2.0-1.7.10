# Verification

```sh
VERSION=0.1.0 ./gradlew build
VERSION=0.1.0 ./gradlew runServer -PspiderIntegration
```

The integration fixture uses the development world in `run/server` and builds a small test arena. It verifies vanilla entity IDs and NBT, wall-to-ceiling traversal for spiders, cave spiders and subclasses (including larger bodies), slab traversal, snow-layer collision handling, pursuit around platform edges, ceiling drops onto disconnected platforms (normal and cave spiders), blocked descents, falling, cave-spider poison, data-watcher compatibility, and disabling the feature. It stops the server and fails the Gradle task if a check fails. Test classes are excluded from release jars.

For packaged-jar testing, `runObfServer -PrunServerWorkingDirectory=run/obfuscated` uses the prepared obfuscated mods directory. Accept the Minecraft EULA in the chosen development server directory before running either server.

Client checks: inspect the mod-list/Catalogue entry, change and save a config value, then view spiders climbing a wall and ceiling while connected to a server. Subclasses that override movement or AI without calling the base implementation may need a compatibility patch; `excluded_classes` can disable the changes for a class and its subclasses.

Rotation regressions cover interpolation across the wall/ceiling yaw seam, real half-turns, vertical-climbing body heading, and synchronized surface/body/head directions. In the client, watch a climbing spider round an overhang: its body should keep facing its movement direction without flipping as the surface changes.

Platform attachment checks cover normal and cave spiders at the middle and both ends of all four faces of a one-block-thick ledge, with and without nearby snow (48 cases). They verify that attached spiders tilt their feet toward the ledge and keep the body tangent to its surface, rather than remaining upright with their head against the edge.

Replanning checks change the destination every five ticks and reject backward movement toward old cell centers. The isolated-block fixture starts normal and cave spiders on one floating plank: they must plan a safe drop to the ground, then climb a separate platform and attack. It also checks that disabled or insufficient drop limits prevent that route and that permitted drops do not damage the spiders.

To test pursuit manually, use a floating platform below a roof connected to a separate pillar. Leave a clear drop within the configured maximum (eight blocks by default) from the roof to the platform. Stand on the platform in Survival and provoke a spider below; it should climb the pillar, cross the underside, and drop onto you. Test at night to prevent vanilla daylight behavior from ending its aggression. Single snow layers around the pillar should not prevent climbing or leave it upright on a wall.

**Falling** settings apply on the server (including singleplayer's integrated server). `max_drop_height` defaults to 8 blocks; 0 disables planned drops. `fall_damage` defaults to true. `safe_fall_distance=-1` follows the drop limit automatically; 0 or higher sets an independent damage-free distance. Beyond it, vanilla fall damage applies: one damage point per extra block, rounded up, with normal potion effects and Forge damage hooks. Vanilla measures accumulated fall distance before the landing tick, so exact damage can vary with the final movement step. Changes apply without reloading the world. Integration checks cover the thresholds for normal/cave/subclass spiders, the independent override, disabling damage, changing the drop limit, actual landings, and unchanged vanilla damage for other mobs and unmodified spiders.

For diagnostics, enable **Pathfinding → Debug logging** (`B:debug_logging=true` in `config/spiderstpo/spiderstpo.cfg`), or launch with `MCMODDING_DEBUG_MODE` present. Logging runs on the server, including the integrated server in singleplayer. It covers spiders within 32 blocks of a player and spiders with an attack target. Look for `[SpiderDebug]` in `logs/fml-client-latest.log` (singleplayer) or `logs/fml-server-latest.log` (dedicated server): each spider has an ID, dimension, target and movement snapshots, path-search outcomes/budget usage, nearby blocks, overhead candidates, and drop/attack decisions. Snapshots and search traces are limited to once per 20 ticks per spider; events also log when they happen. Disable it after reproducing the issue.

The platform regression fixture includes the reported terrain, island and offset overhang, stored as block-only geometry in `src/test/resources/platform-repro.txt`. It checks both spider types from three failing starting positions with the player at three locations, rejects corner-only support and walking directly across the gap, and checks reachable landings beside the target.
