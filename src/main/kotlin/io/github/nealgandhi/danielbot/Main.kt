package io.github.nealgandhi.danielbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.core.kordLogger
import io.github.nealgandhi.danielbot.prompt_sequence.PromptsTestExtension
import io.github.nealgandhi.danielbot.role_menus.MongoRoleMenuStorage
import io.github.nealgandhi.danielbot.role_menus.RoleMenuExtension
import io.github.nealgandhi.danielbot.role_menus.RoleMenuStorage
import io.github.nealgandhi.danielbot.util.requireEnv
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

suspend fun main() {
    val token = requireEnv("DISCORD_TOKEN")

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
            add(::PromptsTestExtension)
            add(::RoleMenuExtension)
        }
        hooks {
            afterKoinSetup {
                loadModule {
                    single {
                        val connectionString = requireEnv("CONNECTION_STRING")
                        val name = requireEnv("DATABASE_NAME")
                        KMongo.createClient(connectionString).coroutine.getDatabase(name)
                    }
                }
                loadModule {
                    single<RoleMenuStorage> { MongoRoleMenuStorage() }
                }
            }
        }
    }

    bot.start()
}
