package cancelcloud.command

import cancelcloud.PurpurInsightPlugin
import cancelcloud.util.PermissionUtil
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

object WhitelistCommand : (SlashCommandInteractionEvent) -> Unit {
    override fun invoke(event: SlashCommandInteractionEvent) {
        // Check admin permission
        if (!PermissionUtil.checkAdminPermission(event)) return

        val subcommand = event.subcommandName
        val playerName = event.getOption("player")?.asString

        if (playerName == null) {
            event.reply("❌ **Error** - Player name is required.").setEphemeral(true).queue()
            return
        }

        when (subcommand) {
            "add" -> addToWhitelist(event, playerName)
            "remove" -> removeFromWhitelist(event, playerName)
            else -> event.reply("❌ **Error** - Invalid subcommand.").setEphemeral(true).queue()
        }
    }

    private fun addToWhitelist(event: SlashCommandInteractionEvent, playerName: String) {
        event.deferReply().queue()

        PurpurInsightPlugin.instance.server.scheduler.runTask(PurpurInsightPlugin.instance, Runnable {
            try {
                val offlinePlayer: OfflinePlayer = Bukkit.getOfflinePlayer(playerName)

                if (offlinePlayer.isWhitelisted) {
                    event.hook.sendMessage("ℹ️ **Already Whitelisted** - `$playerName` is already on the whitelist.")
                        .setEphemeral(true)
                        .queue()
                    return@Runnable
                }

                offlinePlayer.isWhitelisted = true
                event.hook.sendMessage("✅ **Whitelist Added** - `$playerName` has been added to the whitelist by <@${event.user.id}>.")
                    .queue()

                // Log to console
                PurpurInsightPlugin.instance.logger.info("${event.user.asTag} added $playerName to whitelist via Discord")
            } catch (e: Exception) {
                event.hook.sendMessage("❌ **Error** - Failed to add `$playerName` to whitelist: ${e.message}")
                    .setEphemeral(true)
                    .queue()
                PurpurInsightPlugin.instance.logger.warning("Failed to add $playerName to whitelist: ${e.message}")
            }
        })
    }

    private fun removeFromWhitelist(event: SlashCommandInteractionEvent, playerName: String) {
        event.deferReply().queue()

        PurpurInsightPlugin.instance.server.scheduler.runTask(PurpurInsightPlugin.instance, Runnable {
            try {
                val offlinePlayer: OfflinePlayer = Bukkit.getOfflinePlayer(playerName)

                if (!offlinePlayer.isWhitelisted) {
                    event.hook.sendMessage("ℹ️ **Not Whitelisted** - `$playerName` is not on the whitelist.")
                        .setEphemeral(true)
                        .queue()
                    return@Runnable
                }

                offlinePlayer.isWhitelisted = false
                event.hook.sendMessage("🗑️ **Whitelist Removed** - `$playerName` has been removed from the whitelist by <@${event.user.id}>.")
                    .queue()

                // Log to console
                PurpurInsightPlugin.instance.logger.info("${event.user.asTag} removed $playerName from whitelist via Discord")
            } catch (e: Exception) {
                event.hook.sendMessage("❌ **Error** - Failed to remove `$playerName` from whitelist: ${e.message}")
                    .setEphemeral(true)
                    .queue()
                PurpurInsightPlugin.instance.logger.warning("Failed to remove $playerName from whitelist: ${e.message}")
            }
        })
    }
}
