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

        val yes = Component.text("[✓ ACCEPT]")
            .color(net.kyori.adventure.text.format.NamedTextColor.GREEN)
            .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/purpurinsight confirm $discordId"))
        
        player.sendMessage("§e§l🔗 Discord Link Request")
        player.sendMessage("§f┌───────────────────────────┐")
        player.sendMessage("§f│ §b${event.user.asTag} §fwants to link! §f│")
        player.sendMessage("§f│                           │")
        player.sendMessage(Component.text("§f│ ").append(yes).append(Component.text("             §f│")))
        player.sendMessage("§f└───────────────────────────┘")
        
        event.reply("🔗 **Link request sent to ${player.name}!**\n✅ They need to click the confirmation button in Minecraft.").setEphemeral(true).queue()
    }
}
