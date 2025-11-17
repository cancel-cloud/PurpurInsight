# PurpurInsight - Claude Development Reference

## Project Overview

**PurpurInsight** is a Minecraft Paper/Purpur server plugin (1.21.4+) that integrates with Discord for real-time server statistics, monitoring, and player-Discord account linking.

- **Language**: Kotlin 2.1.20
- **Server API**: Purpur API 1.21.5
- **Discord Library**: JDA 5.6.0 + JDA-KTX 0.12.0
- **Build Tool**: Gradle (Kotlin DSL)
- **Java Version**: 21
- **Current Branch**: `claude/minecraft-discord-plugin-enhancements-0198HuwhnZo8gAheA91V8fmZ`

---

## Current Features (As of 2025-11-17)

### Core Functionality

#### 1. Server Statistics Collection
- **Performance Metrics**: TPS (1m/5m/15m), MSPT, CPU load
- **Resource Usage**: Memory (used/max), disk space (total/free)
- **Player Data**: Online count, average latency, top 5 by playtime
- **World Stats**: World count, loaded chunks, total entities
- **System Info**: Server uptime, plugin count, server version

#### 2. Discord Slash Commands
- `/stats` - Display comprehensive server statistics
- `/ping` - Show bot gateway latency
- `/auto-updates` - Configure automatic update interval (minutes)
- `/link <player>` - Initiate Discord-Minecraft account linking

#### 3. Minecraft In-Game Commands
`/purpurinsight` with subcommands:
- `stats-channel <channel-id>` - Set Discord stats channel
- `admin-channel <channel-id>` - Set Discord admin alerts channel
- `restart` - Manually restart Discord bot
- `link` - Display linking instructions
- `confirm <discord-id>` - Confirm pending Discord link request

Requires permission: `purpurstats:discordsettings`

#### 4. Automatic Features
- **Auto-Updates**: Posts stats embed to stats channel at configurable intervals (default: 30 min)
- **Server Monitoring**: Continuous monitoring with alerts for:
  - High player count (≥80% capacity)
  - Critical memory usage (≥90%)
  - High CPU load (≥90%)
  - Low TPS (<15)
  - Disk space critical (≥90% used)
- **Playtime Tracking**: Automatic session duration tracking per player
- **Alert De-duplication**: Prevents spam with state-based alert system

#### 5. Discord-Minecraft Account Linking
- Discord user initiates `/link <minecraft_player>` in Discord
- Minecraft player receives formatted request message with clickable button
- Player confirms with button click: `/purpurinsight confirm <discord-id>`
- Link stored persistently in `links.yml` (UUID ↔ Discord ID mapping)
- Bidirectional confirmation messages
- Duplicate/conflict prevention

---

## Architecture

### Project Structure

```
src/main/kotlin/cancelcloud/
├── PurpurInsightPlugin.kt          # Main plugin entry point
├── command/
│   ├── CommandManager.kt           # Brigadier command registration
│   ├── StatsCommand.kt             # Discord /stats handler
│   ├── PingCommand.kt              # Discord /ping handler
│   ├── AutoUpdatesCommand.kt       # Discord /auto-updates handler
│   ├── LinkDiscordCommand.kt       # Discord /link handler
│   └── DiscordChannelCommand.kt    # Minecraft /purpurinsight handler
├── service/
│   ├── BotService.kt               # Discord bot lifecycle & scheduling
│   ├── StatsService.kt             # Server metrics collection
│   ├── LinkService.kt              # Account linking management
│   └── PlayerDataService.kt        # Playtime queries
├── listener/
│   └── PlayerListener.kt           # Join/quit event handlers
├── config/
│   └── BotConfig.kt                # Configuration data class
└── util/
    └── EmbedBuilderUtil.kt         # Discord embed formatting
```

### Key Design Patterns

- **Singleton Services**: BotService, LinkService use Kotlin object singletons
- **Event-Driven**: JDA event listeners + Bukkit event handlers
- **Scheduled Tasks**: BukkitTask for auto-updates and monitoring
- **Thread Safety**: Main thread scheduling for Bukkit API access
- **Persistent Storage**: YAML-based config for playtime and links

### Data Flow Examples

**Stats Request Flow**:
```
Discord /stats → StatsCommand → StatsService.collectAll() [main thread]
→ EmbedBuilderUtil → Discord embed reply
```

**Monitoring Flow**:
```
BukkitTask (60 ticks) → StatsService.collectAll() → Threshold checks
→ Admin channel alerts (if threshold exceeded)
```

**Linking Flow**:
```
Discord /link → LinkDiscordCommand → LinkService.createRequest()
→ Minecraft formatted message → Player confirms
→ LinkService.link() → links.yml persistence → Confirmations
```

