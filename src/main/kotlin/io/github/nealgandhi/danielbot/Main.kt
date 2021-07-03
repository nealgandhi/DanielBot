package io.github.nealgandhi.danielbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.core.kordLogger
import io.github.nealgandhi.danielbot.faq.FaqExtension
import io.github.nealgandhi.danielbot.faq.FaqService
import io.github.nealgandhi.danielbot.faq.InMemoryFaqService

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
            add(::FaqExtension)
        }
        hooks {
            @Suppress("USELESS_CAST") // koin requires casts to the interface type, so these casts are not useless
            afterKoinSetup {
                loadModule { single { InMemoryFaqService() as FaqService } }
            }
        }
    }

    bot.start()
}
