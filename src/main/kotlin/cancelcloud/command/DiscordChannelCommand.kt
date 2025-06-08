package cancelcloud.command

import cancelcloud.PurpurInsightPlugin
import cancelcloud.service.LinkService
import cancelcloud.service.BotService
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.EmbedBuilder
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
            sender.sendMessage("\u00a7cUsage: /purpurinsight <stats-channel|admin-channel> <id> | restart | link <discordName> | confirm <discordId>")
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
                BotService.restart()
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
                BotService.restart()
                sender.sendMessage("\u00a7aAdmin channel updated and bot restarted.")
            }
            "restart" -> {
                BotService.restart()
                sender.sendMessage("\u00a7aPurpurInsight restarted.")
            }
            "link" -> {
                if (sender !is Player) {
                    sender.sendMessage("\u00a7cOnly players may link accounts.")
                    return true
                }
                if (args.size != 2) {
                    sender.sendMessage("\u00a7cUsage: /purpurinsight link <discordName>")
                    return true
                }
                val name = args[1]
                val user = BotService.jda.getUsersByName(name, true).firstOrNull()
                if (user == null) {
                    sender.sendMessage("\u00a7cUser not found.")
                    return true
                }
                val id = user.idLong
                LinkService.createRequest(sender.uniqueId, id)
                val channel = BotService.jda.getTextChannelById(plugin.config.getLong("bot.stats-channel-id"))
                val embed = EmbedBuilder()
                    .setTitle(sender.server.name)
                    .setDescription("Minecraft user ${sender.name} wants to link this discord account with its minecraft account. Accept?")
                    .build()
                channel?.sendMessage("<@${id}>")
                    ?.setEmbeds(embed)
                    ?.setActionRow(
                        Button.danger("link:no:${sender.uniqueId}", "No"),
                        Button.success("link:yes:${sender.uniqueId}", "Yes")
                    )
                    ?.queue()
                sender.sendMessage("\u00a7aRequest sent to Discord user ${user.asTag}.")
            }
            "confirm" -> {
                if (sender !is Player) return true
                val id = args.getOrNull(1)?.toLongOrNull() ?: return true
                if (!LinkService.requestExists(sender.uniqueId, id)) return true
                LinkService.takeRequest(sender.uniqueId)
                LinkService.link(sender.uniqueId, id)
                sender.sendMessage("\u00a7aAccounts linked.")
            }
            else -> sender.sendMessage("\u00a7cUsage: /purpurinsight <stats-channel|admin-channel> <id> | restart | link <discordName> | confirm <discordId>")
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String>? {
        if (!sender.hasPermission("purpurstats:discordsettings")) return mutableListOf()
        return when (args.size) {
            1 -> listOf("stats-channel", "admin-channel", "restart", "link", "confirm").filter { it.startsWith(args[0]) }.toMutableList()
            else -> mutableListOf()
        }
    }
}
