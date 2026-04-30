# SummitRaffle

A lightweight, production-ready raffle plugin for [Paper](https://papermc.io/) Minecraft servers. Lets server administrators run in-game raffles where players can enter for a chance to win prizes.

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Java        | 21+     |
| Paper       | 1.21.4+ |

---

## Installation

1. Download the latest `SummitRaffle-x.x.x.jar` from [Releases](../../releases).
2. Place the jar into your server's `/plugins` folder.
3. Restart or reload your server.

---

## Usage

### Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/raffle start <prize>` | `summitraffle.admin` | Start a new raffle with the given prize. |
| `/raffle stop` | `summitraffle.admin` | Cancel the active raffle. |
| `/raffle join` | `summitraffle.join` | Join the currently active raffle. |
| `/raffle status` | `summitraffle.join` | Check how many players have entered. |

### Example

```
# Admin starts a raffle
/raffle start Diamond Sword

# Players join
/raffle join

# Admin draws a winner (runs automatically or manually)
/raffle stop
```

---

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `summitraffle.admin` | `op` | Access to start and stop raffles. |
| `summitraffle.join` | `true` | Ability to join and view raffles. |

---

## Building from Source

Requires JDK 21+.

```bash
git clone https://github.com/your-org/SummitRaffle.git
cd SummitRaffle
./gradlew shadowJar
```

The plugin jar will be output to `build/libs/SummitRaffle-<version>.jar`.

---

## Project Structure

```
SummitRaffle/
├── src/
│   └── main/
│       ├── java/com/summitcraft/summitraffle/
│       │   └── SummitRaffle.java       # Plugin entry point
│       └── resources/
│           └── plugin.yml              # Plugin metadata
├── build.gradle                        # Gradle build config
└── settings.gradle                     # Project name
```

---

## License

This project is licensed under the [MIT License](LICENSE).
