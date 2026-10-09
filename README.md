# Screaming BedWars (fork)

BedWars plugin for Minecraft 1.8.8 to 26.3, packaged as one jar. Based on [ScreamingSandals BedWars](https://github.com/ScreamingSandals/BedWars) and inspired by BedwarsRel.

## Features

- Core BedWars game with beds, teams and spectators.
- CakeWars, EggWars and AnchorWars, using a cake, dragon egg or respawn anchor as the target.
- Arena variants, such as `default` and `certain-popular-server`.
- Shops, with several shops per arena.
- BungeeCord support.
- Vault rewards.
- Spectators can join running games.
- Fast arena rebuilding.
- BossBar or XP bar for the lobby countdown or game time.
- Breakable blocks that refresh after a rebuild, and ignored blocks.
- Special items such as RescuePlatform and TNTSheep, also sold in shops.
- Selection GUI for teams.
- Automatic coloring of items such as armor.
- Resource spawners, in any number.
- Player statistics.
- Generator tiers, including Diamond and Emerald II and III.
- A game timeline with bed destruction, sudden death and game end.
- Ender dragons in sudden death, one per team still alive.
- A draw result when the time limit ends.
- Team upgrades: Sharpness, Protection, Haste, Iron Forge tiers, Heal Pool and Dragon Buff, with a price for each tier.
- A trap queue with It's a Trap, Counter-Offensive, Alarm and Miner Fatigue.
- Built-in parties with `/party` commands.
- Game sizes picked at lobby NPCs, with team sizes filled automatically.
- Short setup commands: `/bw setup` and `/bw set`.
- Arena cloning with `/bw admin <arena> clone` and `/bw setup clone`.
- Most behavior is configurable.

The Hypixel-style behavior ships in the bundled `certain-popular-server` variant. The timings and prices are configurable.

## Setting up a server

1. Build or pick the maps. Each arena has 2, 3 or 4 teams.
2. Create each arena with `/bw setup <arena> certain-popular-server`. This variant adds the timeline, upgrades and traps.
3. Set the arena at your position with `/bw set`:
    - `pos1` and `pos2` at opposite corners of the arena.
    - `lobby` and `spectator`.
    - `team <color>`, then `spawn <team>` and `bed <team>` (stand on or look at the bed).
    - `generator <team>`, `shop <team>` and `upgrades <team>`.
    - `diamond` and `emerald` for the resource spawners.

    `/bw setup status` shows what is still missing.
4. Save with `/bw setup save` once the required steps are done.
5. Run `/bw mainlobby set` to mark the lobby. Run `/bw mainlobby enable` so players return after a game.
6. Stand at the hub spot and run `/bw npc add`. Set its click action with `/bw npc action TELEPORT_TO_LOBBY`.
7. In the lobby, run `/bw npc spawnmodes`. It spawns one NPC per game size.
8. Players click a size NPC and join a waiting arena of that size.
    - If none is free, an idle arena with the same number of teams is claimed for them.

## Command reference

### Setup

| Command | What it does |
|---|---|
| `/bw setup` | Shows the selected arena and what is still missing. |
| `/bw setup <arena> [variant]` | Creates or opens an arena for setup and selects it. |
| `/bw setup status` | Shows the setup checklist of the selected arena. |
| `/bw setup variant <variant>` | Changes the variant of the selected arena. |
| `/bw setup save` | Saves the arena once all required steps are done. |
| `/bw setup save force` | Saves the arena and skips the warnings. |
| `/bw setup cancel` | Discards unsaved changes. A new arena that was never saved is dropped. |
| `/bw setup clone <new-name> <x> <y> <z> [world]` | Same as `/bw admin <arena> clone` for the selected arena. Also `confirm`, `cancel` and `status`. |
| `/bw set pos1` | Sets the first corner of the arena at your position. |
| `/bw set pos1 force` | Sets the first corner and skips the overlap check. |
| `/bw set pos2` | Sets the opposite corner of the arena at your position. |
| `/bw set pos2 force` | Sets the opposite corner and skips the overlap check. |
| `/bw set lobby` | Sets the waiting spawn for players. |
| `/bw set lobbypos1` | Sets one corner of the lobby region. |
| `/bw set lobbypos2` | Sets the other corner of the lobby region. |
| `/bw set spectator` | Sets the spectator spawn. `/bw set spec` works too. |
| `/bw set team <color> [size]` | Adds a team of that color. Without a size, `setup.default-team-size` applies. |
| `/bw set spawn <team>` | Sets the spawn of a team at your position. |
| `/bw set bed <team>` | Sets the bed of a team. Stand on it or look at it. |
| `/bw set target <team>` | Sets a non-bed target block of a team, the block you look at. |
| `/bw set generator <team>` | Places the team generators here and replaces the existing ones. |
| `/bw set generator <team> add` | Places the team generators here and keeps the existing ones. |
| `/bw set diamond` | Places a diamond spawner at your position. |
| `/bw set emerald` | Places an emerald spawner at your position. |
| `/bw set shop [team]` | Places a shop at your position, optionally for a team. |
| `/bw set upgrades [team]` | Places the upgrade shop at your position, optionally for a team. |

### Arenas

| Command | What it does |
|---|---|
| `/bw admin <arena> clone <new-name> <x> <y> <z> [world]` | Copies the arena to a new position. Each coordinate is a number, `~`, or `~<n>` relative to your block. |
| `/bw admin <arena> clone confirm` | Starts the pending clone. |
| `/bw admin <arena> clone cancel` | Cancels the pending clone. |
| `/bw admin <arena> clone status` | Shows the state of the current clone. |
| `/bw admin <arena> variant <variant>` | Changes the variant of an arena that is open for setup. |
| `/bw admin <arena> prefab <prefab>` | Runs a variant prefab on an arena open for setup. `upgrade-shop` (`certain-popular-server`) places an upgrade shop at your position. |

### Lobby and NPCs

| Command | What it does |
|---|---|
| `/bw mainlobby set` | Sets the main lobby to your position. |
| `/bw mainlobby enable` | Players return to the main lobby after a game. |
| `/bw npc add` | Places an NPC at your position and selects it for editing. |
| `/bw npc select` | Selects an existing NPC by clicking it. |
| `/bw npc quit` | Stops editing the selected NPC. |
| `/bw npc remove` | Removes the selected NPC. |
| `/bw npc action TELEPORT_TO_LOBBY` | On click, teleports the player to the main lobby. |
| `/bw npc action JOIN_MODE <mode>` | On click, joins a game size, such as `4v4v4v4`. |
| `/bw npc action <action> [value]` | Sets what a click does. Other actions: `JOIN_GAME`, `JOIN_GROUP`, `JOIN_VARIANT`, `JOIN_RANDOM`, `OPEN_GAMES_INVENTORY`, `PLAYER_COMMAND`, `CONSOLE_COMMAND`, `DUMMY`. |
| `/bw npc spawnmodes [team-count] [spacing]` | Spawns one `JOIN_MODE` NPC per game size, in a row from your position. A team count limits the sizes. |
| `/bw npc shouldLookAtPlayer <value>` | Sets whether the selected NPC turns toward players. |
| `/bw npc skin <skin>` | Sets the skin of the selected NPC. |
| `/bw npc hologram addline <line>` | Adds a hologram line to the selected NPC. |
| `/bw npc hologram setline <number> <line>` | Changes one hologram line of the selected NPC. |
| `/bw npc hologram remove <number>` | Removes one hologram line of the selected NPC. |
| `/bw npc hologram clear` | Removes all hologram lines of the selected NPC. |

### Game sizes

| Command | What it does |
|---|---|
| `/bw mode list` | Lists the game sizes with player and arena counts. |
| `/bw mode join <mode>` | Joins a game size, such as `2v2`. |
| `/bw mode leavequeue` | Leaves the queue for a game size. |

### Parties

| Command | What it does |
|---|---|
| `/party` or `/party help` | Shows the party help. |
| `/party invite <player>` | Invites an online player. |
| `/party <player>` | Same as `/party invite <player>`, when `party.commands.invite-shortcut` is on. |
| `/party accept [player]` | Accepts an invite, optionally from a named player. |
| `/party deny [player]` | Declines an invite, optionally from a named player. |
| `/party leave` | Leaves your party. |
| `/party kick <player>` | Removes a member. `/party remove <player>` works too. |
| `/party disband` | Disbands the party. |
| `/party list` | Shows the members. `members` and `info` work too. |
| `/party chat <message>` | Sends one message to party chat. |
| `/party chat` | Toggles party chat on and off. |
| `/party transfer <player>` | Gives leadership to a member. `promote` works too. |
| `/party warp` | Leader only. Pulls members into your waiting game, or teleports them to you outside games. |

`/p` works as an alias of `/party` (`party.commands.aliases`). All party commands also run under `/bw party`.

## Configuration

- Settings resolve per arena (`/bw admin <arena> config`), then the variant's `config:` block, then `config.yml`.
- `timeline:` in a variant file (`variants/<name>.yml`): spawner tier intervals and event times.
- `modes.list`: defines the game sizes, each with an id, team count, team size and minimum players.
- `modes.allowed-team-sizes`: team sizes an arena accepts for game sizes (default `1` to `4`).
- `modes.only-via-mode-selection`: unclaimed arenas accept non-admins only through game-size NPCs and `/bw mode join`.
- `party.max-size`: largest party size (default `4`).
- `party.invite-expire-seconds`: how long a party invite stays valid (default `60`).
- `sudden-death.dragon.block-destruction`: blocks dragons can break: `all`, `placed` (default) or `none`.
- `game-end-by-time.mode`: result when the time limit ends, `draw` (default) or `tie-break`.
- `bundled-files.auto-update`: replaces outdated bundled variant and shop files on start (default `true`).
- `setup.default-team-size`: team size for `/bw set team` without a size (default `4`).
- `setup.team-generator-types`: generator types placed by `/bw set generator` (default `bronze`; the `certain-popular-server` variant uses iron and gold).
- `clone.blocks-per-tick`: blocks copied per tick when cloning an arena (default `4096`).

## Compiling

This project uses **Gradle** and requires **JDK 17** or newer. To build it, clone the repository and run:

```bash
./gradlew clean build
```

On Windows, use:

```bat
gradlew.bat clean build
```

The compiled JAR file is located in `plugin/universal/build/libs`.

## Upstream

The upstream [Discord](https://discord.gg/4xB54Ts) and [docs](https://docs.screamingsandals.org) cover the upstream project, not this fork.

## License

This project is licensed under the **GNU Lesser General Public License v3.0**. See the [LICENSE](LICENSE) file for details.