---

## Configuration Reference

### config.yml

```yaml
bot:
  token: "YOUR_BOT_TOKEN"              # Discord bot token (required)
  guild-id: 123456789012345678        # Discord server/guild ID
  stats-channel-id: 123456789012345678 # Channel for stats & auto-updates
  admin-channel-id: 123456789012345678 # Channel for monitoring alerts
  command-name: "stats"                # Discord command name (customizable)

auto-update-minutes: 30                 # Auto-update interval (0 = disabled)

playtime:                               # Auto-populated: UUID → milliseconds
  "uuid-here": 3600000

links:                                  # Managed by LinkService
  "uuid-here": "discord-id-here"
```

### links.yml

```yaml
# UUID → Discord ID mappings
"minecraft-uuid": "discord-user-id"
```

---

## Development Guidelines

### Adding New Discord Commands

1. Create command class in `command/` package:
```kotlin
object NewCommand : (SlashCommandInteractionEvent) -> Unit {
    override fun invoke(event: SlashCommandInteractionEvent) {
        // Implementation
        event.reply("Response").queue()
    }
}
```

2. Register in `BotService.kt` `onEvent(ReadyEvent)`:
```kotlin
guild.upsertCommand("commandname", "Description")
    .addOptions(/* options */)
    .queue()
```

3. Add handler in `BotService.kt` `onEvent(SlashCommandInteractionEvent)`:
```kotlin
"commandname" -> NewCommand.invoke(event)
```

### Adding New Minecraft Commands

1. Add subcommand in `DiscordChannelCommand.kt` or create new command class
2. Register in `CommandManager.kt`:
```kotlin
Commands.literal("subcommand")
    .executes { context ->
        // Implementation
        1
    }
```

### Adding New Statistics

1. Add field to `ServerStats` data class in `StatsService.kt`
2. Collect metric in `StatsService.collectAll()`
3. Format in `EmbedBuilderUtil.buildEmbed()`

### Testing Checklist

- [ ] Test on Paper/Purpur 1.21.4+
- [ ] Verify Discord bot permissions (slash commands, send messages, embed links)
- [ ] Test with multiple concurrent players
- [ ] Verify config persistence after restart
- [ ] Test thread safety for Bukkit API access
- [ ] Validate input for channel IDs and player names
- [ ] Test alert de-duplication logic

---

## Recommended Enhancements

### Priority 1: High-Impact Features

1. **Discord Role Synchronization**
   - Auto-assign Discord roles based on Minecraft ranks/permissions
   - Sync Minecraft group changes to Discord roles in real-time
   - Configurable role mappings in config.yml

2. **Rich Player Profiles**
   - `/profile [player]` command showing detailed stats (deaths, kills, playtime, achievements)
   - Link player skin rendering in Discord embeds
   - First join date, last seen, current status

3. **Chat Bridge**
   - Relay Minecraft chat to Discord channel and vice versa
   - Player join/leave notifications in Discord
   - Death messages, advancement announcements
   - Configurable message filtering and formatting

4. **Historical Statistics & Graphs**
   - Store metrics history (TPS, player count, etc.)
   - Generate time-series graphs (last 24h/7d/30d)
   - Database backend (SQLite/MySQL) for better performance

### Priority 2: Enhanced Monitoring

5. **Advanced Alert System**
   - Customizable alert thresholds per metric
   - Alert cooldowns to prevent spam
   - Multiple alert severity levels (warning/critical)
   - Alert acknowledgment system

6. **Player Activity Analytics**
   - Most active players (weekly/monthly leaderboards)
   - Play time distribution graphs
   - Peak hours analysis
   - Player retention metrics

7. **Performance Profiling**
   - Track plugin performance impact
   - Entity count by type
   - Chunk loading statistics
   - Identify laggy chunks/worlds

### Priority 3: User Experience

8. **Interactive Discord Panels**
   - Auto-updating status message (edits instead of posting new)
   - Button controls (restart warnings, whitelist management)
   - Paginated player lists
   - Real-time player count in bot status

9. **Whitelist Management via Discord**
   - `/whitelist add/remove` commands
   - Linked Discord users auto-whitelisted
   - Ban/kick commands with reason logging
   - Approval workflow for whitelist requests

10. **Custom Announcements**
    - `/announce` command to broadcast to Minecraft from Discord
    - Scheduled announcements (server events, maintenance)
    - Rich formatting support

### Priority 4: Quality of Life

11. **Multi-Language Support**
    - Configurable language files
    - Currently German labels - add EN, ES, FR, etc.
    - Per-user language preference

