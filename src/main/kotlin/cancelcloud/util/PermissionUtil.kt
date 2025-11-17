package cancelcloud.util

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object PermissionUtil {
    /**
     * Checks if the user executing the command has administrator permission in Discord
     * @param event The slash command event
     * @return true if user has admin permission, false otherwise
     */
    fun hasAdminPermission(event: SlashCommandInteractionEvent): Boolean {
        val member = event.member ?: return false
        return member.hasPermission(Permission.ADMINISTRATOR)
    }

    /**
     * Sends an ephemeral error message if the user lacks admin permission
     * @param event The slash command event
     * @return true if user has permission, false if permission denied message was sent
     */
    fun checkAdminPermission(event: SlashCommandInteractionEvent): Boolean {
        if (!hasAdminPermission(event)) {
            event.reply("❌ **Permission Denied** - You need Discord Administrator permission to use this command.")
                .setEphemeral(true)
                .queue()
            return false
        }
        return true
    }
}
