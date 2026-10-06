<div align="center">

# EssentialsCooldown

**Configurable, rank-based cooldowns for EssentialsX commands.**

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-3C8527?style=for-the-badge)](https://www.minecraft.net/)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](LICENSE)

[![Bukkit](https://img.shields.io/badge/Bukkit-supported-informational?style=flat-square)]()
[![Spigot](https://img.shields.io/badge/Spigot-supported-informational?style=flat-square)]()
[![Paper](https://img.shields.io/badge/Paper-supported-informational?style=flat-square)]()
[![Folia](https://img.shields.io/badge/Folia-supported-informational?style=flat-square)]()
[![ShreddedPaper](https://img.shields.io/badge/ShreddedPaper-supported-informational?style=flat-square)]()
[![MultiPaper](https://img.shields.io/badge/MultiPaper-synced-success?style=flat-square)]()

</div>

---

EssentialsCooldown adds **cooldowns to the most commonly used player commands in
EssentialsX**, configurable per command and per rank. It was written for a
personal server and released publicly in the hope that others find it useful —
there did not appear to be anything quite like it.

> **Requires [EssentialsX](https://essentialsx.net/).** This plugin only adds
> cooldowns to EssentialsX commands; it does not implement any commands itself.

## Features

- **Per-command, per-rank cooldowns** — every command can have a different
  cooldown for every rank.
- **Unlimited, fully configurable ranks** — a rank named `hyper` simply uses the
  permission node `essentialscooldown.<rank>`. Adding a rank is a config change,
  not a code change.
- **Disable a command per rank** — set the cooldown to `0` and that rank cannot
  use the command at all.
- **Alias-aware** — EssentialsX aliases are detected from the server and share
  their parent command's cooldown, so they cannot be used to dodge it.
- **Reload without restarting** — `/essentialscooldown reload`.
- **Cross-server cooldowns on MultiPaper** — cooldowns follow the player across
  every server in the network and survive restarts.

## Supported platforms

One jar runs on all of the following:

| Platform | Notes |
|---|---|
| **Bukkit** (and forks) | — |
| **Spigot** (and forks) | — |
| **Paper** (and forks) | — |
| **Folia** | Opted in via `folia-supported: true` |
| **ShreddedPaper** | Opted in via `folia-supported: true` |
| **MultiPaper** | Cooldowns shared and persistent across servers |

## Requirements

- [EssentialsX](https://essentialsx.net/)
- Java 25
- A Bukkit-derived server (any of the platforms above)

## Installation

1. Drop `EssentialsCooldown-<version>.jar` into your server's `plugins/` folder.
2. Make sure [EssentialsX](https://essentialsx.net/) is installed.
3. Start the server. `plugins/EssentialsCooldown/config.yml` is created on first
   run.
4. Edit the config, then run `/essentialscooldown reload`.

## Commands

| Command | Aliases | Description |
|---|---|---|
| `/essentialscooldown reload` | `/ecool`, `/esscool` | Reload the configuration. |

## Permissions

| Node | Description |
|---|---|
| `essentialscooldown.<rank>` | Grants a rank's cooldown. One node per rank in your config — e.g. `essentialscooldown.player`, `essentialscooldown.vip`, `essentialscooldown.hyper`. |
| `essentialscooldown.bypass` | Bypass all cooldowns. |
| `essentialscooldown.admin` | Use `/essentialscooldown reload`. |

## Configuration

Cooldowns are listed **per command, most specific rank first**. The first rank a
player holds wins.

```yaml
fallback-rank: player

commands:
  heal:
    aliases: [eheal]
    cooldowns:
      - legend: 30
      - vip: 120
      - player: 300
```

| Field | Meaning |
|---|---|
| `cooldowns` | An ordered list of `rank: seconds`. First match wins. |
| `fallback-rank` | The rank used when a player holds **none** of a command's rank nodes. |
| `aliases` | Extra names that share the command's cooldown. |

### How it behaves

- **Cooldown values are in seconds.**
- **`0` disables the command** for that rank.
- Ranks are checked in the order written, so put the most specific rank first.
- A player with no matching rank node falls back to `fallback-rank`.

### Worked example

With the config above:

| Player holds | `/heal` cooldown |
|---|---|
| `essentialscooldown.legend` | 30 seconds |
| `essentialscooldown.vip` | 2 minutes |
| `essentialscooldown.player` | 5 minutes |
| *(nothing)* | 5 minutes (the fallback rank) |

## Adding your own ranks and commands

**Adding a rank** — copy a line inside any `cooldowns` list, rename it, and set
the value. No code change or recompile is needed:

```yaml
  heal:
    aliases: [eheal]
    cooldowns:
      - hyper: 60
      - player: 300
```

Then grant `essentialscooldown.hyper` in LuckPerms.

**Adding a command** — copy a whole command block and rename the key to the
EssentialsX command name (without the slash):

```yaml
  spawn:
    aliases: []
    cooldowns:
      - player: 300
```

You do not need to know the command's aliases. On startup and on reload the
plugin asks the server which aliases a command has and adds them automatically,
so a copied block cannot be bypassed through an alias you did not list.

> The plugin **only** supports EssentialsX commands.

## MultiPaper

On a MultiPaper network, cooldowns are **shared between every server and persist
across restarts**, so a player cannot dodge a cooldown by switching servers.
This is automatic — no configuration required. On any other server the cooldown
store is per-server and in-memory.

## Building from source

Requires **JDK 25** and **Maven**.

```bash
mvn clean package
```

The finished jar is written to `Compiled/<version>/`.

```
pom.xml                  Maven project
src/main/java/           Plugin source
src/main/resources/      config.yml, plugin.yml
Compiled/<version>/      Build output (gitignored)
```

## Disclosure

EssentialsCooldown has had help from AI in its development. It is not fully
AI-written — AI assisted with laying out and reviewing the code.

## License

Released under the [Apache License 2.0](LICENSE).