12. **Permission System Enhancement**
    - Discord role-based command permissions
    - More granular Minecraft permissions
    - Permission group templates

13. **Backup & Export**
    - Automatic config backups
    - Export statistics to CSV/JSON
    - Link data migration tools

14. **Web Dashboard** (Advanced)
    - Web interface for server stats
    - Real-time metrics visualization
    - Player management interface
    - OAuth2 login with Discord

### Priority 5: Integration & Extensibility

15. **Webhook Support**
    - External webhooks for events
    - Third-party service integrations
    - Custom event triggers

16. **API for Other Plugins**
    - Expose stats API for other plugins
    - Event system for link changes
    - Developer documentation

---

## Dependencies

### Build Dependencies (build.gradle.kts)

```kotlin
dependencies {
    compileOnly("com.purpurmc.purpur:purpur-api:1.21.5-R0.1-SNAPSHOT")
    implementation("net.dv8tion:JDA:5.6.0") {
        exclude(module = "opus-java")
    }
    implementation("dev.minn:jda-ktx:0.12.0")
    implementation("io.github.helplanner:stacked:2025.3")
    implementation("io.github.helplanner:ascend:2025.3")
    implementation(kotlin("stdlib"))
}
```

### Repositories

- Paper repository: https://repo.papermc.io/repository/maven-public/
- JitPack: https://jitpack.io
- Maven Central

---

## Build & Deploy

### Build Commands

```bash
# Build shadow JAR with all dependencies
./gradlew shadowJar

# Output: build/libs/PurpurInsight-1.0.0.jar
```

### Deployment Steps

1. Build plugin JAR
2. Place in server's `plugins/` directory
3. Configure `plugins/PurpurInsight/config.yml` with Discord bot token
4. Restart server or load plugin with `/reload confirm` (not recommended in production)
5. Verify bot connection in Discord

### Discord Bot Setup

1. Create bot at https://discord.com/developers/applications
2. Enable intents: Server Members, Message Content (if using chat bridge)
3. Generate bot token and add to config.yml
4. Invite bot with permissions:
   - Send Messages
   - Embed Links
   - Use Slash Commands
   - Manage Roles (if using role sync)
5. Copy Guild ID and Channel IDs to config

---

## Git Workflow

### Current Development Branch

```
claude/minecraft-discord-plugin-enhancements-0198HuwhnZo8gAheA91V8fmZ
```

### Commit Guidelines

- Use descriptive commit messages
- Reference issue numbers if applicable
- Test thoroughly before committing
- Keep commits focused and atomic

### Push Instructions

```bash
# Push to feature branch
git push -u origin claude/minecraft-discord-plugin-enhancements-0198HuwhnZo8gAheA91V8fmZ

# IMPORTANT: Branch must start with 'claude/' and end with session ID
# Retry up to 4 times with exponential backoff (2s, 4s, 8s, 16s) on network failures
```

---

## Recent Development History

### Latest Commits

1. `77d7916` - working discord link
2. `6523117` - Merge PR #5: Fix discord bot member caching in linking
3. `5c1c7f1` - Fix asynchronous Discord user lookup
4. `92e857e` - chore discord linking
5. `602c918` - Merge PR #4: Rework account linking between Minecraft and Discord

### Known Issues

- None currently identified in codebase
- Pending link requests are in-memory (lost on restart)
- Single guild support only

---

## Useful Resources

### API Documentation

- **Paper API**: https://jd.papermc.io/paper/1.21/
- **JDA**: https://docs.jda.wiki/
- **Kotlin**: https://kotlinlang.org/docs/

### Related Projects

- Purpur: https://purpurmc.org/
- Paper: https://papermc.io/
- Discord Developer Portal: https://discord.com/developers/docs

---

## Notes for Future Development

### Performance Considerations

- Stats collection runs on main thread (necessary for Bukkit API)
- Keep `collectAll()` lightweight to avoid TPS impact
- Consider caching for expensive calculations
- Use async Discord operations (`.queue()` not `.complete()`)

### Security Considerations

- Never expose Discord bot token in logs or error messages
- Validate all user input (channel IDs, player names)
- Permission checks for sensitive commands
- Rate limiting for Discord commands (future enhancement)

### Maintenance Tasks

- Update JDA version periodically for Discord API changes
- Monitor Paper/Purpur API changes for compatibility
- Test with new Minecraft versions
- Review and optimize scheduled task intervals

---

## Contact & Support

For issues, feature requests, or contributions, refer to the project repository.

**Last Updated**: 2025-11-17
**Plugin Version**: 1.0.0
**Minecraft Version**: 1.21.4+
