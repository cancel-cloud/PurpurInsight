package cancelcloud.service

import cancelcloud.PurpurInsightPlugin
import cancelcloud.command.*
import cancelcloud.config.BotConfig
import cancelcloud.service.LinkService
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
    private var playerAlert = false
    private var memoryAlert = false
    private var cpuAlert = false
    private var tpsAlert = false
    private var diskAlert = false

    fun init(plugin: PurpurInsightPlugin, config: BotConfig) {
        this.plugin = plugin
        botConfig = config
        autoUpdateMinutes = plugin.config.getLong("auto-update-minutes", 30)
        startBot()
        startAutoUpdates()
        startMonitoring()
    }

    fun restart() {
        shutdown()
        botConfig = BotConfig.load(plugin.config)
        startBot()
        startAutoUpdates()
        startMonitoring()
    }

    fun shutdown() {
        updateTask?.cancel()
        monitorTask?.cancel()
        if (this::jda.isInitialized) {
            jda.shutdownNow()
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
            channel?.sendMessageEmbeds(embed)?.queue()
        }, ticks, ticks)
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
                GatewayIntent.MESSAGE_CONTENT
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

        jda.listener<ButtonInteractionEvent> { e ->
            when {
                e.componentId.startsWith("link:yes:") -> {
                    val uuid = UUID.fromString(e.componentId.substringAfter("link:yes:"))
                    val req = LinkService.getRequestByDiscord(e.user.idLong)
                    if (req == uuid) {
                        LinkService.takeRequest(uuid)
                        LinkService.link(uuid, e.user.idLong)
                        e.message.editMessage("Link accepted by <@${e.user.id}>").setComponents().queue()
                        e.channel.sendMessage("<@${e.user.id}> linked with ${PurpurInsightPlugin.instance.server.getPlayer(uuid)?.name ?: "player"}.").queue()
                        e.reply("Accounts linked!").setEphemeral(true).queue()
                        PurpurInsightPlugin.instance.server.scheduler.runTask(PurpurInsightPlugin.instance) {
                            PurpurInsightPlugin.instance.server.getPlayer(uuid)?.sendMessage("\u00a7aAccounts linked with ${e.user.asTag}.")
                        }
                    } else {
                        e.reply("No request found.").setEphemeral(true).queue()
                    }
                }
                e.componentId.startsWith("link:no:") -> {
                    val uuid = UUID.fromString(e.componentId.substringAfter("link:no:"))
                    val req = LinkService.getRequestByDiscord(e.user.idLong)
                    if (req == uuid) {
                        LinkService.takeRequest(uuid)
                        e.message.editMessage("Link declined by <@${e.user.id}>").setComponents().queue()
                        e.channel.sendMessage("<@${e.user.id}> declined the link request.").queue()
                        e.reply("Request declined.").setEphemeral(true).queue()
                        PurpurInsightPlugin.instance.server.scheduler.runTask(PurpurInsightPlugin.instance) {
                            PurpurInsightPlugin.instance.server.getPlayer(uuid)?.sendMessage("\u00a7cLink request declined by ${e.user.asTag}.")
                        }
                    } else {
                        e.reply("No request found.").setEphemeral(true).queue()
                    }
                }
            }
        }
    }
}
