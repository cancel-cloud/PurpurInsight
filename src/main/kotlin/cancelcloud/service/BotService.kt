package cancelcloud.service

import cancelcloud.PurpurInsightPlugin
import cancelcloud.command.*
import cancelcloud.config.BotConfig
import cancelcloud.util.EmbedBuilderUtil
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.requests.GatewayIntent
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.jdabuilder.intents
import dev.minn.jda.ktx.jdabuilder.light
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

object BotService {
    lateinit var jda: JDA
        private set

    private lateinit var plugin: PurpurInsightPlugin
    private lateinit var botConfig: BotConfig
    private var autoUpdateMinutes: Long = 30
    val intervalMinutes: Long
        get() = autoUpdateMinutes
    private var updateTask: BukkitTask? = null
    private var monitorTask: BukkitTask? = null
    private var statusUpdateTask: BukkitTask? = null
    private var playerAlert = false
    private var memoryAlert = false
    private var cpuAlert = false
    private var tpsAlert = false
    private var diskAlert = false
    private var statsPanelMessageId: Long = 0

    fun init(plugin: PurpurInsightPlugin, config: BotConfig) {
        this.plugin = plugin
        botConfig = config
        autoUpdateMinutes = plugin.config.getLong("auto-update-minutes", 30)
        statsPanelMessageId = plugin.config.getLong("stats-panel-message-id", 0)
        startBot()
        startAutoUpdates()
        startMonitoring()
        startBotStatusUpdates()
    }

    fun restart() {
        shutdown()
        botConfig = BotConfig.load(plugin.config)
        statsPanelMessageId = plugin.config.getLong("stats-panel-message-id", 0)
        startBot()
        startAutoUpdates()
        startMonitoring()
        startBotStatusUpdates()
    }

    fun shutdown() {
        updateTask?.cancel()
        monitorTask?.cancel()
        statusUpdateTask?.cancel()
        if (this::jda.isInitialized) {
            try {
                plugin.logger.info("Shutting down Discord bot...")
                jda.shutdown()
                if (!jda.awaitShutdown(java.time.Duration.ofSeconds(10))) {
                    plugin.logger.warning("JDA shutdown timed out, forcing shutdown...")
                    jda.shutdownNow()
                }
                plugin.logger.info("Discord bot shutdown complete.")
            } catch (e: Exception) {
                plugin.logger.warning("Error during JDA shutdown: ${e.message}")
                // Force shutdown if graceful shutdown fails
                try {
                    jda.shutdownNow()
                } catch (e2: Exception) {
                    // Ignore errors during force shutdown
                }
            }
        }
    }

    fun updateInterval(minutes: Long) {
        autoUpdateMinutes = minutes
        plugin.config.set("auto-update-minutes", minutes)
        plugin.saveConfig()
        startAutoUpdates()
    }

