package io.github.nealgandhi.danielbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.core.kordLogger

suspend fun main() {
    val token = env("DISCORD_TOKEN") ?: throw Exception("You must provide the DISCORD_TOKEN environment variable.")

    val bot = ExtensibleBot(token) {
        slashCommands {
            enabled = true
            env("GUILD_ONLY_COMMANDS")?.let { guildId ->
                kordLogger.warn("Registering commands as guild-only because the GUILD_ONLY_COMMANDS environment variable was set.")
                defaultGuild(guildId)
            }
        }
        extensions {
            add(::TestExtension)
        }
    }

    bot.start()
}
