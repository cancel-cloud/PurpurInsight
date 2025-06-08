package cancelcloud.command

import cancelcloud.PurpurInsightPlugin
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.command.brigadier.Commands
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import org.bukkit.command.Command as BukkitCommand

@Suppress("UnstableApiUsage")
class CommandManager(private val plugin: PurpurInsightPlugin) {
    
    private fun createDummyCommand(name: String): BukkitCommand {
        return object : BukkitCommand(name) {
            override fun execute(sender: org.bukkit.command.CommandSender, commandLabel: String, args: Array<String>): Boolean {
                return true
            }
        }
    }

    fun registerCommands() {
        val discordChannelCommand = DiscordChannelCommand()
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()
            commands.register(
                Commands.literal("purpurinsight")
                    .executes { ctx ->
                        discordChannelCommand.onCommand(
                            ctx.source.sender,
                            createDummyCommand("purpurinsight"),
                            "purpurinsight",
                            emptyArray()
                        )
                        Command.SINGLE_SUCCESS
                    }
                    .then(Commands.literal("stats-channel")
                        .then(Commands.argument("id", StringArgumentType.word())
                            .executes { ctx ->
                                val args = arrayOf("stats-channel", ctx.getArgument("id", String::class.java))
                                discordChannelCommand.onCommand(
                                    ctx.source.sender,
                                    createDummyCommand("purpurinsight"),
                                    "purpurinsight",
                                    args
                                )
                                Command.SINGLE_SUCCESS
                            }
                        )
                    )
                    .then(Commands.literal("admin-channel")
                        .then(Commands.argument("id", StringArgumentType.word())
                            .executes { ctx ->
                                val args = arrayOf("admin-channel", ctx.getArgument("id", String::class.java))
                                discordChannelCommand.onCommand(
                                    ctx.source.sender,
                                    createDummyCommand("purpurinsight"),
                                    "purpurinsight",
                                    args
                                )
                                Command.SINGLE_SUCCESS
                            }
                        )
                    )
                    .then(Commands.literal("restart")
                        .executes { ctx ->
                            val args = arrayOf("restart")
                            discordChannelCommand.onCommand(
                                ctx.source.sender,
                                createDummyCommand("purpurinsight"),
                                "purpurinsight",
                                args
                            )
                            Command.SINGLE_SUCCESS
                        }
                    )
                    .then(Commands.literal("link")
                        .then(Commands.argument("username", StringArgumentType.word())
                            .executes { ctx ->
                                val args = arrayOf("link", ctx.getArgument("username", String::class.java))
                                discordChannelCommand.onCommand(
                                    ctx.source.sender,
                                    createDummyCommand("purpurinsight"),
                                    "purpurinsight",
                                    args
                                )
                                Command.SINGLE_SUCCESS
                            }
                        )
                    )
                    .then(Commands.literal("confirm")
                        .then(Commands.argument("discord-id", StringArgumentType.word())
                            .executes { ctx ->
                                val args = arrayOf("confirm", ctx.getArgument("discord-id", String::class.java))
                                discordChannelCommand.onCommand(
                                    ctx.source.sender,
                                    createDummyCommand("purpurinsight"),
                                    "purpurinsight",
                                    args
                                )
                                Command.SINGLE_SUCCESS
                            }
                        )
                    )
                    .build(),
                "Manage Discord channel settings"
            )
        }
    }
} 