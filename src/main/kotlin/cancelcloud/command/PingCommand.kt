package cancelcloud.command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object PingCommand {
    operator fun invoke(event: SlashCommandInteractionEvent) {
        val ping = event.jda.gatewayPing
        event.reply("Pong! ${ping}ms").queue()
    }
}
