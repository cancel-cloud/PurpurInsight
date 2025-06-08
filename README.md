# PurpurInsight



## English

### Overview
PurpurInsight is a Paper plugin for Minecraft 1.21.4 that integrates a Discord bot to display server statistics via a slash command (`/stats`). It uses the Purpur API and JDA-KTX for bot functionality.

### Features
- Online player count
- TPS (1, 5, 15 minute averages) and MSPT
- RAM usage (used / max)
- CPU load
- Server uptime
- Number of loaded plugins
- World statistics (worlds, chunks, entities)
- Average player latency
- Top 5 players by playtime
- Disk usage (total / free)
- Scheduled updates to the stats channel
- Ping command
- Admin alerts for critical server load
- In-game command `/purpurinsight` to change Discord channel IDs or restart the bot

### Requirements
- Java 21
- Paper/Purpur Server 1.21.4
- Gradle (Kotlin DSL)
- Discord bot token with `bot` and `applications.commands` scopes

### Installation
1. Clone the repository and set the package name to `cancelcloud`.
2. Build the plugin with Gradle:
   ```bash
   ./gradlew shadowJar

3. Copy the generated `PurpurInsight-1.0.0.jar` to your server's `plugins/` folder.
4. Start the server to generate the default `config.yml`.
5. Fill in your Discord bot token, guild ID, channel ID, and command name in `plugins/PurpurInsight/config.yml`.
6. Restart the server.

### Configuration (`config.yml`)

```yaml
bot:
  token: "YOUR_BOT_TOKEN"
  guild-id: 123456789012345678
  stats-channel-id: 123456789012345678
  admin-channel-id: 123456789012345678
  command-name: "stats"
auto-update-minutes: 30
playtime: {}
```

### Usage

* In Discord, type `/stats` in your configured guild and channel.
* The bot will reply with an embed containing all configured statistics.
* Use `/purpurinsight <stats-channel|admin-channel> <id>` in-game to update the Discord channel IDs. The bot will restart automatically.
* Run `/purpurinsight restart` in-game to manually restart the bot without restarting the server.

### Development

* Source code is under `src/main/kotlin/cancelcloud/`.
* Build scripts: `build.gradle.kts`, `settings.gradle.kts`.

### Contributing

Pull requests are welcome. Please follow the Kotlin coding conventions and include tests if applicable.

### License

This project is licensed under the MIT License.

## Deutsch

### Übersicht
PurpurInsight ist ein Paper-Plugin für Minecraft 1.21.4, das einen Discord-Bot integriert, um Serverstatistiken per Slash-Command (`/stats`) anzuzeigen. Es verwendet die Purpur-API und JDA-KTX.

### Funktionen
- Anzahl der Online-Spieler
- TPS (1-, 5- und 15-Minuten-Durchschnitte) und MSPT
- RAM-Nutzung (verwendet / maximal)
- CPU-Auslastung
- Server-Laufzeit
- Anzahl der geladenen Plugins
- Weltstatistiken (Welten, Chunks, Entities)
- Durchschnittliche Spieler-Latenz
- Top 5 Spieler nach Spielzeit
- Festplattennutzung (gesamt / frei)
- Automatische Updates im Discord-Kanal
- Ping-Befehl
- Admin-Benachrichtigungen bei kritischer Serverlast
- Befehl `/purpurinsight`, um Discord-Kanäle zu ändern oder den Bot neu zu starten


### Voraussetzungen
- Java 21
- Paper-/Purpur-Server 1.21.4
- Gradle (Kotlin DSL)
- Discord-Bot-Token mit `bot`- und `applications.commands`-Scopes

### Installation
1. Repository klonen und Paketname auf `cancelcloud` setzen.
2. Plugin mit Gradle bauen:
   ```bash
   ./gradlew shadowJar

3. Die erzeugte `PurpurInsight-1.0.0.jar` in den `plugins/`-Ordner des Servers kopieren.
4. Server starten, um die Standard-`config.yml` zu erstellen.
5. Discord-Bot-Token, Guild-ID, Channel-ID und Command-Name in `plugins/PurpurInsight/config.yml` ausfüllen.
6. Server neu starten.

### Konfiguration (`config.yml`)

```yaml
bot:
  token: "DEIN_BOT_TOKEN"
  guild-id: 123456789012345678
  stats-channel-id: 123456789012345678
  admin-channel-id: 123456789012345678
  command-name: "stats"
auto-update-minutes: 30
playtime: {}
```

### Nutzung

* In Discord `/stats` in der konfigurierten Gilde und im Kanal eingeben.
* Der Bot antwortet mit einem Embed, das alle Statistiken enthält.
* Mit `/purpurinsight <stats-channel|admin-channel> <id>` können die Discord-Kanäle im Spiel geändert werden; der Bot startet danach neu.
* Mit `/purpurinsight restart` lässt sich der Bot neu starten, ohne den Server neu zu starten.

### Beitrag

Pull-Requests sind willkommen. Bitte halte dich an die Kotlin-Coding-Conventions und füge bei Bedarf Tests hinzu.

### Lizenz

Dieses Projekt ist unter der MIT-Lizenz lizenziert.