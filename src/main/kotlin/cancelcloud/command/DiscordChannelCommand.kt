package cancelcloud.command

import cancelcloud.PurpurInsightPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class DiscordChannelCommand : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("purpurstats:discordsettings")) {
            sender.sendMessage("\u00a7cYou don't have permission.")
            return true
        }
        val plugin = PurpurInsightPlugin.instance
        if (args.isEmpty()) {
            sender.sendMessage("\u00a7cUsage: /purpurinsight <stats-channel|admin-channel> <id> | restart")
            return true
        }

        when (args[0].lowercase()) {
            "stats-channel" -> {
                if (args.size != 2) {
                    sender.sendMessage("\u00a7cUsage: /purpurinsight stats-channel <id>")
                    return true
                }
                val channelId = args[1].toLongOrNull()
                if (channelId == null) {
                    sender.sendMessage("\u00a7cInvalid channel ID.")
                    return true
                }
                plugin.config.set("bot.stats-channel-id", channelId)
                plugin.saveConfig()
                plugin.restartBot()
                sender.sendMessage("\u00a7aStats channel updated and bot restarted.")
            }
            "admin-channel" -> {
                if (args.size != 2) {
                    sender.sendMessage("\u00a7cUsage: /purpurinsight admin-channel <id>")
                    return true
                }
                val channelId = args[1].toLongOrNull()
                if (channelId == null) {
                    sender.sendMessage("\u00a7cInvalid channel ID.")
                    return true
                }
                plugin.config.set("bot.admin-channel-id", channelId)
                plugin.saveConfig()
                plugin.restartBot()
                sender.sendMessage("\u00a7aAdmin channel updated and bot restarted.")
            }
            "restart" -> {
                plugin.restartBot()
                sender.sendMessage("\u00a7aPurpurInsight restarted.")
            }
            else -> sender.sendMessage("\u00a7cUsage: /purpurinsight <stats-channel|admin-channel> <id> | restart")
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String>? {
        if (!sender.hasPermission("purpurstats:discordsettings")) return mutableListOf()
        return when (args.size) {
            1 -> listOf("stats-channel", "admin-channel", "restart").filter { it.startsWith(args[0]) }.toMutableList()
            else -> mutableListOf()
        }
    }
}
