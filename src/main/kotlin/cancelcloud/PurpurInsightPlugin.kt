package cancelcloud

import org.bukkit.plugin.java.JavaPlugin
import cancelcloud.config.BotConfig
import cancelcloud.listener.PlayerListener
import cancelcloud.command.CommandManager
import cancelcloud.service.LinkService
import cancelcloud.service.BotService
import java.io.File

class PurpurInsightPlugin : JavaPlugin() {
    companion object {
        lateinit var instance: PurpurInsightPlugin
    }

    private lateinit var botConfig: BotConfig
    private lateinit var commandManager: CommandManager

    override fun onEnable() {
        instance = this

        if (!dataFolder.exists()) dataFolder.mkdirs()
        val cfgFile = File(dataFolder, "config.yml")
        if (!cfgFile.exists()) {
            saveDefaultConfig()
            logger.info("§econfig.yml wurde erstellt. Bitte fülle sie aus und starte den Server neu.")
            server.pluginManager.disablePlugin(this)
            return
        }

        saveDefaultConfig()
        botConfig = BotConfig.load(config)
        commandManager = CommandManager(this)

        server.pluginManager.registerEvents(PlayerListener(), this)
        LinkService.load(this)

        commandManager.registerCommands()
        BotService.init(this, botConfig)
    }

    override fun onDisable() {
        BotService.shutdown()
    }
}
