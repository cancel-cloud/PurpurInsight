package cancelcloud.command

import cancelcloud.PurpurInsightPlugin
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.awt.Color

object PlayersCommand : (SlashCommandInteractionEvent) -> Unit {
    private const val PLAYERS_PER_PAGE = 10

    override fun invoke(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()

        PurpurInsightPlugin.instance.server.scheduler.runTask(PurpurInsightPlugin.instance, Runnable {
            val players = PurpurInsightPlugin.instance.server.onlinePlayers.toList()

            if (players.isEmpty()) {
                event.hook.sendMessage("ℹ️ **No Players Online** - The server is currently empty.").queue()
                return@Runnable
            }

            sendPlayerPage(event, players, 0)
        })
    }

    private fun sendPlayerPage(event: SlashCommandInteractionEvent, players: List<org.bukkit.entity.Player>, page: Int) {
        val totalPages = (players.size + PLAYERS_PER_PAGE - 1) / PLAYERS_PER_PAGE
        val startIndex = page * PLAYERS_PER_PAGE
        val endIndex = minOf(startIndex + PLAYERS_PER_PAGE, players.size)
        val pageePlayers = players.subList(startIndex, endIndex)

        val embed = EmbedBuilder()
            .setTitle("🎮 Online Players (${players.size}/${PurpurInsightPlugin.instance.server.maxPlayers})")
            .setColor(Color(0, 153, 255))
            .setDescription(buildPlayerList(pageePlayers))
            .setFooter("Page ${page + 1}/$totalPages", null)
            .build()

        val buttons = mutableListOf<Button>()
        if (page > 0) {
            buttons.add(Button.primary("players:prev:$page", "◀ Previous"))
        }
        if (page < totalPages - 1) {
            buttons.add(Button.primary("players:next:$page", "Next ▶"))
        }

        if (buttons.isEmpty()) {
            event.hook.sendMessageEmbeds(embed).queue()
        } else {
            event.hook.sendMessageEmbeds(embed)
                .addActionRow(buttons)
                .queue()
        }
    }

    private fun buildPlayerList(players: List<org.bukkit.entity.Player>): String {
        return players.joinToString("\n") { player ->
            val ping = player.ping
            val pingEmoji = when {
                ping < 50 -> "🟢"
                ping < 100 -> "🟡"
                ping < 200 -> "🟠"
                else -> "🔴"
            }
            "**${player.name}** $pingEmoji ${ping}ms"
        }
    }

    fun handleButtonInteraction(event: ButtonInteractionEvent) {
        val parts = event.componentId.split(":")
        if (parts.size != 3 || parts[0] != "players") return

        val action = parts[1]
        val currentPage = parts[2].toIntOrNull() ?: return

        event.deferEdit().queue()

        PurpurInsightPlugin.instance.server.scheduler.runTask(PurpurInsightPlugin.instance, Runnable {
            val players = PurpurInsightPlugin.instance.server.onlinePlayers.toList()

            if (players.isEmpty()) {
                event.hook.editOriginal("ℹ️ **No Players Online** - The server is currently empty.")
                    .setEmbeds(emptyList())
                    .setActionRow(emptyList())
                    .queue()
                return@Runnable
            }

            val newPage = when (action) {
                "next" -> currentPage + 1
                "prev" -> currentPage - 1
                else -> currentPage
            }

            val totalPages = (players.size + PLAYERS_PER_PAGE - 1) / PLAYERS_PER_PAGE
            val validPage = newPage.coerceIn(0, totalPages - 1)

            val startIndex = validPage * PLAYERS_PER_PAGE
            val endIndex = minOf(startIndex + PLAYERS_PER_PAGE, players.size)
            val pagePlayers = players.subList(startIndex, endIndex)

            val embed = EmbedBuilder()
                .setTitle("🎮 Online Players (${players.size}/${PurpurInsightPlugin.instance.server.maxPlayers})")
                .setColor(Color(0, 153, 255))
                .setDescription(buildPlayerList(pagePlayers))
                .setFooter("Page ${validPage + 1}/$totalPages", null)
                .build()

            val buttons = mutableListOf<Button>()
            if (validPage > 0) {
                buttons.add(Button.primary("players:prev:$validPage", "◀ Previous"))
            }
            if (validPage < totalPages - 1) {
                buttons.add(Button.primary("players:next:$validPage", "Next ▶"))
            }

            if (buttons.isEmpty()) {
                event.hook.editOriginalEmbeds(embed).setActionRow(emptyList()).queue()
            } else {
                event.hook.editOriginalEmbeds(embed).setActionRow(buttons).queue()
            }
        })
    }
}
