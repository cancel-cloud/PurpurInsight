package cancelcloud.command

import cancelcloud.PurpurInsightPlugin
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object AutoUpdatesCommand {
    operator fun invoke(event: SlashCommandInteractionEvent) {
        val minutes = event.getOption("minutes")?.asLong
        val plugin = PurpurInsightPlugin.instance
        if (minutes == null) {
            event.reply("Aktuelles Intervall: ${plugin.autoUpdateMinutes} Minuten")
                .setEphemeral(true).queue()
            return
        }
        plugin.updateInterval(minutes)
        if (minutes <= 0) {
            event.reply("Automatische Updates deaktiviert.").queue()
        } else {
            event.reply("Intervall auf $minutes Minuten gesetzt.").queue()
        }
    }
}
