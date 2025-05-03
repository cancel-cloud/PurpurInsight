package cancelcloud.command
import cancelcloud.PurpurInsightPlugin
import cancelcloud.service.StatsService
import cancelcloud.util.EmbedBuilderUtil
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object StatsCommand {
    operator fun invoke(event: SlashCommandInteractionEvent) {
        // Daten im Haupt-Thread sammeln
        PurpurInsightPlugin.instance.server.scheduler.runTask(PurpurInsightPlugin.instance) { ->
            val stats = StatsService.collectAll()
            val embed = EmbedBuilderUtil.buildEmbed(stats).build()
            // Antwort an Discord senden
            event.replyEmbeds(embed).queue()
        }
    }
}