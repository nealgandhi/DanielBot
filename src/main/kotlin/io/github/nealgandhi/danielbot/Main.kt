package io.github.nealgandhi.danielbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.core.kordLogger
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

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
        hooks {
            afterKoinSetup {
                loadModule {
                    single {
                        val connectionString = env("CONNECTION_STRING")
                            ?: throw Exception("You must provide the MONGO_CONNECTION_STRING environment variable.")
                        val name = env("DATABASE_NAME")
                            ?: throw Exception("You must provide the DATABASE_NAME environment variable.")
                        KMongo.createClient(connectionString).coroutine.getDatabase(name)
                    }
                }
            }
        }
    }

    bot.start()
}
