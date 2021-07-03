package io.github.nealgandhi.danielbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env

suspend fun main() {
    val token = env("DISCORD_TOKEN") ?: throw Exception("You must provide the DISCORD_TOKEN environment variable.")

    val bot = ExtensibleBot(token) {
        slashCommands {
            enabled = true
        }
        extensions {
            add(::TestExtension)
        }
    }

    bot.start()
}
