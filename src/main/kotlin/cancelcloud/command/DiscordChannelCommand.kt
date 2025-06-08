package cancelcloud.command

import cancelcloud.PurpurInsightPlugin
import cancelcloud.service.LinkService
import cancelcloud.service.BotService
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.concurrent.Task
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
            sender.sendMessage("\u00a7cUsage: /purpurinsight <stats-channel|admin-channel> <id> | restart | link <username> | confirm <discordId>")
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
                    sender.sendMessage("\u00a7cUsage: /purpurinsight link <username>")
                    return true
                }
                val username = args[1]
                
                // Debug: Print available users to console
                plugin.logger.info("=== LINK COMMAND DEBUG ===")
                plugin.logger.info("Searching for Discord user: '$username'")
                
                // Debug: Print online Minecraft players
                val onlinePlayers = plugin.server.onlinePlayers.map { it.name }
                plugin.logger.info("Online Minecraft players (${onlinePlayers.size}): ${onlinePlayers.joinToString(", ")}")
                
                // Debug: Print Discord guild members
                val guild = BotService.jda.getGuildById(plugin.config.getString("bot.guild-id")!!)
                if (guild != null) {
                    plugin.logger.info("Guild found: ${guild.name} (ID: ${guild.id})")
                    plugin.logger.info("Bot permissions in guild: ${guild.selfMember.permissions}")
                    
                    // Check cached members first
                    val cachedMembers = guild.members
                    plugin.logger.info("Cached guild members (${cachedMembers.size}):")
                    cachedMembers.forEach { member: Member ->
                        plugin.logger.info("  - Username: ${member.user.name}, Nickname: ${member.nickname ?: "none"}, ID: ${member.id}")
                    }
                    
                } else {
                    plugin.logger.info("Guild not found! Check your guild-id in config.")
                }
                
                // Try to load all members first if cache is small
                if (guild != null && guild.memberCount > guild.memberCache.size()) {
                    plugin.logger.info("Cache incomplete (${guild.memberCache.size()}/${guild.memberCount}), loading all members...")
                    try {
                        guild.loadMembers().get() // Load all members synchronously
                        plugin.logger.info("Loaded all members, cache now has ${guild.memberCache.size()} members")
                    } catch (e: Exception) {
                        plugin.logger.warning("Failed to load all members: ${e.message}")
                    }
                }
                
                // Search for user using cached members first
                var member = guild?.let { g ->
                    g.getMembersByName(username, true).firstOrNull() 
                        ?: g.getMembersByEffectiveName(username, true).firstOrNull()
                        ?: g.getMembersByNickname(username, true).firstOrNull()
                }
                
                // If not found in cache and cache is incomplete, search through all cached members case-insensitively
                if (member == null && guild != null) {
                    plugin.logger.info("Member '$username' not found via exact name search, trying case-insensitive search...")
                    
                    member = guild.members.find { m ->
                        m.user.name.equals(username, ignoreCase = true) ||
                        m.effectiveName.equals(username, ignoreCase = true) ||
                        (m.nickname?.equals(username, ignoreCase = true) == true)
                    }
                    
                    if (member != null) {
                        plugin.logger.info("Found Discord member via case-insensitive search: ${member.user.name} (ID: ${member.id})")
                    } else {
                        plugin.logger.info("Member '$username' not found in cached members")
                    }
                }
                
                if (member == null) {
                    plugin.logger.info("Member '$username' not found in guild")
                    sender.sendMessage("\u00a7cDiscord user '$username' not found in this server.")
                    sender.sendMessage("\u00a7cTry using either the Discord username or server nickname.")
                    return true
                } else {
                    plugin.logger.info("Found Discord member: ${member.user.name} (ID: ${member.id})")
                }
                val user = member.user
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
            else -> sender.sendMessage("\u00a7cUsage: /purpurinsight <stats-channel|admin-channel> <id> | restart | link <username> | confirm <discordId>")
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
