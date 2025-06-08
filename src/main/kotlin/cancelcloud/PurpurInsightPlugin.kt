package cancelcloud

import org.bukkit.plugin.java.JavaPlugin
import cancelcloud.config.BotConfig
import cancelcloud.listener.PlayerListener
import cancelcloud.command.StatsCommand
import cancelcloud.command.PingCommand
import cancelcloud.command.AutoUpdatesCommand
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

        // JDA mit CoroutineEventManager erstellen
        jda = light(botConfig.token, enableCoroutines = true) {
            intents += listOf(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT
            )
        }  // enableCoroutines=true aktiviert Coroutine-Event-Manager  [oai_citation:7‡minndevelopment.github.io](https://minndevelopment.github.io/jda-ktx/)

        // Sobald JDA bereit ist, Slash-Command in der Gilde upserten
        jda.listener<ReadyEvent> {
            val guild = it.jda.getGuildById(botConfig.guildId)
            guild?.upsertCommand(botConfig.commandName, "Zeigt Server-Statistiken")?.queue()
            guild?.upsertCommand("ping", "Zeigt Bot-Latenz")?.queue()
            guild?.upsertCommand("auto-updates", "Setzt Intervall f\u00fcr automatische Updates")
                ?.addOption(OptionType.INTEGER, "minutes", "Intervall in Minuten (0 zum Deaktivieren)", false)
                ?.queue()
        }

        // Handler für Slash-Command: liefert GenericCommandInteractionEvent  [oai_citation:9‡minndevelopment.github.io](https://minndevelopment.github.io/jda-ktx/jda-ktx/dev.minn.jda.ktx.events/index.html)
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

        startAutoUpdates()
    }

    override fun onDisable() {
        updateTask?.cancel()
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

    fun updateInterval(minutes: Long) {
        autoUpdateMinutes = minutes
        config.set("auto-update-minutes", minutes)
        saveConfig()
        startAutoUpdates()
    }
}