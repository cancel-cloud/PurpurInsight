package cancelcloud

import org.bukkit.plugin.java.JavaPlugin
import cancelcloud.config.BotConfig
import cancelcloud.listener.PlayerListener
import cancelcloud.command.StatsCommand

// JDA & JDA-KTX
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

import dev.minn.jda.ktx.jdabuilder.light
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.jdabuilder.intents
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import java.io.File

class PurpurInsightPlugin : JavaPlugin() {
    companion object {
        lateinit var instance: PurpurInsightPlugin
    }

    private lateinit var botConfig: BotConfig

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

        // Spieler-Zeitlistener registrieren
        server.pluginManager.registerEvents(PlayerListener(), this)

        // JDA mit CoroutineEventManager erstellen
        val jda = light(botConfig.token, enableCoroutines = true) {
            intents += listOf(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT
            )
        }  // enableCoroutines=true aktiviert Coroutine-Event-Manager  [oai_citation:7‡minndevelopment.github.io](https://minndevelopment.github.io/jda-ktx/)

        // Sobald JDA bereit ist, Slash-Command in der Gilde upserten
        jda.listener<ReadyEvent> {
            val guild = it.jda.getGuildById(botConfig.guildId)
            guild?.upsertCommand(botConfig.commandName, "Zeigt Server-Statistiken")?.queue()
        }

        // Handler für Slash-Command: liefert GenericCommandInteractionEvent  [oai_citation:9‡minndevelopment.github.io](https://minndevelopment.github.io/jda-ktx/jda-ktx/dev.minn.jda.ktx.events/index.html)
        jda.onCommand(botConfig.commandName) { event: GenericCommandInteractionEvent ->
            // Sicherstellen, dass es ein SlashCommandInteractionEvent ist
            val slashEvent = event as? SlashCommandInteractionEvent ?: return@onCommand
            StatsCommand(slashEvent)  // Dein Handler erwartet nun SlashCommandInteractionEvent
        }
    }

    override fun onDisable() {
        // Optional: jda.shutdown()
    }
}