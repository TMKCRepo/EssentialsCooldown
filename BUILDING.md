# Building EssentialsCooldown

Requires **JDK 25** and **Maven**.

```
cd Source/1.0.0
mvn clean package
```

The finished plugin jar is written to:

```
Compiled/1.0.0/EssentialsCooldown-1.0.0.jar
```

Drop that jar into your server's `plugins/` folder.

## Repository layout

```
Source/<version>/     Maven project (pom.xml + src/)
Compiled/<version>/   Built plugin jar
```

## Platform support

One jar runs on Bukkit, Spigot, Paper, Folia, ShreddedPaper and MultiPaper.

- `folia-supported: true` is set in `plugin.yml`; Folia and ShreddedPaper only
  load plugins that opt in.
- MultiLib (the official MultiPaper compatibility library) is bundled and
  relocated into `com.tmkc.essentialscooldown.libs.multilib`, so it cannot clash
  with another plugin's copy.
- On MultiPaper, cooldowns are shared across every server in the network and
  persist across restarts, so a player cannot dodge a cooldown by switching
  servers. On other servers the cooldown store is per-server and in-memory.

## Configuration

`config.yml` is written to the plugin data folder on first run. The shipped copy
is fully annotated; the same file lives at
`Source/1.0.0/src/main/resources/config.yml`.

Ranks are listed per command, most specific first:

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

- The permission node for a rank is `essentialscooldown.<rank>`.
- `fallback-rank` applies when a player holds none of a command's rank nodes.
- `0` disables the command for that rank.
- Command aliases are auto-detected from the server, so a copied command block
  cannot be bypassed through an alias you did not list.
