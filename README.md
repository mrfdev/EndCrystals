# 1MB-EndCrystals

`1MB-EndCrystals` is a small Paper plugin for one job: let end crystals behave like end crystals, but never let them break blocks. It also optionally protects decorative crystals from players and player-fired projectiles, independent from WorldGuard or any other region plugin.

This refresh targets Java 25 and has been built for modern Paper installs, with validation intended for:

- Paper `1.21.11`
- Paper `26.1.2`

The produced jar is named:

`1MB-EndCrystals-v2.0.1-021-v25-26.1.2.jar`

## Feature List

- Clears end crystal block damage without cancelling the whole explosion event
- Prevents item and decorative entity destruction from crystal explosions
- Protects minecart variants, boat or raft variants, display entities, paintings, frames, armor stands, leash knots, and dropped items
- Optionally blocks players from breaking protected crystals
- Optionally blocks player projectiles from breaking protected crystals
- Keeps The End configurable so dragon-fight style gameplay can stay intact if desired
- Uses the normal Paper plugin data folder: `plugins/1MB-EndCrystals/config.yml`
- Migrates the misplaced legacy home-folder config into the correct server-local folder on first startup
- Supports `/_endcrystals reload`
- Supports `/_endcrystals toggle [setting] [true|false]` for live boolean config toggles
- Supports `/_endcrystals debug` for runtime/build/config diagnostics
- Supports configurable command aliases from `config.yml`
- Uses MiniMessage formatting for plugin output
- Uses strict permission nodes with op not automatically bypassing protection
- Targets Java `25` and modern Paper builds
- Builds with Gradle from a fresh clone without requiring any private `/servers/` test setup

## Commands

- `/_endcrystals debug`
- `/_endcrystals reload`
- `/_endcrystals toggle`
- `/_endcrystals toggle protection.prevent-block-damage`
- `/_endcrystals toggle debug.log-block-protection true`

Configurable aliases:

- `/endcrystals`
- `/ec`

Those aliases are controlled by `commands.aliases` in `plugins/1MB-EndCrystals/config.yml`. Set `commands.aliases: []` if you want no extra aliases. Changes apply after `/_endcrystals reload`.
If another plugin or the server already owns one of those names, 1MB-EndCrystals skips that alias and logs a warning while keeping `/_endcrystals` available.

## Permissions

- `onembendcrystals.admin`
- `onembendcrystals.debug`
- `onembendcrystals.reload`
- `onembendcrystals.toggle`
- `onembendcrystals.break`

All of these permissions default to `false`, so being op alone does not grant them. `onembendcrystals.admin` grants the command permissions. `onembendcrystals.break` allows a player to break otherwise protected crystals.

## Config

The plugin reads and writes its main config here:

`plugins/1MB-EndCrystals/config.yml`

Each Paper server gets its own local config folder, which is the normal Bukkit/Paper behavior.

If you are upgrading from an older build that wrote to `~/plugins/1MB-EndCrystals/config.yml`, the plugin will copy that legacy file into the server-local plugin folder on first startup if the new server-local config does not already exist.

Important defaults:

- `commands.aliases: [endcrystals, ec]`
- `protection.prevent-block-damage: true`
- `protection.prevent-player-break: true`
- `protection.prevent-projectile-break: true`
- `protection.prevent-protected-entity-damage: true`
- `protection.allow-player-break-in-the-end: false`
- `protection.clear-explosion-yield: true`

By default the protected entity list includes armor stands, dropped items, item frames, glow item frames, paintings, display entities, leash knots, minecart variants, and boat or raft variants. Live toggle keys are listed in the config under `live-toggles`. Those settings can be changed immediately with `/_endcrystals toggle [setting] [true|false]` and also refresh when you run `/_endcrystals reload`.

## Changelog

### Legacy -> `v2.0.1` Refresh

This section summarizes the modernization work completed for the current refresh.

- Migrated the plugin to a Gradle build for Java `25`
- Retargeted the plugin for modern Paper, validated against `1.21.11` and `26.1.2`
- Standardized the shipped jar name to `1MB-EndCrystals-v2.0.1-021-v25-26.1.2.jar`
- Reworked config storage to use the active server's `plugins/1MB-EndCrystals/config.yml`
- Added one-time migration support for the old misplaced `~/plugins/1MB-EndCrystals/config.yml`
- Added `/_endcrystals reload`
- Added `/_endcrystals debug`
- Added live config toggles through `/_endcrystals toggle [setting] [true|false]`
- Moved command aliases into `config.yml` so they can be added or removed without editing `plugin.yml`
- Kept `/_endcrystals` as the permanent primary command
- Switched to explicit `onembendcrystals.*` permission nodes with `default: false`
- Made crystal breaking permission-driven instead of implicitly allowing ops
- Expanded protection beyond blocks to cover decorative and item entities
- Added protection for minecart and boat-style entities that were still vulnerable on the legacy path
- Improved debug output to show build/runtime info, config path, permissions, live toggles, and active commands
- Moved plugin messages into config and formatted output with MiniMessage
- Updated docs so public builds do not depend on any private local `/servers/` test directory

## Build

The public build does not depend on any local `servers/` directory. A normal checkout builds against the Paper API from the Paper Maven repository, and any repo-local test servers are strictly optional private development tooling.

Build with:

```bash
./gradlew build
```

Output jar:

`build/libs/1MB-EndCrystals-v2.0.1-021-v25-26.1.2.jar`

## Install

1. Build the jar.
2. Copy it into a Paper server's `plugins/` folder.
3. Start the server once so the plugin creates `plugins/1MB-EndCrystals/config.yml`.
4. Use `/_endcrystals debug` to verify runtime information.
5. Use `/_endcrystals reload` after editing the config.

## Testing Notes

The plugin is meant to be testable without WorldGuard present. If an end crystal explodes near normal blocks, the explosion should still happen but the blocks should remain intact while the plugin is enabled. Any `/servers/` directory used in development is for private local testing only and is not part of the public build flow.
