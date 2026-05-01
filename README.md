# SummitRaffle

A production-ready raffle plugin for [Paper](https://papermc.io/) Minecraft servers. Admins hold an item and start a raffle — players click a chat button to join, a winner is drawn automatically, and the item is delivered directly to their inventory.

---

## Requirements

| Requirement | Version  |
|-------------|----------|
| Java        | 21+      |
| Paper       | 1.21.4+  |

---

## Installation

1. Download the latest `SummitRaffle-x.x.x.jar` from [Releases](../../releases).
2. Drop the jar into your server's `/plugins` folder.
3. Restart the server — `config.yml` is generated automatically on first run.

---

## Commands

| Command | Permission | Description |
|---|---|---|
| `/raffle start` | `raffle.start` | Hold an item and run this to start a raffle. The item is taken from your hand. |
| `/raffle join` | `raffle.join` | Enter the currently active raffle. Clickable from chat. |
| `/raffle stop` | `raffle.stop` | Force-cancel the active raffle. The prize is returned to the creator. |

**Alias:** `/rf` works as a shorthand for `/raffle`.

---

## Permissions

### Admin

| Permission | Default | Description |
|---|---|---|
| `raffle.stop` | `op` | Force-cancel an active raffle. Prize is returned to the creator immediately (or on next login if offline). |

### Starting a Raffle

Starting a raffle requires **two** separate permissions: one that grants access to the command, and one that determines the cooldown.

**Step 1 — Grant start access**

| Permission | Default | Description |
|---|---|---|
| `raffle.start` | `op` | Required to run `/raffle start`. Without this, the command is blocked entirely. Assign this only to ranks you want to be able to host raffles. |

**Step 2 — Assign a cooldown tier**

Once a player has `raffle.start`, their cooldown is determined by whichever `raffle.cooldown.<tier>` permission they hold. If they hold more than one, they get the **lowest** (best) cooldown.

| Permission | Default | Cooldown |
|---|---|---|
| `raffle.cooldown.default` | `true` | 300s (5 min) — the baseline, granted to everyone |
| `raffle.cooldown.vip` | `false` | 180s (3 min) |
| `raffle.cooldown.mvp` | `false` | 60s (1 min) |

> **Example setup with a permission plugin:**
> - Regular players: no `raffle.start` — cannot host raffles at all
> - VIP rank: `raffle.start` + `raffle.cooldown.vip` — can start a raffle every 3 minutes
> - MVP rank: `raffle.start` + `raffle.cooldown.mvp` — can start a raffle every 1 minute
> - Staff/op: `raffle.start` + `raffle.stop` — can start and force-cancel raffles

Cooldown tiers are fully configurable in `config.yml`. Add as many as you like — the key just needs to match the permission suffix (`raffle.cooldown.<key>`).

### Joining a Raffle

| Permission | Default | Description |
|---|---|---|
| `raffle.join` | `true` | Grants access to `/raffle join`. Granted to all players by default. Remove or negate this to restrict joining. |

---

## Configuration

The full `config.yml` is generated on first run. All values can be changed live — reload the server to apply.

```yaml
# Prefix used wherever {prefix} appears in a message
prefix: "&6[SummitRaffle]&r"

# Raffle entry window in seconds
duration: 30

# Start cooldowns — permission node: raffle.cooldown.<key>
# Player always gets the lowest cooldown they qualify for
cooldowns:
  default: 300   # raffle.cooldown.default — 5 minutes
  vip:     180   # raffle.cooldown.vip    — 3 minutes
  mvp:     60    # raffle.cooldown.mvp    — 1 minute
```

### Adding a custom rank

1. Add a tier to `config.yml`:
   ```yaml
   cooldowns:
     elite: 30   # 30 second cooldown
   ```
2. Give players the permission `raffle.cooldown.elite`.
3. Reload the server — no restart required.

### Messages

Every player-facing string lives under `messages:` in `config.yml`. All messages support `&` colour codes and the following placeholders:

| Placeholder | Description |
|---|---|
| `{prefix}` | The configured plugin prefix |
| `{prize}` | The prize item name (e.g. `64x Diamond`) |
| `{player}` | A player's name |
| `{seconds}` | Cooldown or countdown seconds remaining |
| `{count}` | Number of raffle participants |

---

## Features

- **Item preservation** — Full NBT is captured via Paper's binary serialization. Enchantments, custom names, lore, potions, books — everything survives.
- **Clickable announcements** — The start panel and countdown reminders are clickable in chat, running `/raffle join` automatically.
- **Centered panels** — Start and winner announcements are pixel-accurately centered using Minecraft's character width table.
- **Offline prize delivery** — If the winner or creator is offline when the prize should be delivered, it is persisted to `pending_prizes.yml` and delivered on next login.
- **Inventory full handling** — If a player's inventory is full on delivery, the item drops naturally at their feet.
- **Server restart safety** — If the server restarts or reloads mid-raffle, the prize is automatically queued for the creator via the pending prize system.
- **Per-rank cooldowns** — Cooldown tiers are permission-based and fully configurable. Players always get their best applicable tier.
- **Daily logs** — Every raffle start and end is written to `plugins/SummitRaffle/logs/YYYY-MM-DD.txt` with full participant lists and winner details.
- **Admin force-stop** — Admins can cancel any active raffle with `/raffle stop`. The prize is always returned.

---

## How a Raffle Works

```
Admin holds item → /raffle start
        ↓
Server broadcasts announcement panel (clickable "Join" button)
        ↓
Countdown: 30s → 20s → 10s → 5s → 4s → 3s → 2s → 1s
        ↓
"Entries closed — N participants"
        ↓  (1 second pause)
"Drawing a winner..."
        ↓  (2 more seconds)
Winner picked at random → item delivered
        ↓
Winner announcement panel broadcast
```

**Edge cases:**
- Nobody entered → item returned to the creator
- Winner offline → item queued, delivered on next login
- Winner's inventory full → item drops at their feet
- Creator offline at return → item queued via pending prize system
- Server restarts mid-raffle → item queued for creator

---

## Logs

Raffle events are written to:
```
plugins/SummitRaffle/logs/YYYY-MM-DD.txt
```

**Example entries:**
```
[2025-05-01 14:32:11] [RAFFLE START] Player: Steve (uuid) | Prize: 64x Diamond | Duration: 30s
[2025-05-01 14:32:44] [RAFFLE END]   Prize: 64x Diamond | Participants: 4 | WINNER: Alice (uuid) [online=true] | All entrants: [uuid1, uuid2, uuid3, uuid4]
```

A new file is created automatically each day.

---

## Project Structure

```
SummitRaffle/
├── src/main/
│   ├── java/com/summitcraft/summitraffle/
│   │   ├── SummitRaffle.java           — Plugin entry point
│   │   ├── command/
│   │   │   ├── RaffleCommand.java      — /raffle start|join|stop
│   │   │   └── Messages.java           — All player-facing text (config-driven)
│   │   ├── config/
│   │   │   └── ConfigManager.java      — Loads config.yml, exposes typed values
│   │   ├── cooldown/
│   │   │   └── CooldownManager.java    — Per-player cooldown tracking & tier resolution
│   │   ├── logging/
│   │   │   └── LogManager.java         — Daily rotating log files
│   │   ├── prize/
│   │   │   └── PendingPrizeManager.java — Offline prize persistence & delivery
│   │   └── raffle/
│   │       ├── Raffle.java             — Raffle data model
│   │       ├── RaffleManager.java      — Lifecycle, countdown, edge cases
│   │       └── WinnerResolver.java     — Random draw, prize delivery
│   └── resources/
│       ├── plugin.yml
│       └── config.yml
├── build.gradle
└── settings.gradle
```

---

## Building from Source

Requires JDK 21+.

```bash
git clone https://github.com/your-org/SummitRaffle.git
cd SummitRaffle
gradle build
```

Output jar: `build/libs/SummitRaffle-<version>.jar`

---

## License

This project is licensed under the [MIT License](LICENSE).
