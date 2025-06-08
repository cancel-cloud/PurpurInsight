package cancelcloud

import org.bukkit.plugin.java.JavaPlugin
import cancelcloud.config.BotConfig
import cancelcloud.listener.PlayerListener
import cancelcloud.command.StatsCommand
import cancelcloud.command.PingCommand
import cancelcloud.command.AutoUpdatesCommand
import cancelcloud.command.DiscordChannelCommand
import cancelcloud.service.StatsService
import cancelcloud.util.EmbedBuilderUtil

// JDA & JDA-KTX
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.JDA

import dev.minn.jda.ktx.jdabuilder.light
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.jdabuilder.intents
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import java.io.File
import org.bukkit.scheduler.BukkitTask

class PurpurInsightPlugin : JavaPlugin() {
    companion object {
        lateinit var instance: PurpurInsightPlugin
    }

    private lateinit var botConfig: BotConfig
    lateinit var jda: JDA
    var autoUpdateMinutes: Long = 30
    private var updateTask: BukkitTask? = null
    private var monitorTask: BukkitTask? = null
    private var playerAlert = false
    private var memoryAlert = false
    private var cpuAlert = false
    private var tpsAlert = false
    private var diskAlert = false

    override fun onEnable() {
        instance = this

        // Stelle sicher, dass die Default-Konfiguration existiert
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        val cfgFile = File(dataFolder, "config.yml")
        if (!cfgFile.exists()) {
            saveDefaultConfig()
            logger.info("§econfig.yml wurde erstellt. Bitte fülle sie aus und starte den Server neu.")
            // Plugin deaktivieren, bis der Benutzer die Konfiguration angepasst hat
            server.pluginManager.disablePlugin(this)
            return
        }


        saveDefaultConfig()
        botConfig = BotConfig.load(config)
        autoUpdateMinutes = config.getLong("auto-update-minutes", 30)

        // Spieler-Zeitlistener registrieren
        server.pluginManager.registerEvents(PlayerListener(), this)

        // Serverbefehl registrieren
        getCommand("purpurinsight")?.setExecutor(DiscordChannelCommand())
        getCommand("purpurinsight")?.tabCompleter = DiscordChannelCommand()

        startBot()
        startAutoUpdates()
        startMonitoring()
    }

    override fun onDisable() {
        updateTask?.cancel()
        monitorTask?.cancel()
        if (this::jda.isInitialized) {
            jda.shutdownNow()
        }
    }

    private fun startAutoUpdates() {
        updateTask?.cancel()
        if (autoUpdateMinutes <= 0) return
        val ticks = autoUpdateMinutes * 60L * 20L
        updateTask = server.scheduler.runTaskTimer(this, Runnable {
            val stats = StatsService.collectAll()
            val embed = EmbedBuilderUtil.buildEmbed(stats).build()
            val channel = jda.getTextChannelById(botConfig.statsChannelId)
            channel?.sendMessageEmbeds(embed)?.queue()
        }, ticks, ticks)
    }

    private fun startMonitoring() {
        monitorTask?.cancel()
        val interval = 60L * 20L // every minute
        monitorTask = server.scheduler.runTaskTimer(this, Runnable {
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

    fun updateInterval(minutes: Long) {
        autoUpdateMinutes = minutes
        config.set("auto-update-minutes", minutes)
        saveConfig()
        startAutoUpdates()
    }

    private fun startBot() {

        jda = light(botConfig.token, enableCoroutines = true) {
            intents += listOf(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT
            )
        }

        jda.listener<ReadyEvent> {
            val guild = it.jda.getGuildById(botConfig.guildId)
            guild?.upsertCommand(botConfig.commandName, "Zeigt Server-Statistiken")?.queue()
            guild?.upsertCommand("ping", "Zeigt Bot-Latenz")?.queue()
            guild?.upsertCommand("auto-updates", "Setzt Intervall f\u00fcr automatische Updates")
                ?.addOption(OptionType.INTEGER, "minutes", "Intervall in Minuten (0 zum Deaktivieren)", false)
                ?.queue()
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

        jda.onCommand("ping") { event: GenericCommandInteractionEvent ->
            val slashEvent = event as? SlashCommandInteractionEvent ?: return@onCommand
            PingCommand(slashEvent)
        }

        jda.onCommand("auto-updates") { event: GenericCommandInteractionEvent ->
            val slashEvent = event as? SlashCommandInteractionEvent ?: return@onCommand
            AutoUpdatesCommand(slashEvent)
        }

        startAutoUpdates()
        startMonitoring()
    }


    fun restartBot() {
        updateTask?.cancel()
        monitorTask?.cancel()
        if (this::jda.isInitialized) {
            jda.shutdownNow()
        }

        botConfig = BotConfig.load(config)
        startBot()
        startAutoUpdates()
        startMonitoring()
        
    private fun startAutoUpdates() {
        updateTask?.cancel()
        if (autoUpdateMinutes <= 0) return
        val ticks = autoUpdateMinutes * 60L * 20L
        updateTask = server.scheduler.runTaskTimer(this, Runnable {
            val stats = StatsService.collectAll()
            val embed = EmbedBuilderUtil.buildEmbed(stats).build()
            val channel = jda.getTextChannelById(botConfig.statsChannelId)
            channel?.sendMessageEmbeds(embed)?.queue()
        }, ticks, ticks)
    }

    private fun startMonitoring() {
        monitorTask?.cancel()
        val interval = 60L * 20L // every minute
        monitorTask = server.scheduler.runTaskTimer(this, Runnable {
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

    fun updateInterval(minutes: Long) {
        autoUpdateMinutes = minutes
        config.set("auto-update-minutes", minutes)
        saveConfig()
        startAutoUpdates()
    }
}