package cancelcloud.command

import cancelcloud.service.LinkService
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object LinkDiscordCommand {
    operator fun invoke(event: SlashCommandInteractionEvent) {
        val playerName = event.getOption("player")?.asString ?: return
        val player: Player? = Bukkit.getPlayerExact(playerName)
        if (player == null) {
            event.reply("Player not online.").setEphemeral(true).queue()
            return
        }
        val discordId = event.user.idLong
        LinkService.createRequest(player.uniqueId, discordId)

        val yes = Component.text("[YES]").clickEvent(ClickEvent.runCommand("/purpurinsight confirm $discordId"))
        player.sendMessage(Component.text("Discord user ${event.user.asTag} wants to link with you. ").append(yes))
        event.reply("Request sent to ${player.name}.").setEphemeral(true).queue()
    }
}
