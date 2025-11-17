package cancelcloud.command

import cancelcloud.PurpurInsightPlugin
import cancelcloud.util.PermissionUtil
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object KickCommand : (SlashCommandInteractionEvent) -> Unit {
    override fun invoke(event: SlashCommandInteractionEvent) {
        // Check admin permission
        if (!PermissionUtil.checkAdminPermission(event)) return

        val playerName = event.getOption("player")?.asString
        val reason = event.getOption("reason")?.asString ?: "Kicked by Discord admin"

        if (playerName == null) {
            event.reply("❌ **Error** - Player name is required.").setEphemeral(true).queue()
            return
        }

        event.deferReply().queue()

        PurpurInsightPlugin.instance.server.scheduler.runTask(PurpurInsightPlugin.instance, Runnable {
            try {
                val player = PurpurInsightPlugin.instance.server.getPlayer(playerName)

                if (player == null) {
                    event.hook.sendMessage("❌ **Error** - Player `$playerName` is not online.")
                        .setEphemeral(true)
                        .queue()
                    return@Runnable
                }

                player.kick(net.kyori.adventure.text.Component.text(reason))
                event.hook.sendMessage("👢 **Player Kicked** - `$playerName` has been kicked by <@${event.user.id}>.\n**Reason:** $reason")
                    .queue()

                // Log to console
                PurpurInsightPlugin.instance.logger.info("${event.user.asTag} kicked $playerName via Discord. Reason: $reason")
            } catch (e: Exception) {
                event.hook.sendMessage("❌ **Error** - Failed to kick `$playerName`: ${e.message}")
                    .setEphemeral(true)
                    .queue()
                PurpurInsightPlugin.instance.logger.warning("Failed to kick $playerName: ${e.message}")
            }
        })
    }
}
