package io.github.nealgandhi.danielbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.booleanList
import com.kotlindiscord.kord.extensions.commands.converters.string
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.channel.createEmbed

@OptIn(KordPreview::class)
class TestExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name = "test"

    class TestArgs : Arguments() {
        val string by string("string", "String argument")

        val bools by booleanList("bools", "Multiple boolean arguments")
    }

    override suspend fun setup() {
        command(TestExtension::TestArgs) {
            name = "test"

            description = "Test command, please ignore"

            action {
                message.channel.createEmbed {
                    title = "Test response title"
                    description = "Test description"

                    field {
                        name = "String"
                        value = arguments.string
                    }

                    field {
                        name = "Bools (${arguments.bools.size})"
                        value = arguments.bools.joinToString(", ") { "`$it`" }
                    }
                }
            }
        }
    }
}
