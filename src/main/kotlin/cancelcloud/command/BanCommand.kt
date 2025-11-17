package cancelcloud.command

import cancelcloud.PurpurInsightPlugin
import cancelcloud.util.PermissionUtil
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.bukkit.BanList
import org.bukkit.Bukkit
import java.time.Duration
import java.time.Instant
import java.util.Date

object BanCommand : (SlashCommandInteractionEvent) -> Unit {
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
            "add" -> {
                val reason = event.getOption("reason")?.asString ?: "Banned by Discord admin"
                val durationMinutes = event.getOption("duration")?.asLong
                banPlayer(event, playerName, reason, durationMinutes)
            }
            "remove" -> unbanPlayer(event, playerName)
            else -> event.reply("❌ **Error** - Invalid subcommand.").setEphemeral(true).queue()
        }
    }

    private fun banPlayer(event: SlashCommandInteractionEvent, playerName: String, reason: String, durationMinutes: Long?) {
        event.deferReply().queue()

        PurpurInsightPlugin.instance.server.scheduler.runTask(PurpurInsightPlugin.instance, Runnable {
            try {
                val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                val banList = Bukkit.getBanList(BanList.Type.PROFILE)

                // Check if already banned
                if (banList.isBanned(offlinePlayer.playerProfile)) {
                    event.hook.sendMessage("ℹ️ **Already Banned** - `$playerName` is already banned.")
                        .setEphemeral(true)
                        .queue()
                    return@Runnable
                }

                // Calculate expiration date if duration is specified
                val expiration = if (durationMinutes != null && durationMinutes > 0) {
                    Date.from(Instant.now().plus(Duration.ofMinutes(durationMinutes)))
                } else {
                    null
                }

                // Ban the player
                val source = "Discord: ${event.user.asTag}"
                banList.addBan(offlinePlayer.playerProfile, reason, expiration, source)

                // Kick if online
                val onlinePlayer = PurpurInsightPlugin.instance.server.getPlayer(playerName)
                if (onlinePlayer != null && onlinePlayer.isOnline) {
                    onlinePlayer.kick(net.kyori.adventure.text.Component.text(reason))
                }

                val durationText = if (durationMinutes != null && durationMinutes > 0) {
                    " for ${durationMinutes} minutes"
                } else {
                    " permanently"
                }

                event.hook.sendMessage("🔨 **Player Banned** - `$playerName` has been banned$durationText by <@${event.user.id}>.\n**Reason:** $reason")
                    .queue()

                // Log to console
                PurpurInsightPlugin.instance.logger.info("${event.user.asTag} banned $playerName via Discord$durationText. Reason: $reason")
            } catch (e: Exception) {
                event.hook.sendMessage("❌ **Error** - Failed to ban `$playerName`: ${e.message}")
                    .setEphemeral(true)
                    .queue()
                PurpurInsightPlugin.instance.logger.warning("Failed to ban $playerName: ${e.message}")
            }
        })
    }

    private fun unbanPlayer(event: SlashCommandInteractionEvent, playerName: String) {
        event.deferReply().queue()

        PurpurInsightPlugin.instance.server.scheduler.runTask(PurpurInsightPlugin.instance, Runnable {
            try {
                val offlinePlayer = Bukkit.getOfflinePlayer(playerName)
                val banList = Bukkit.getBanList(BanList.Type.PROFILE)

                if (!banList.isBanned(offlinePlayer.playerProfile)) {
                    event.hook.sendMessage("ℹ️ **Not Banned** - `$playerName` is not banned.")
                        .setEphemeral(true)
                        .queue()
                    return@Runnable
                }

                banList.pardon(offlinePlayer.playerProfile)
                event.hook.sendMessage("✅ **Player Unbanned** - `$playerName` has been unbanned by <@${event.user.id}>.")
                    .queue()

                // Log to console
                PurpurInsightPlugin.instance.logger.info("${event.user.asTag} unbanned $playerName via Discord")
            } catch (e: Exception) {
                event.hook.sendMessage("❌ **Error** - Failed to unban `$playerName`: ${e.message}")
                    .setEphemeral(true)
                    .queue()
                PurpurInsightPlugin.instance.logger.warning("Failed to unban $playerName: ${e.message}")
            }
        })
    }
}