    private fun startAutoUpdates() {
        updateTask?.cancel()
        if (autoUpdateMinutes <= 0) return
        val ticks = autoUpdateMinutes * 60L * 20L
        updateTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val stats = StatsService.collectAll()
            val embed = EmbedBuilderUtil.buildEmbed(stats).build()
            val channel = jda.getTextChannelById(botConfig.statsChannelId)

            if (channel == null) return@Runnable

            // If we have a stats panel message ID, edit it instead of posting new
            if (statsPanelMessageId != 0L) {
                channel.editMessageEmbedsById(statsPanelMessageId, embed).queue(
                    { /* Success */ },
                    { error ->
                        // If editing fails (message deleted), post a new one
                        channel.sendMessageEmbeds(embed).queue { message ->
                            statsPanelMessageId = message.idLong
                            plugin.config.set("stats-panel-message-id", statsPanelMessageId)
                            plugin.saveConfig()
                        }
                    }
                )
            } else {
                // Post new message and save its ID
                channel.sendMessageEmbeds(embed).queue { message ->
                    statsPanelMessageId = message.idLong
                    plugin.config.set("stats-panel-message-id", statsPanelMessageId)
                    plugin.saveConfig()
                }
            }
        }, ticks, ticks)
    }

    private fun startBotStatusUpdates() {
        statusUpdateTask?.cancel()
        // Update bot status every 30 seconds (600 ticks)
        statusUpdateTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val onlinePlayers = plugin.server.onlinePlayers.size
            val maxPlayers = plugin.server.maxPlayers
            jda.presence.setPresence(
                net.dv8tion.jda.api.OnlineStatus.ONLINE,
                net.dv8tion.jda.api.entities.Activity.playing("$onlinePlayers/$maxPlayers players online")
            )
        }, 0L, 600L)
    }

    private fun startMonitoring() {
        monitorTask?.cancel()
        val interval = 60L * 20L
        monitorTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            val stats = StatsService.collectAll()
            val channel = jda.getTextChannelById(botConfig.adminChannelId)
            channel ?: return@Runnable

            val playerLoad = stats.onlinePlayers.toDouble() / stats.maxPlayers
            if (playerLoad >= 0.8 && !playerAlert) {
                channel.sendMessage("Player count high: ${stats.onlinePlayers}/${stats.maxPlayers}").queue()
                playerAlert = true
            } else if (playerLoad < 0.7) {
                playerAlert = false
            }

            val memLoad = stats.ramUsed.toDouble() / stats.ramMax
            if (memLoad >= 0.9 && !memoryAlert) {
                channel.sendMessage("Memory usage critical: ${(memLoad*100).toInt()}%").queue()
                memoryAlert = true
            } else if (memLoad < 0.8) {
                memoryAlert = false
            }

            if (stats.cpuLoad >= 90 && !cpuAlert) {
                channel.sendMessage("CPU load critical: ${"%.1f".format(stats.cpuLoad)}%").queue()
                cpuAlert = true
            } else if (stats.cpuLoad < 80) {
                cpuAlert = false
            }

            if (stats.tps1 < 15 && !tpsAlert) {
                channel.sendMessage("TPS low: ${"%.2f".format(stats.tps1)}").queue()
                tpsAlert = true
            } else if (stats.tps1 >= 16) {
                tpsAlert = false
            }

            val diskUsage = 1.0 - stats.diskFree.toDouble() / stats.diskTotal
            if (diskUsage >= 0.9 && !diskAlert) {
                channel.sendMessage("Disk almost full: ${(diskUsage*100).toInt()}% used").queue()
                diskAlert = true
            } else if (diskUsage < 0.85) {
                diskAlert = false
            }
        }, interval, interval)
    }

    private fun startBot() {
        jda = light(botConfig.token, enableCoroutines = true) {
            intents += listOf(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_MEMBERS
            )
        }

        jda.listener<ReadyEvent> {
            val guild = it.jda.getGuildById(botConfig.guildId)
            guild?.upsertCommand(botConfig.commandName, "Zeigt Server-Statistiken")?.queue()
            guild?.upsertCommand("ping", "Zeigt Bot-Latenz")?.queue()
            guild?.upsertCommand("auto-updates", "Setzt Intervall für automatische Updates")
                ?.addOption(OptionType.INTEGER, "minutes", "Intervall in Minuten (0 zum Deaktivieren)", false)
                ?.queue()
            guild?.upsertCommand("link", "Verknüpft Discord mit Minecraft")
                ?.addOption(OptionType.STRING, "player", "Minecraft Spieler", true)
                ?.queue()

            // Whitelist management (requires Discord admin permission)
            guild?.upsertCommand("whitelist", "Verwaltet die Server-Whitelist (Admin)")
                ?.addSubcommands(
                    net.dv8tion.jda.api.interactions.commands.build.SubcommandData("add", "Fügt einen Spieler zur Whitelist hinzu")
                        .addOption(OptionType.STRING, "player", "Minecraft Spieler", true),
                    net.dv8tion.jda.api.interactions.commands.build.SubcommandData("remove", "Entfernt einen Spieler von der Whitelist")
                        .addOption(OptionType.STRING, "player", "Minecraft Spieler", true)
                )
                ?.queue()

            // Kick command (requires Discord admin permission)
            guild?.upsertCommand("kick", "Kickt einen Spieler vom Server (Admin)")
                ?.addOption(OptionType.STRING, "player", "Minecraft Spieler", true)
                ?.addOption(OptionType.STRING, "reason", "Grund für den Kick", false)
                ?.queue()

            // Ban management (requires Discord admin permission)
            guild?.upsertCommand("ban", "Verwaltet Server-Bans (Admin)")
                ?.addSubcommands(
                    net.dv8tion.jda.api.interactions.commands.build.SubcommandData("add", "Bannt einen Spieler")
                        .addOption(OptionType.STRING, "player", "Minecraft Spieler", true)
                        .addOption(OptionType.STRING, "reason", "Grund für den Ban", false)
                        .addOption(OptionType.INTEGER, "duration", "Dauer in Minuten (leer = permanent)", false),
                    net.dv8tion.jda.api.interactions.commands.build.SubcommandData("remove", "Entbannt einen Spieler")
                        .addOption(OptionType.STRING, "player", "Minecraft Spieler", true)
                )
                ?.queue()

            // Players list command
            guild?.upsertCommand("players", "Zeigt alle Online-Spieler")?.queue()
        }

        jda.onCommand(botConfig.commandName) { event: GenericCommandInteractionEvent ->
            val slashEvent = event as? SlashCommandInteractionEvent ?: return@onCommand
            StatsCommand(slashEvent)
        }

        jda.onCommand("ping") { event: GenericCommandInteractionEvent ->
            val slashEvent = event as? SlashCommandInteractionEvent ?: return@onCommand
            PingCommand(slashEvent)
        }

        jda.onCommand("auto-updates") { event: GenericCommandInteractionEvent ->
            val slashEvent = event as? SlashCommandInteractionEvent ?: return@onCommand
            AutoUpdatesCommand(slashEvent)
        }

        jda.onCommand("link") { event: GenericCommandInteractionEvent ->
            val slashEvent = event as? SlashCommandInteractionEvent ?: return@onCommand
            LinkDiscordCommand(slashEvent)
        }

        jda.onCommand("whitelist") { event: GenericCommandInteractionEvent ->
            val slashEvent = event as? SlashCommandInteractionEvent ?: return@onCommand
            WhitelistCommand(slashEvent)
        }

        jda.onCommand("kick") { event: GenericCommandInteractionEvent ->
            val slashEvent = event as? SlashCommandInteractionEvent ?: return@onCommand
            KickCommand(slashEvent)
        }

        jda.onCommand("ban") { event: GenericCommandInteractionEvent ->
            val slashEvent = event as? SlashCommandInteractionEvent ?: return@onCommand
            BanCommand(slashEvent)
        }

        jda.onCommand("players") { event: GenericCommandInteractionEvent ->
            val slashEvent = event as? SlashCommandInteractionEvent ?: return@onCommand
            PlayersCommand(slashEvent)
        }

        jda.listener<ButtonInteractionEvent> { e ->
            when {
                e.componentId.startsWith("players:") -> {
                    PlayersCommand.handleButtonInteraction(e)
                }
                e.componentId.startsWith("link:yes:") -> {
                    val uuid = UUID.fromString(e.componentId.substringAfter("link:yes:"))
                    val req = LinkService.getRequestByDiscord(e.user.idLong)
                    if (req == uuid) {
                        LinkService.takeRequest(uuid)
                        LinkService.link(uuid, e.user.idLong)
                        e.message.editMessage("✅ **Link request accepted by <@${e.user.id}>**").setComponents().queue()
                        e.channel.sendMessage("🔗 <@${e.user.id}> successfully linked with **${PurpurInsightPlugin.instance.server.getPlayer(uuid)?.name ?: "player"}**!").queue()
                        e.reply("✅ **Accounts linked successfully!** You're now connected to your Minecraft account.").setEphemeral(true).queue()
                        PurpurInsightPlugin.instance.server.scheduler.runTask(PurpurInsightPlugin.instance, Runnable {
                            PurpurInsightPlugin.instance.server.getPlayer(uuid)?.sendMessage("\u00a7a\u00a7l✅ Account Linked! \u00a7fYour Discord account \u00a7b${e.user.asTag} \u00a7fis now connected.")
                        })
                    } else {
                        e.reply("❌ **No link request found.** The request may have expired or already been processed.").setEphemeral(true).queue()
                    }
                }
                e.componentId.startsWith("link:no:") -> {
                    val uuid = UUID.fromString(e.componentId.substringAfter("link:no:"))
                    val req = LinkService.getRequestByDiscord(e.user.idLong)
                    if (req == uuid) {
                        LinkService.takeRequest(uuid)
                        e.message.editMessage("❌ **Link request declined by <@${e.user.id}>**").setComponents().queue()
                        e.channel.sendMessage("🚫 <@${e.user.id}> declined the link request.").queue()
                        e.reply("❌ **Request declined.** You chose not to link your accounts.").setEphemeral(true).queue()
                        PurpurInsightPlugin.instance.server.scheduler.runTask(PurpurInsightPlugin.instance, Runnable {
                            PurpurInsightPlugin.instance.server.getPlayer(uuid)?.sendMessage("\u00a7c\u00a7l❌ Link Declined! \u00a7fDiscord user \u00a7b${e.user.asTag} \u00a7fdeclined the link request.")
                        })
                    } else {
                        e.reply("❌ **No link request found.** The request may have expired or already been processed.").setEphemeral(true).queue()
                    }
                }
            }
        }
    }
}
