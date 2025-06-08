package cancelcloud.command

import cancelcloud.service.BotService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object AutoUpdatesCommand {
    operator fun invoke(event: SlashCommandInteractionEvent) {
        val minutes = event.getOption("minutes")?.asLong
        if (minutes == null) {
            event.reply("Aktuelles Intervall: ${BotService.intervalMinutes} Minuten")
                .setEphemeral(true).queue()
            return
        }
        BotService.updateInterval(minutes)
        if (minutes <= 0) {
            event.reply("Automatische Updates deaktiviert.").queue()
        } else {
            event.reply("Intervall auf $minutes Minuten gesetzt.").queue()
        }
    }
}
