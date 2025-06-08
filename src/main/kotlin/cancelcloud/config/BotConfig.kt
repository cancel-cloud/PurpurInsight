package cancelcloud.config

import org.bukkit.configuration.file.FileConfiguration

data class BotConfig(
    val token: String,
    val guildId: Long,
    val statsChannelId: Long,
    val adminChannelId: Long,
    val commandName: String
) {
    companion object {
        fun load(cfg: FileConfiguration): BotConfig = BotConfig(
            token = cfg.getString("bot.token")!!,
            guildId = cfg.getLong("bot.guild-id"),
            statsChannelId = cfg.getLong("bot.stats-channel-id"),
            adminChannelId = cfg.getLong("bot.admin-channel-id"),
            commandName = cfg.getString("bot.command-name")!!
        )
    }
}