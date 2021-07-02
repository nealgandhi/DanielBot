package io.github.nealgandhi.danielbot

import com.kotlindiscord.kord.extensions.ExtensibleBot

suspend fun main() {
    val token = System.getenv("DISCORD_TOKEN") ?: throw Exception("You must provide the DISCORD_TOKEN environment variable.")

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
