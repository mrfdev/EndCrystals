# 1MB-EndCrystals

`1MB-EndCrystals` is a small Paper plugin for one job: let end crystals behave like end crystals, but never let them break blocks. It also optionally protects decorative crystals from players and player-fired projectiles, independent from WorldGuard or any other region plugin.

This refresh targets Java 25 and has been built for modern Paper installs, with validation intended for:

- Paper `1.21.11`
- Paper `26.1.2`

The produced jar is named:

`1MB-EndCrystals-v2.0.1-021-v25-26.1.2.jar`

## What It Does

- Clears end crystal block damage without cancelling the whole explosion event
- Optionally blocks players from breaking protected crystals
- Optionally blocks player projectiles from breaking protected crystals
- Keeps The End configurable so dragon-fight style gameplay can stay intact if desired
- Stores config in a shared home-folder location: `~/plugins/1MB-EndCrystals/config.yml`
- Supports `/_endcrystals reload`
- Supports `/_endcrystals toggle <setting> [true|false]` for live boolean config toggles
- Supports `/_endcrystals debug` for runtime/build/config diagnostics

## Commands

- `/_endcrystals debug`
- `/_endcrystals reload`
- `/_endcrystals toggle`
- `/_endcrystals toggle protection.prevent-block-damage`
- `/_endcrystals toggle debug.log-block-protection true`

Aliases:

- `/endcrystals`
- `/ec`

## Permissions

- `1mb.endcrystals.admin`
- `1mb.endcrystals.debug`
- `1mb.endcrystals.reload`
- `1mb.endcrystals.toggle`
- `1mb.endcrystals.bypass`

`1mb.endcrystals.admin` grants the command permissions. `1mb.endcrystals.bypass` allows a player to break otherwise protected crystals.

## Config

The plugin reads and writes its main config here:

`~/plugins/1MB-EndCrystals/config.yml`

That makes it easy to share one config across multiple local servers started by the same user account.

Important defaults:

- `protection.prevent-block-damage: true`
- `protection.prevent-player-break: true`
- `protection.prevent-projectile-break: true`
- `protection.allow-player-break-in-the-end: true`
- `protection.clear-explosion-yield: true`

Live toggle keys are listed in the config under `live-toggles`. Those settings can be changed immediately with `/_endcrystals toggle ...` and also refresh when you run `/_endcrystals reload`.

## Build

If the local Paper `1.21.11` API jar is available in `servers/Server-One-Paper-1.21.11`, Gradle uses that directly for offline local builds. Otherwise it falls back to the Paper Maven repository.

Build with:

```bash
./gradlew build
```

Output jar:

`build/libs/1MB-EndCrystals-v2.0.1-021-v25-26.1.2.jar`

## Install

1. Build the jar.
2. Copy it into a Paper server's `plugins/` folder.
3. Start the server once so the shared config is created under `~/plugins/1MB-EndCrystals/`.
4. Use `/_endcrystals debug` to verify runtime information.
5. Use `/_endcrystals reload` after editing the config.

## Testing Notes

The plugin is meant to be testable without WorldGuard present. If an end crystal explodes near normal blocks, the explosion should still happen but the blocks should remain intact while the plugin is enabled.
