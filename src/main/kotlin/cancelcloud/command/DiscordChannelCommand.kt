package cancelcloud.command

import cancelcloud.PurpurInsightPlugin
import cancelcloud.service.LinkService
import cancelcloud.service.BotService
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.bukkit.entity.Player
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
            sender.sendMessage("\u00a7c\u00a7l❌ Usage: \u00a7f/purpurinsight \u00a76<stats-channel|admin-channel> \u00a7e<id> \u00a77| \u00a7brestart \u00a77| \u00a7dlink \u00a7e<username>")
            return true
        }

        when (args[0].lowercase()) {
            "stats-channel" -> {
                if (args.size != 2) {
                    sender.sendMessage("\u00a7c\u00a7l❌ Usage: \u00a7f/purpurinsight stats-channel \u00a7e<channel-id>")
                    return true
                }
                val channelId = args[1].toLongOrNull()
                if (channelId == null) {
                    sender.sendMessage("\u00a7c\u00a7l❌ Invalid channel ID! \u00a7fPlease provide a valid Discord channel ID.")
                    return true
                }
                plugin.config.set("bot.stats-channel-id", channelId)
                plugin.saveConfig()
                BotService.restart()
                sender.sendMessage("\u00a7a\u00a7l✅ Stats channel updated! \u00a7fBot has been restarted with the new settings.")
            }
            "admin-channel" -> {
                if (args.size != 2) {
                    sender.sendMessage("\u00a7c\u00a7l❌ Usage: \u00a7f/purpurinsight admin-channel \u00a7e<channel-id>")
                    return true
                }
                val channelId = args[1].toLongOrNull()
                if (channelId == null) {
                    sender.sendMessage("\u00a7c\u00a7l❌ Invalid channel ID! \u00a7fPlease provide a valid Discord channel ID.")
                    return true
                }
                plugin.config.set("bot.admin-channel-id", channelId)
                plugin.saveConfig()
                BotService.restart()
                sender.sendMessage("\u00a7a\u00a7l✅ Admin channel updated! \u00a7fBot has been restarted with the new settings.")
            }
            "restart" -> {
                BotService.restart()
                sender.sendMessage("\u00a7a\u00a7l🔄 PurpurInsight Bot Restarted! \u00a7fAll systems are now online.")
            }
            "link" -> {
                if (sender !is Player) {
                    sender.sendMessage("\u00a7c\u00a7l❌ Only players can link Discord accounts!")
                    return true
                }
                sender.sendMessage("\u00a7e\u00a7l⚠️ Account Linking Info")
                sender.sendMessage("\u00a7f┌─────────────────────────────┐")
                sender.sendMessage("\u00a7f│ \u00a7b\u00a7lTo link your Discord:       \u00a7f│")
                sender.sendMessage("\u00a7f│                             │")
                sender.sendMessage("\u00a7f│ \u00a791. \u00a7fGo to Discord           \u00a7f│")
                sender.sendMessage("\u00a7f│ \u00a792. \u00a7fUse \u00a7d/link \u00a7e${sender.name}      \u00a7f│")
                sender.sendMessage("\u00a7f│ \u00a793. \u00a7fClick confirmation      \u00a7f│")
                sender.sendMessage("\u00a7f│                             │")
                sender.sendMessage("\u00a7f└─────────────────────────────┘")
                return true


            }
            "confirm" -> {
                if (sender !is Player) return true
                val id = args.getOrNull(1)?.toLongOrNull() ?: return true
                if (!LinkService.requestExists(sender.uniqueId, id)) {
                    sender.sendMessage("\u00a7c\u00a7l❌ No pending link request found!")
                    return true
                }
                LinkService.takeRequest(sender.uniqueId)
                LinkService.link(sender.uniqueId, id)
                sender.sendMessage("\u00a7a\u00a7l✅ Discord Account Linked! \u00a7fYour accounts are now connected.")
                
                // Send feedback to Discord
                val channel = BotService.jda.getTextChannelById(plugin.config.getLong("bot.stats-channel-id"))
                channel?.sendMessage("🔗<@${id}> you have now connected your Minecraft account with Discord.")?.queue()
            }
            else -> sender.sendMessage("\u00a7c\u00a7l❌ Unknown command! \u00a7fUse: \u00a76stats-channel\u00a7f, \u00a76admin-channel\u00a7f, \u00a7brestart\u00a7f, or \u00a7dlink")
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String>? {
        if (!sender.hasPermission("purpurstats:discordsettings")) return mutableListOf()
        return when (args.size) {
            1 -> listOf("stats-channel", "admin-channel", "restart", "link").filter { it.startsWith(args[0]) }.toMutableList()
            else -> mutableListOf()
        }
    }
}
